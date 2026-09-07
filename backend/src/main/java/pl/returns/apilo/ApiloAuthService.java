package pl.returns.apilo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.returns.common.ApiException;
import pl.returns.crypto.SecretCipher;
import pl.returns.domain.ApiloConnection;
import pl.returns.domain.ApiloConnectionRepository;
import tools.jackson.databind.JsonNode;

@Service
public class ApiloAuthService {

	private final ApiloConnectionRepository repository;
	private final ApiloClient client;
	private final SecretCipher secretCipher;

	public ApiloAuthService(ApiloConnectionRepository repository, ApiloClient client, SecretCipher secretCipher) {
		this.repository = repository;
		this.client = client;
		this.secretCipher = secretCipher;
	}

	@Transactional
	public ApiloConnection connection() {
		return repository.findById((short) 1).orElseGet(() -> {
			ApiloConnection created = new ApiloConnection();
			return repository.save(created);
		});
	}

	@Transactional
	public void exchangeAuthorizationCode(String authorizationCode) {
		ApiloConnection connection = connection();
		String secret = secretCipher.decrypt(connection.getClientSecretEnc());
		if (connection.getClientId() == null || secret == null) {
			throw ApiException.badRequest("Uzupełnij Client ID i Client Secret Apilo");
		}
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw ApiException.badRequest("Podaj kod autoryzacyjny z panelu Apilo");
		}
		storeTokens(connection, client.exchangeToken(connection, secret, "authorization_code", authorizationCode.trim()));
	}

	@Transactional
	public String accessToken() {
		ApiloConnection connection = connection();
		ensureFresh(connection);
		String token = secretCipher.decrypt(connection.getAccessTokenEnc());
		if (token == null) {
			throw ApiException.badRequest("Apilo nie jest jeszcze autoryzowane");
		}
		return token;
	}

	@Transactional
	public void ensureFresh(ApiloConnection connection) {
		if (connection.getRefreshTokenEnc() == null) {
			return;
		}
		Instant accessExp = connection.getAccessExpiresAt();
		if (accessExp != null && accessExp.isAfter(Instant.now().plus(2, ChronoUnit.DAYS))) {
			return;
		}
		refresh(connection);
	}

	@Transactional
	public void refreshIfUnauthorized() {
		refresh(connection());
	}

	private void refresh(ApiloConnection connection) {
		String secret = secretCipher.decrypt(connection.getClientSecretEnc());
		String refresh = secretCipher.decrypt(connection.getRefreshTokenEnc());
		if (secret == null || refresh == null) {
			throw ApiException.badRequest("Brak danych do odświeżenia tokenu Apilo");
		}
		storeTokens(connection, client.exchangeToken(connection, secret, "refresh_token", refresh));
	}

	private void storeTokens(ApiloConnection connection, JsonNode json) {
		String access = first(json, "accessToken", "access_token");
		String refresh = first(json, "refreshToken", "refresh_token");
		if (access == null) {
			throw ApiException.serviceUnavailable("Apilo nie zwróciło accessToken");
		}
		connection.setAccessTokenEnc(secretCipher.encrypt(access));
		if (refresh != null) {
			connection.setRefreshTokenEnc(secretCipher.encrypt(refresh));
		}
		int expires = json.path("expiresIn").asInt(json.path("expires_in").asInt(21 * 24 * 3600));
		connection.setAccessExpiresAt(Instant.now().plusSeconds(Math.max(3600, expires)));
		connection.setRefreshExpiresAt(Instant.now().plus(60, ChronoUnit.DAYS));
		connection.setLastError(null);
		repository.save(connection);
	}

	private static String first(JsonNode json, String a, String b) {
		JsonNode n = json.get(a);
		if (n == null || n.isNull()) {
			n = json.get(b);
		}
		if (n == null || n.isNull()) {
			return null;
		}
		String text = n.asString();
		return text == null || text.isBlank() ? null : text;
	}
}
