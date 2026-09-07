package pl.returns.returnsapp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.returns.allegro.AllegroAuthService;
import pl.returns.allegro.AllegroClient;
import pl.returns.allegro.AllegroUnauthorizedException;
import pl.returns.allegro.ApiloBackgroundPrefill;
import pl.returns.allegro.ProductCodeFormatter;
import pl.returns.allegro.ReturnedItemDto;
import pl.returns.apilo.ApiloAuthService;
import pl.returns.apilo.ApiloClient;
import pl.returns.apilo.ApiloOrder;
import pl.returns.apilo.ApiloUnauthorizedException;
import pl.returns.common.ApiException;
import pl.returns.domain.AllegroAccount;
import pl.returns.domain.AllegroReturnEntity;
import pl.returns.domain.AllegroReturnRepository;
import pl.returns.domain.LocalReturnState;
import pl.returns.domain.ReturnRecord;
import pl.returns.domain.ReturnRecordRepository;
import pl.returns.ledger.LedgerPeriod;
import pl.returns.settings.SettingsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RecognizeReturnService implements ApiloBackgroundPrefill {

	private final AllegroReturnRepository returnRepository;
	private final ReturnRecordRepository recordRepository;
	private final AllegroAuthService allegroAuth;
	private final AllegroClient allegroClient;
	private final ApiloAuthService apiloAuth;
	private final ApiloClient apiloClient;
	private final SettingsService settingsService;
	private final JsonMapper jsonMapper;
	private final ExecutorService virtualThreadExecutor;

	public RecognizeReturnService(
			AllegroReturnRepository returnRepository,
			ReturnRecordRepository recordRepository,
			AllegroAuthService allegroAuth,
			AllegroClient allegroClient,
			ApiloAuthService apiloAuth,
			ApiloClient apiloClient,
			SettingsService settingsService,
			JsonMapper jsonMapper,
			ExecutorService virtualThreadExecutor) {
		this.returnRepository = returnRepository;
		this.recordRepository = recordRepository;
		this.allegroAuth = allegroAuth;
		this.allegroClient = allegroClient;
		this.apiloAuth = apiloAuth;
		this.apiloClient = apiloClient;
		this.settingsService = settingsService;
		this.jsonMapper = jsonMapper;
		this.virtualThreadExecutor = virtualThreadExecutor;
	}

	@Transactional(readOnly = true)
	public List<ReturnListItem> list(Long accountId, String status, LocalReturnState localState) {
		Specification<AllegroReturnEntity> spec = Specification.unrestricted();
		if (accountId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("id"), accountId));
		}
		if (status != null && !status.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}
		if (localState != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("localState"), localState));
		}
		return returnRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(this::toListItem)
				.toList();
	}

	@Override
	public void prefill(UUID returnId) {
		try {
			PrefillResponse response = buildPrefill(returnId);
			returnRepository.findById(returnId).ifPresent(entity -> {
				if (entity.getApiloOrderNumber() == null && response.apiloOrderNumber() != null) {
					entity.setApiloOrderNumber(response.apiloOrderNumber());
					returnRepository.save(entity);
				}
			});
		} catch (Exception ignored) {
			// Prefill w tle nie może zepsuć synchronizacji.
		}
	}

	public PrefillResponse buildPrefill(UUID returnId) {
		AllegroReturnEntity entity = returnRepository.findById(returnId)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono zwrotu"));
		List<ReturnedItemDto> items = parseItems(entity.getItems());
		CompletableFuture<Optional<ApiloOrder>> apiloFuture = CompletableFuture.supplyAsync(
				() -> lookupApilo(entity.getOrderId()), virtualThreadExecutor);
		CompletableFuture<RefundGuess> refundFuture = CompletableFuture.supplyAsync(
				() -> lookupRefund(entity.getAccount(), entity.getOrderId(), items), virtualThreadExecutor);
		Optional<ApiloOrder> apilo = apiloFuture.join();
		RefundGuess refund = refundFuture.join();

		String productCodes = ProductCodeFormatter.format(items, apilo.map(ApiloOrder::items).orElse(List.of()));
		String buyer = apilo.map(ApiloOrder::customerName).filter(v -> v != null && !v.isBlank())
				.orElse(entity.getBuyerLogin());
		LocalDate saleDate = apilo.map(ApiloOrder::saleDate).orElse(null);
		if (saleDate == null) {
			saleDate = refund.saleDate();
		}
		ZoneId zone = settingsService.zoneId();
		LocalDate returnDate = entity.getCreatedAt() == null
				? LocalDate.now(zone)
				: entity.getCreatedAt().atZone(zone).toLocalDate();
		String warning = refund.estimated()
				? "Nie znaleziono zwrotu płatności w Allegro – wpisano sumę pozycji (kwota szacunkowa)."
				: null;
		if (apilo.isEmpty()) {
			String missing = "Nie znaleziono zamówienia w Apilo po numerze Allegro.";
			warning = warning == null ? missing : warning + " " + missing;
		}
		String apiloNumber = apilo.map(ApiloOrder::id).orElse(entity.getApiloOrderNumber());
		if (apiloNumber != null && entity.getApiloOrderNumber() == null) {
			entity.setApiloOrderNumber(apiloNumber);
			returnRepository.save(entity);
		}
		return new PrefillResponse(
				entity.getId(),
				apiloNumber,
				saleDate,
				buyer,
				productCodes,
				returnDate,
				refund.amount(),
				refund.currency(),
				refund.estimated(),
				null,
				warning);
	}

	@Transactional
	public ReturnRecord recognize(UUID returnId, RecognizeRequest request) {
		AllegroReturnEntity entity = returnRepository.findById(returnId)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono zwrotu"));
		if (entity.getLocalState() == LocalReturnState.RECOGNIZED) {
			throw ApiException.conflict("Ten zwrot jest już uznany");
		}
		ReturnRecord record = new ReturnRecord();
		record.setAllegroReturn(entity);
		record.setAccount(entity.getAccount());
		applyRequest(record, request);
		entity.setLocalState(LocalReturnState.RECOGNIZED);
		entity.setApiloOrderNumber(request.apiloOrderNumber());
		recordRepository.save(record);
		return record;
	}

	@Transactional
	public void ignore(UUID returnId) {
		AllegroReturnEntity entity = returnRepository.findById(returnId)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono zwrotu"));
		entity.setLocalState(LocalReturnState.IGNORED);
	}

	@Transactional
	public PrefillResponse refreshAmount(UUID returnId) {
		return buildPrefill(returnId);
	}

	private Optional<ApiloOrder> lookupApilo(String orderId) {
		if (orderId == null) {
			return Optional.empty();
		}
		try {
			var connection = apiloAuth.connection();
			if (!connection.isAuthorized()) {
				return Optional.empty();
			}
			try {
				return apiloClient.findByExternalId(connection, apiloAuth.accessToken(), orderId);
			} catch (ApiloUnauthorizedException ex) {
				apiloAuth.refreshIfUnauthorized();
				return apiloClient.findByExternalId(connection, apiloAuth.accessToken(), orderId);
			}
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	private RefundGuess lookupRefund(AllegroAccount account, String orderId, List<ReturnedItemDto> items) {
		BigDecimal fallback = ProductCodeFormatter.sumItemValues(items);
		String currency = items.stream().map(ReturnedItemDto::currency).filter(c -> c != null).findFirst().orElse("PLN");
		if (orderId == null || !account.isAuthorized()) {
			return new RefundGuess(fallback, currency, true, null);
		}
		try {
			allegroAuth.ensureFreshToken(account);
			String token = allegroAuth.accessToken(account);
			JsonNode checkout;
			try {
				checkout = allegroClient.getCheckoutForm(account.apiBaseUrl(), token, orderId);
			} catch (AllegroUnauthorizedException ex) {
				allegroAuth.refreshAfterUnauthorized(account);
				token = allegroAuth.accessToken(account);
				checkout = allegroClient.getCheckoutForm(account.apiBaseUrl(), token, orderId);
			}
			LocalDate saleDate = parseSaleDate(checkout);
			String paymentId = paymentId(checkout);
			if (paymentId == null) {
				return new RefundGuess(fallback, currency, true, saleDate);
			}
			JsonNode refundsJson = allegroClient.listRefunds(account.apiBaseUrl(), token, paymentId);
			RefundGuess fromApi = sumRefunds(refundsJson, currency, saleDate);
			if (fromApi != null) {
				return fromApi;
			}
			return new RefundGuess(fallback, currency, true, saleDate);
		} catch (Exception ex) {
			return new RefundGuess(fallback, currency, true, null);
		}
	}

	private RefundGuess sumRefunds(JsonNode json, String fallbackCurrency, LocalDate saleDate) {
		JsonNode array = json.get("refunds");
		if (array == null || !array.isArray()) {
			return null;
		}
		BigDecimal sum = BigDecimal.ZERO;
		String currency = fallbackCurrency;
		int counted = 0;
		for (JsonNode refund : array) {
			String status = text(refund, "status");
			if (status != null && !(status.equalsIgnoreCase("SUCCESS")
					|| status.equalsIgnoreCase("COMPLETED")
					|| status.equalsIgnoreCase("DONE"))) {
				continue;
			}
			JsonNode total = refund.get("totalValue");
			if (total == null) {
				total = refund.get("value");
			}
			if (total == null) {
				continue;
			}
			String amount = text(total, "amount");
			if (amount == null) {
				continue;
			}
			try {
				sum = sum.add(new BigDecimal(amount.replace(',', '.')));
				counted++;
				String cur = text(total, "currency");
				if (cur != null) {
					currency = cur;
				}
			} catch (NumberFormatException ignored) {
				// pomiń
			}
		}
		if (counted == 0) {
			return null;
		}
		return new RefundGuess(sum, currency, false, saleDate);
	}

	private String paymentId(JsonNode checkout) {
		JsonNode payment = checkout.get("payment");
		if (payment != null && payment.get("id") != null && !payment.get("id").isNull()) {
			return payment.get("id").asString();
		}
		JsonNode payments = checkout.get("payments");
		if (payments != null && payments.isArray() && !payments.isEmpty()) {
			return text(payments.get(0), "id");
		}
		return null;
	}

	private LocalDate parseSaleDate(JsonNode checkout) {
		String value = first(text(checkout, "occurredAt"), text(checkout, "boughtAt"), text(checkout, "createdAt"));
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value).atZone(settingsService.zoneId()).toLocalDate();
		} catch (Exception ex) {
			return null;
		}
	}

	private List<ReturnedItemDto> parseItems(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		JsonNode node = jsonMapper.readTree(json);
		List<ReturnedItemDto> items = new ArrayList<>();
		if (!node.isArray()) {
			return items;
		}
		for (JsonNode item : node) {
			JsonNode price = item.path("price");
			items.add(new ReturnedItemDto(
					text(item, "offerId"),
					item.path("quantity").asInt(1),
					text(item, "name"),
					decimal(price, "amount"),
					text(price, "currency")));
		}
		return items;
	}

	private ReturnListItem toListItem(AllegroReturnEntity entity) {
		List<String> names = parseItems(entity.getItems()).stream()
				.map(item -> item.quantity() > 1 ? item.name() + " ×" + item.quantity() : item.name())
				.toList();
		return new ReturnListItem(
				entity.getId(),
				entity.getAccount().getId(),
				entity.getAccount().getName(),
				entity.getReferenceNumber(),
				entity.getOrderId(),
				entity.getCreatedAt(),
				entity.getStatus(),
				entity.getBuyerLogin(),
				names,
				entity.getLocalState(),
				entity.getApiloOrderNumber());
	}

	public static void applyRequest(ReturnRecord record, RecognizeRequest request) {
		record.setApiloOrderNumber(request.apiloOrderNumber());
		record.setSaleDate(request.saleDate());
		record.setBuyerName(request.buyerName());
		record.setProductCodes(request.productCodes());
		record.setReturnDate(request.returnDate());
		record.setGrossAmount(request.grossAmount());
		record.setCurrency(request.currency() == null || request.currency().isBlank() ? "PLN" : request.currency());
		record.setAmountEstimated(request.amountEstimated());
		record.setVatAmount(request.vatAmount());
		record.setNotes(request.notes());
		record.setPeriod(LedgerPeriod.fromReturnDate(request.returnDate()));
	}

	private static String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text;
	}

	private static String first(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static BigDecimal decimal(JsonNode node, String field) {
		String value = text(node, field);
		if (value == null) {
			return null;
		}
		try {
			return new BigDecimal(value.replace(',', '.'));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private record RefundGuess(BigDecimal amount, String currency, boolean estimated, LocalDate saleDate) {
	}
}
