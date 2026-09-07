package pl.returns.allegro;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pl.returns.domain.AllegroAccount;
import pl.returns.domain.AllegroAccountRepository;
import pl.returns.domain.AllegroReturnEntity;
import pl.returns.domain.AllegroReturnRepository;
import pl.returns.domain.LocalReturnState;
import pl.returns.domain.SyncRun;
import pl.returns.domain.SyncRunRepository;
import pl.returns.settings.SettingsService;

@Service
public class AllegroReturnSyncJob {

	private static final Logger log = LoggerFactory.getLogger(AllegroReturnSyncJob.class);
	private static final Set<String> TERMINAL = Set.of("FINISHED", "REJECTED");
	private static final int PAGE_SIZE = 100;

	private final AllegroAccountRepository accounts;
	private final AllegroReturnRepository returns;
	private final SyncRunRepository syncRuns;
	private final AllegroClient allegroClient;
	private final AllegroAuthService authService;
	private final SettingsService settingsService;
	private final ExecutorService virtualThreadExecutor;
	private final TransactionTemplate tx;
	private final AtomicBoolean running = new AtomicBoolean();
	private final ApiloBackgroundPrefill prefill;

	public AllegroReturnSyncJob(
			AllegroAccountRepository accounts,
			AllegroReturnRepository returns,
			SyncRunRepository syncRuns,
			AllegroClient allegroClient,
			AllegroAuthService authService,
			SettingsService settingsService,
			ExecutorService virtualThreadExecutor,
			TransactionTemplate tx,
			ApiloBackgroundPrefill prefill) {
		this.accounts = accounts;
		this.returns = returns;
		this.syncRuns = syncRuns;
		this.allegroClient = allegroClient;
		this.authService = authService;
		this.settingsService = settingsService;
		this.virtualThreadExecutor = virtualThreadExecutor;
		this.tx = tx;
		this.prefill = prefill;
	}

	@Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT20S")
	public void scheduledTick() {
		syncAllAccounts();
	}

	public void syncAllAccounts() {
		syncAllAccounts(false);
	}

	public void syncAllAccounts(boolean force) {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		try {
			List<AllegroAccount> enabled = accounts.findByEnabledTrue().stream()
					.filter(AllegroAccount::isAuthorized)
					.toList();
			try (var scope = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
				for (AllegroAccount account : enabled) {
					if (!force && shouldSkip(account)) {
						continue;
					}
					scope.submit(() -> syncAccountInternal(account.getId()));
				}
			}
		} finally {
			running.set(false);
		}
	}

	public void syncAccount(Long accountId) {
		syncAccountInternal(accountId);
	}

	private boolean shouldSkip(AllegroAccount account) {
		Instant last = account.getLastSyncAt();
		return last != null && last.isAfter(Instant.now().minus(settingsService.syncInterval()));
	}

