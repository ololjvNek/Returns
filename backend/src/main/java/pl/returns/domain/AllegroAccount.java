package pl.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "allegro_account")
public class AllegroAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "client_id", nullable = false)
	private String clientId;

	@Column(name = "client_secret_enc")
	private String clientSecretEnc;

	@Column(name = "access_token_enc")
	private String accessTokenEnc;

	@Column(name = "refresh_token_enc")
	private String refreshTokenEnc;

	@Column(name = "token_expires_at")
	private Instant tokenExpiresAt;

	@Column(name = "allegro_login")
	private String allegroLogin;

	@Column(nullable = false)
	private boolean sandbox;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "last_sync_at")
	private Instant lastSyncAt;

	@Column(name = "last_error")
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected AllegroAccount() {
	}

	public AllegroAccount(String name, String clientId) {
		this.name = name;
		this.clientId = clientId;
	}

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecretEnc() {
		return clientSecretEnc;
	}

	public void setClientSecretEnc(String clientSecretEnc) {
		this.clientSecretEnc = clientSecretEnc;
	}

	public String getAccessTokenEnc() {
		return accessTokenEnc;
	}

	public void setAccessTokenEnc(String accessTokenEnc) {
		this.accessTokenEnc = accessTokenEnc;
	}

	public String getRefreshTokenEnc() {
		return refreshTokenEnc;
	}

	public void setRefreshTokenEnc(String refreshTokenEnc) {
		this.refreshTokenEnc = refreshTokenEnc;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(Instant tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}

	public String getAllegroLogin() {
		return allegroLogin;
	}

	public void setAllegroLogin(String allegroLogin) {
		this.allegroLogin = allegroLogin;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	public void setSandbox(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getLastSyncAt() {
		return lastSyncAt;
	}

	public void setLastSyncAt(Instant lastSyncAt) {
		this.lastSyncAt = lastSyncAt;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public boolean isAuthorized() {
		return accessTokenEnc != null && !accessTokenEnc.isBlank();
	}

	public String apiBaseUrl() {
		return sandbox ? "https://api.allegro.pl.allegrosandbox.pl" : "https://api.allegro.pl";
	}

	public String authBaseUrl() {
		return sandbox ? "https://allegro.pl.allegrosandbox.pl" : "https://allegro.pl";
	}
}
