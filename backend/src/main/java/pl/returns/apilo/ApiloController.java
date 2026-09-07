package pl.returns.apilo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.returns.crypto.SecretCipher;
import pl.returns.domain.ApiloConnection;

@RestController
@RequestMapping("/api/apilo")
public class ApiloController {

	private final ApiloAuthService authService;
	private final ApiloClient client;
	private final SecretCipher secretCipher;

	public ApiloController(ApiloAuthService authService, ApiloClient client, SecretCipher secretCipher) {
		this.authService = authService;
		this.client = client;
		this.secretCipher = secretCipher;
	}

	@GetMapping
	public ApiloDto get() {
		return toDto(authService.connection());
	}

	@PutMapping
	@Transactional
	public ApiloDto update(@Valid @RequestBody UpdateRequest request) {
		ApiloConnection connection = authService.connection();
		connection.setBaseUrl(request.baseUrl().trim());
		connection.setClientId(request.clientId().trim());
		if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
			connection.setClientSecretEnc(secretCipher.encrypt(request.clientSecret().trim()));
		}
		return toDto(connection);
	}

	@PostMapping("/authorize")
	public ApiloDto authorize(@Valid @RequestBody AuthorizeRequest request) {
		authService.exchangeAuthorizationCode(request.authorizationCode());
		return toDto(authService.connection());
	}

	@PostMapping("/test")
	public TestResult test() {
		try {
			ApiloConnection connection = authService.connection();
			client.testConnection(connection, authService.accessToken());
			connection.setLastError(null);
			return new TestResult(true, "Połączenie z Apilo działa");
		} catch (Exception ex) {
			ApiloConnection connection = authService.connection();
			connection.setLastError(ex.getMessage());
			return new TestResult(false, ex.getMessage());
		}
	}

	private ApiloDto toDto(ApiloConnection connection) {
		return new ApiloDto(
				connection.getBaseUrl(),
				connection.getClientId(),
				connection.getClientSecretEnc() != null,
				connection.isAuthorized(),
				connection.getAccessExpiresAt(),
				connection.getLastError());
	}

	public record UpdateRequest(@NotBlank String baseUrl, @NotBlank String clientId, String clientSecret) {
	}

	public record AuthorizeRequest(@NotBlank String authorizationCode) {
	}

	public record ApiloDto(
			String baseUrl,
			String clientId,
			boolean secretConfigured,
			boolean authorized,
			java.time.Instant accessExpiresAt,
			String lastError) {
	}

	public record TestResult(boolean ok, String message) {
	}
}