	private void syncAccountInternal(Long accountId) {
		AllegroAccount account = accounts.findById(accountId).orElse(null);
		if (account == null || !account.isEnabled() || !account.isAuthorized()) {
			return;
		}
		SyncRun run = syncRuns.save(new SyncRun(account));
		int fetched = 0;
		int created = 0;
		try {
			authService.ensureFreshToken(account);
			String token = authService.accessToken(account);
			Instant gte = Optional.ofNullable(account.getLastSyncAt())
					.map(ts -> ts.minus(Duration.ofDays(1)))
					.orElse(Instant.now().minus(Duration.ofDays(90)));
			int offset = 0;
			while (true) {
				List<CustomerReturnDto> page;
				try {
					page = allegroClient.listCustomerReturns(account.getId(), account.apiBaseUrl(), token, gte, PAGE_SIZE, offset);
				} catch (AllegroUnauthorizedException ex) {
					authService.refreshAfterUnauthorized(account);
					token = authService.accessToken(account);
					page = allegroClient.listCustomerReturns(account.getId(), account.apiBaseUrl(), token, gte, PAGE_SIZE, offset);
				}
				if (page.isEmpty()) {
					break;
				}
				fetched += page.size();
				created += upsertAll(account.getId(), page);
				if (page.size() < PAGE_SIZE) {
					break;
				}
				offset += PAGE_SIZE;
			}
			refreshOpenStatuses(account, token);
			tx.executeWithoutResult(status -> {
				AllegroAccount fresh = accounts.findById(accountId).orElseThrow();
				fresh.setLastSyncAt(Instant.now());
				fresh.setLastError(null);
				accounts.save(fresh);
			});
			run.setFetched(fetched);
			run.setNewCount(created);
		} catch (Exception ex) {
			log.warn("Synchronizacja Allegro nie powiodła się dla konta {}: {}", account.getName(), ex.getMessage());
			run.setError(ex.getMessage());
			tx.executeWithoutResult(status -> {
				AllegroAccount fresh = accounts.findById(accountId).orElseThrow();
				fresh.setLastError(ex.getMessage());
				accounts.save(fresh);
			});
		} finally {
			run.setFinishedAt(Instant.now());
			syncRuns.save(run);
		}
	}

	private int upsertAll(Long accountId, List<CustomerReturnDto> page) {
		List<UUID> newIds = new ArrayList<>();
		Integer created = tx.execute(status -> {
			AllegroAccount account = accounts.findById(accountId).orElseThrow();
			int count = 0;
			for (CustomerReturnDto dto : page) {
				Optional<AllegroReturnEntity> existing = returns.findById(dto.id());
				if (existing.isEmpty()) {
					AllegroReturnEntity entity = new AllegroReturnEntity(dto.id(), account);
					apply(entity, dto);
					returns.save(entity);
					newIds.add(dto.id());
					count++;
				} else {
					AllegroReturnEntity entity = existing.get();
					apply(entity, dto);
					returns.save(entity);
				}
			}
			return count;
		});
		if (settingsService.apiloPrefillBackground()) {
			newIds.forEach(id -> virtualThreadExecutor.submit(() -> prefill.prefill(id)));
		}
		return created == null ? 0 : created;
	}

	private void refreshOpenStatuses(AllegroAccount account, String token) {
		List<AllegroReturnEntity> open = returns.findByAccountAndLocalStateAndStatusNotIn(
				account, LocalReturnState.NEW, TERMINAL);
		List<UUID> ids = open.stream().map(AllegroReturnEntity::getId).toList();
		for (int i = 0; i < ids.size(); i += 20) {
			List<UUID> chunk = ids.subList(i, Math.min(i + 20, ids.size()));
			List<CustomerReturnDto> fresh;
			try {
				fresh = allegroClient.listCustomerReturnsByIds(account.getId(), account.apiBaseUrl(), token, chunk);
			} catch (AllegroUnauthorizedException ex) {
				authService.refreshAfterUnauthorized(account);
				token = authService.accessToken(account);
				fresh = allegroClient.listCustomerReturnsByIds(account.getId(), account.apiBaseUrl(), token, chunk);
			}
			List<CustomerReturnDto> page = fresh;
			tx.executeWithoutResult(status -> {
				for (CustomerReturnDto dto : page) {
					returns.findById(dto.id()).ifPresent(entity -> {
						apply(entity, dto);
						returns.save(entity);
					});
				}
			});
		}
	}

	private static void apply(AllegroReturnEntity entity, CustomerReturnDto dto) {
		entity.setReferenceNumber(dto.referenceNumber());
		entity.setOrderId(dto.orderId());
		entity.setCreatedAt(dto.createdAt());
		entity.setStatus(dto.status());
		entity.setBuyerLogin(dto.buyerLogin());
		entity.setBuyerEmail(dto.buyerEmail());
		entity.setItems(dto.itemsJson());
		entity.setParcels(dto.parcelsJson());
		entity.setRaw(dto.rawJson());
	}
}
