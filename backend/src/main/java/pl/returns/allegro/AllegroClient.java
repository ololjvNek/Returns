package pl.returns.allegro;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.returns.common.ApiException;
import pl.returns.common.TokenBucketLimiter;
import pl.returns.settings.SettingsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AllegroClient {

	private static final Logger log = LoggerFactory.getLogger(AllegroClient.class);
	private static final String PUBLIC = "application/vnd.allegro.public.v1+json";
	private static final String BETA = "application/vnd.allegro.beta.v1+json";

	private final HttpClient httpClient;
	private final JsonMapper jsonMapper;
	private final SettingsService settingsService;
	private final ConcurrentHashMap<Long, TokenBucketLimiter> returnLimiters = new ConcurrentHashMap<>();

	public AllegroClient(HttpClient httpClient, JsonMapper jsonMapper, SettingsService settingsService) {
		this.httpClient = httpClient;
		this.jsonMapper = jsonMapper;
		this.settingsService = settingsService;
	}

	public DeviceStart startDeviceFlow(String authBaseUrl, String clientId, String clientSecret) {
		String body = "client_id=" + url(clientId);
		HttpRequest request = HttpRequest.newBuilder(URI.create(authBaseUrl + "/auth/oauth/device"))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", basic(clientId, clientSecret))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("User-Agent", settingsService.userAgent())
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		JsonNode json = send(request, false);
		return new DeviceStart(
				text(json, "device_code"),
				text(json, "user_code"),
				text(json, "verification_uri"),
				text(json, "verification_uri_complete"),
				json.path("expires_in").asInt(600),
				Math.max(5, json.path("interval").asInt(5)));
	}

	public Optional<TokenPair> pollDeviceToken(String authBaseUrl, String clientId, String clientSecret, String deviceCode) {
		String query = "grant_type=" + url("urn:ietf:params:oauth:grant-type:device_code")
				+ "&device_code=" + url(deviceCode);
		HttpRequest request = HttpRequest.newBuilder(URI.create(authBaseUrl + "/auth/oauth/token?" + query))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", basic(clientId, clientSecret))
				.header("User-Agent", settingsService.userAgent())
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		HttpResponse<String> response = raw(request);
		if (response.statusCode() == 200) {
			return Optional.of(parseToken(jsonMapper.readTree(response.body())));
		}
		JsonNode json = parseQuietly(response.body());
		String error = json != null ? text(json, "error") : null;
		if ("authorization_pending".equals(error) || "slow_down".equals(error)) {
			return Optional.empty();
		}
		if ("expired_token".equals(error) || "access_denied".equals(error)) {
			throw ApiException.badRequest("Autoryzacja Allegro wygasła lub została odrzucona");
		}
		throw ApiException.serviceUnavailable("Allegro OAuth: HTTP " + response.statusCode());
	}

	public TokenPair refreshToken(String authBaseUrl, String clientId, String clientSecret, String refreshToken) {
		String query = "grant_type=refresh_token&refresh_token=" + url(refreshToken);
		HttpRequest request = HttpRequest.newBuilder(URI.create(authBaseUrl + "/auth/oauth/token?" + query))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", basic(clientId, clientSecret))
				.header("User-Agent", settingsService.userAgent())
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		return parseToken(send(request, false));
	}

	public String fetchLogin(String apiBaseUrl, String accessToken) {
		JsonNode json = send(get(apiBaseUrl + "/me", accessToken, PUBLIC), true);
		return text(json, "login");
	}

	public List<CustomerReturnDto> listCustomerReturns(
			long accountId,
			String apiBaseUrl,
			String accessToken,
			Instant createdAtGte,
			int limit,
			int offset) {
		acquireReturnLimit(accountId);
		StringBuilder query = new StringBuilder("limit=").append(limit).append("&offset=").append(offset);
		if (createdAtGte != null) {
			query.append("&createdAt.gte=").append(url(createdAtGte.toString()));
		}
		JsonNode json = send(get(apiBaseUrl + "/order/customer-returns?" + query, accessToken, BETA), true);
		return parseReturnList(json);
	}

	public List<CustomerReturnDto> listCustomerReturnsByIds(
			long accountId,
			String apiBaseUrl,
			String accessToken,
			List<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		acquireReturnLimit(accountId);
		StringBuilder query = new StringBuilder("limit=").append(Math.max(ids.size(), 1));
		for (UUID id : ids) {
			query.append("&customerReturnId=").append(url(id.toString()));
		}
		JsonNode json = send(get(apiBaseUrl + "/order/customer-returns?" + query, accessToken, BETA), true);
		return parseReturnList(json);
	}

	public JsonNode getCheckoutForm(String apiBaseUrl, String accessToken, String orderId) {
		return send(get(apiBaseUrl + "/order/checkout-forms/" + url(orderId), accessToken, PUBLIC), true);
	}

	public JsonNode listRefunds(String apiBaseUrl, String accessToken, String paymentId) {
		String uri = apiBaseUrl + "/payments/refunds?payment.id=" + url(paymentId) + "&limit=100";
		return send(get(uri, accessToken, PUBLIC), true);
	}

	public record DeviceStart(
			String deviceCode,
			String userCode,
			String verificationUri,
			String verificationUriComplete,
			int expiresIn,
			int intervalSeconds) {
	}

	public record TokenPair(String accessToken, String refreshToken, Instant expiresAt) {
	}

	private List<CustomerReturnDto> parseReturnList(JsonNode json) {
		JsonNode array = json.get("customerReturns");
		if (array == null || !array.isArray()) {
			array = json.get("returns");
		}
		List<CustomerReturnDto> result = new ArrayList<>();
		if (array == null || !array.isArray()) {
			return result;
		}
		for (JsonNode node : array) {
			result.add(parseReturn(node));
		}
		return result;
	}

	private CustomerReturnDto parseReturn(JsonNode node) {
		UUID id = UUID.fromString(text(node, "id"));
		Instant created = parseInstant(text(node, "createdAt"));
		JsonNode buyer = node.path("buyer");
		JsonNode itemsNode = node.path("items");
		List<ReturnedItemDto> items = new ArrayList<>();
		if (itemsNode.isArray()) {
			for (JsonNode item : itemsNode) {
				JsonNode price = item.path("price");
				items.add(new ReturnedItemDto(
						text(item, "offerId"),
						item.path("quantity").asInt(1),
						text(item, "name"),
						decimal(price, "amount"),
						text(price, "currency")));
			}
		}
		JsonNode parcels = node.get("parcels");
		return new CustomerReturnDto(
				id,
				created,
				text(node, "referenceNumber"),
				text(node, "orderId"),
				text(node, "status"),
				text(buyer, "login"),
				text(buyer, "email"),
				items,
				write(itemsNode),
				parcels == null ? "[]" : write(parcels),
				write(node));
	}

	private TokenPair parseToken(JsonNode json) {
		int expiresIn = json.path("expires_in").asInt(43199);
		return new TokenPair(
				text(json, "access_token"),
				text(json, "refresh_token"),
				Instant.now().plusSeconds(Math.max(60, expiresIn - 60)));
	}

	private void acquireReturnLimit(long accountId) {
		returnLimiters
				.computeIfAbsent(accountId, id -> new TokenBucketLimiter(1, Duration.ofSeconds(1)))
				.acquire();
	}

	private HttpRequest get(String uri, String accessToken, String accept) {
		return HttpRequest.newBuilder(URI.create(uri))
				.timeout(Duration.ofSeconds(45))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", accept)
				.header("User-Agent", settingsService.userAgent())
				.GET()
				.build();
	}

	private JsonNode send(HttpRequest request, boolean map401) {
		HttpResponse<String> response = raw(request);
		int status = response.statusCode();
		if (status == 401 && map401) {
			throw new AllegroUnauthorizedException("Token Allegro wygasł");
		}
		if (status == 429) {
			sleepRetry(response);
			response = raw(request);
			status = response.statusCode();
		}
		if (status >= 500) {
			throw ApiException.serviceUnavailable("Allegro API: HTTP " + status);
		}
		if (status >= 400) {
			throw ApiException.badRequest("Allegro API: HTTP " + status + " " + shorten(response.body()));
		}
		return jsonMapper.readTree(response.body());
	}

	private HttpResponse<String> raw(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() == 429 || response.statusCode() >= 500) {
				for (int attempt = 1; attempt <= 4; attempt++) {
					sleepRetry(response);
					response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
					if (response.statusCode() != 429 && response.statusCode() < 500) {
						break;
					}
				}
			}
			return response;
		} catch (IOException ex) {
			throw ApiException.serviceUnavailable("Błąd połączenia z Allegro: " + ex.getMessage());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw ApiException.serviceUnavailable("Przerwano połączenie z Allegro");
		}
	}

	private void sleepRetry(HttpResponse<String> response) {
		long seconds = response.headers().firstValue("Retry-After")
				.map(this::parseLongSafe)
				.filter(v -> v > 0)
				.orElse(2L);
		try {
			Thread.sleep(Duration.ofSeconds(Math.min(seconds, 60)));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private long parseLongSafe(String value) {
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ex) {
			return 2L;
		}
	}

	private JsonNode parseQuietly(String body) {
		try {
			return jsonMapper.readTree(body);
		} catch (Exception ex) {
			return null;
		}
	}

	private String write(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return "[]";
		}
		return jsonMapper.writeValueAsString(node);
	}

	private static String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.isMissingNode()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text;
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

	private static Instant parseInstant(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (Exception ex) {
			return Instant.now();
		}
	}

	private static String basic(String user, String password) {
		return "Basic " + java.util.Base64.getEncoder()
				.encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
	}

	private static String url(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String shorten(String body) {
		if (body == null) {
			return "";
		}
		return body.length() > 240 ? body.substring(0, 240) : body;
	}
}
