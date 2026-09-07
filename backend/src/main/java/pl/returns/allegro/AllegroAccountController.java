package pl.returns.allegro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.returns.common.ApiException;
import pl.returns.crypto.SecretCipher;
import pl.returns.domain.AllegroAccount;
import pl.returns.domain.AllegroAccountRepository;

@RestController
@RequestMapping("/api/allegro-accounts")
public class AllegroAccountController {

	private final AllegroAccountRepository repository;
	private final SecretCipher secretCipher;
	private final AllegroAuthService authService;
	private final AllegroReturnSyncJob syncJob;

	public AllegroAccountController(
			AllegroAccountRepository repository,
			SecretCipher secretCipher,
			AllegroAuthService authService,
			AllegroReturnSyncJob syncJob) {
		this.repository = repository;
		this.secretCipher = secretCipher;
		this.authService = authService;
		this.syncJob = syncJob;
	}

	@GetMapping
	public List<AllegroAccountDto> list() {
		return repository.findAll().stream().map(this::toDto).toList();
	}

	@PostMapping
	@Transactional
	public AllegroAccountDto create(@Valid @RequestBody UpsertRequest request) {
		AllegroAccount account = new AllegroAccount(request.name(), request.clientId());
		account.setSandbox(Boolean.TRUE.equals(request.sandbox()));
		account.setEnabled(request.enabled() == null || request.enabled());
		if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
			account.setClientSecretEnc(secretCipher.encrypt(request.clientSecret()));
		}
		return toDto(repository.save(account));
	}

	@PutMapping("/{id}")
	@Transactional
	public AllegroAccountDto update(@PathVariable Long id, @Valid @RequestBody UpsertRequest request) {
		AllegroAccount account = repository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono konta Allegro"));
		account.setName(request.name());
		account.setClientId(request.clientId());
		if (request.sandbox() != null) {
			account.setSandbox(request.sandbox());
		}
		if (request.enabled() != null) {
			account.setEnabled(request.enabled());
		}
		if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
			account.setClientSecretEnc(secretCipher.encrypt(request.clientSecret()));
		}
		return toDto(account);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		if (!repository.existsById(id)) {
			throw ApiException.notFound("Nie znaleziono konta Allegro");
		}
		repository.deleteById(id);
	}

	@PostMapping("/{id}/device-flow/start")
	public AllegroAuthService.DeviceStatusDto start(@PathVariable Long id) {
		return authService.startDeviceFlow(id);
	}

	@GetMapping("/{id}/device-flow/status")
	public AllegroAuthService.DeviceStatusDto status(@PathVariable Long id) {
		return authService.status(id);
	}

	@PostMapping("/{id}/sync")
	public void syncOne(@PathVariable Long id) {
		syncJob.syncAccount(id);
	}

	@PostMapping("/sync")
	public void syncAll() {
		syncJob.syncAllAccounts(true);
	}

	private AllegroAccountDto toDto(AllegroAccount account) {
		return new AllegroAccountDto(
				account.getId(),
				account.getName(),
				account.getClientId(),
				account.isSandbox(),
				account.isEnabled(),
				account.getAllegroLogin(),
				account.getLastSyncAt(),
				account.getLastError(),
				account.isAuthorized(),
				account.getTokenExpiresAt(),
				account.getClientSecretEnc() != null);
	}

	public record UpsertRequest(
			@NotBlank String name,
			@NotBlank String clientId,
			String clientSecret,
			Boolean sandbox,
			Boolean enabled) {
	}

	public record AllegroAccountDto(
			Long id,
			String name,
			String clientId,
			boolean sandbox,
			boolean enabled,
			String allegroLogin,
			Instant lastSyncAt,
			String lastError,
			boolean authorized,
			Instant tokenExpiresAt,
			boolean secretConfigured) {
	}
}
