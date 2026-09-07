package pl.returns.allegro;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.returns.common.ApiException;
import pl.returns.crypto.SecretCipher;
import pl.returns.domain.AllegroAccount;
import pl.returns.domain.AllegroAccountRepository;

@Service
public class AllegroAuthService {

	public enum DeviceStatus {
		IDLE, WAITING, AUTHORIZED, ERROR, EXPIRED
	}

	public record DeviceStatusDto(
			DeviceStatus status,
			String userCode,
			String verificationUri,
			String verificationUriComplete,
			String message) {
	}

	private record DeviceSession(
			String deviceCode,
			String userCode,
			String verificationUri,
			String verificationUriComplete,
			Instant expiresAt,
			int intervalSeconds,
			DeviceStatus status,
			String message) {
	}

	private final AllegroAccountRepository accounts;
	private final AllegroClient allegroClient;
	private final SecretCipher secretCipher;
	private final ExecutorService virtualThreadExecutor;
	private final Map<Long, DeviceSession> sessions = new ConcurrentHashMap<>();

	public AllegroAuthService(
			AllegroAccountRepository accounts,
			AllegroClient allegroClient,
			SecretCipher secretCipher,
			ExecutorService virtualThreadExecutor) {
		this.accounts = accounts;
		this.allegroClient = allegroClient;
		this.secretCipher = secretCipher;
		this.virtualThreadExecutor = virtualThreadExecutor;
	}

	@Transactional
	public DeviceStatusDto startDeviceFlow(Long accountId) {
		AllegroAccount account = accounts.findById(accountId)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono konta Allegro"));
		String secret = secretCipher.decrypt(account.getClientSecretEnc());
		if (secret == null) {
			throw ApiException.badRequest("Uzupełnij Client Secret przed autoryzacją");
		}
		AllegroClient.DeviceStart start = allegroClient.startDeviceFlow(
				account.authBaseUrl(), account.getClientId(), secret);
		sessions.put(accountId, new DeviceSession(
				start.deviceCode(),
				start.userCode(),
				start.verificationUri(),
				start.verificationUriComplete(),
				Instant.now().plusSeconds(start.expiresIn()),
				start.intervalSeconds(),
				DeviceStatus.WAITING,
				null));
		virtualThreadExecutor.submit(() -> pollUntilDone(accountId));
		return status(accountId);
	}

	public DeviceStatusDto status(Long accountId) {
		DeviceSession session = sessions.get(accountId);
		if (session == null) {
			return new DeviceStatusDto(DeviceStatus.IDLE, null, null, null, null);
		}
		return new DeviceStatusDto(
				session.status(),
				session.userCode(),
				session.verificationUri(),
				session.verificationUriComplete(),
				session.message());
	}

	public String accessToken(AllegroAccount account) {
		ensureFreshToken(account);
		return secretCipher.decrypt(account.getAccessTokenEnc());
	}

	@Transactional
	public void ensureFreshToken(AllegroAccount account) {
		if (account.getRefreshTokenEnc() == null) {
			throw ApiException.badRequest("Konto Allegro „" + account.getName() + "” nie jest jeszcze autoryzowane");
		}
		Instant expires = account.getTokenExpiresAt();
		if (expires != null && expires.isAfter(Instant.now().plusSeconds(120))) {
			return;
		}
		refresh(account);
	}

	@Transactional
	public void refreshAfterUnauthorized(AllegroAccount account) {
		refresh(account);
	}

	private void refresh(AllegroAccount account) {
		String secret = secretCipher.decrypt(account.getClientSecretEnc());
		String refreshToken = secretCipher.decrypt(account.getRefreshTokenEnc());
		AllegroClient.TokenPair pair = allegroClient.refreshToken(
				account.authBaseUrl(), account.getClientId(), secret, refreshToken);
		storeTokens(account, pair);
	}

	private void pollUntilDone(Long accountId) {
		try {
			while (true) {
				DeviceSession session = sessions.get(accountId);
				if (session == null) {
					return;
				}
				if (Instant.now().isAfter(session.expiresAt())) {
					sessions.put(accountId, withStatus(session, DeviceStatus.EXPIRED, "Kod wygasł – uruchom autoryzację ponownie"));
					return;
				}
				AllegroAccount account = accounts.findById(accountId).orElse(null);
				if (account == null) {
					return;
				}
				String secret = secretCipher.decrypt(account.getClientSecretEnc());
				Optional<AllegroClient.TokenPair> token = allegroClient.pollDeviceToken(
						account.authBaseUrl(), account.getClientId(), secret, session.deviceCode());
				if (token.isPresent()) {
					storeTokens(account, token.get());
					try {
						String login = allegroClient.fetchLogin(account.apiBaseUrl(), token.get().accessToken());
						account.setAllegroLogin(login);
						accounts.save(account);
					} catch (Exception ignored) {
						accounts.save(account);
					}
					sessions.put(accountId, withStatus(session, DeviceStatus.AUTHORIZED, "Konto połączone"));
					return;
				}
				Thread.sleep(DurationSafe(session.intervalSeconds()));
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (Exception ex) {
			DeviceSession session = sessions.get(accountId);
			if (session != null) {
				sessions.put(accountId, withStatus(session, DeviceStatus.ERROR, ex.getMessage()));
			}
		}
	}

	private void storeTokens(AllegroAccount account, AllegroClient.TokenPair pair) {
		account.setAccessTokenEnc(secretCipher.encrypt(pair.accessToken()));
		account.setRefreshTokenEnc(secretCipher.encrypt(pair.refreshToken()));
		account.setTokenExpiresAt(pair.expiresAt());
		account.setLastError(null);
		accounts.save(account);
	}

	private static DeviceSession withStatus(DeviceSession session, DeviceStatus status, String message) {
		return new DeviceSession(
				session.deviceCode(),
				session.userCode(),
				session.verificationUri(),
				session.verificationUriComplete(),
				session.expiresAt(),
				session.intervalSeconds(),
				status,
				message);
	}

	private static java.time.Duration DurationSafe(int seconds) {
		return java.time.Duration.ofSeconds(Math.max(5, seconds));
	}
}
