package pl.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "apilo_connection")
public class ApiloConnection {

	@Id
	private Short id = 1;

	@Column(name = "base_url", nullable = false)
	private String baseUrl = "https://api.apilo.com";

	@Column(name = "client_id")
	private String clientId;

	@Column(name = "client_secret_enc")
	private String clientSecretEnc;

	@Column(name = "access_token_enc")
	private String accessTokenEnc;

	@Column(name = "refresh_token_enc")
	private String refreshTokenEnc;

	@Column(name = "access_expires_at")
	private Instant accessExpiresAt;

	@Column(name = "refresh_expires_at")
	private Instant refreshExpiresAt;

	@Column(name = "last_error")
	private String lastError;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public Short getId() {
		return id;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		this.updatedAt = Instant.now();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
		this.updatedAt = Instant.now();
	}

	public String getClientSecretEnc() {
		return clientSecretEnc;
	}

	public void setClientSecretEnc(String clientSecretEnc) {
		this.clientSecretEnc = clientSecretEnc;
		this.updatedAt = Instant.now();
	}

	public String getAccessTokenEnc() {
		return accessTokenEnc;
	}

	public void setAccessTokenEnc(String accessTokenEnc) {
		this.accessTokenEnc = accessTokenEnc;
		this.updatedAt = Instant.now();
	}

	public String getRefreshTokenEnc() {
		return refreshTokenEnc;
	}

	public void setRefreshTokenEnc(String refreshTokenEnc) {
		this.refreshTokenEnc = refreshTokenEnc;
		this.updatedAt = Instant.now();
	}

	public Instant getAccessExpiresAt() {
		return accessExpiresAt;
	}

	public void setAccessExpiresAt(Instant accessExpiresAt) {
		this.accessExpiresAt = accessExpiresAt;
		this.updatedAt = Instant.now();
	}

	public Instant getRefreshExpiresAt() {
		return refreshExpiresAt;
	}

	public void setRefreshExpiresAt(Instant refreshExpiresAt) {
		this.refreshExpiresAt = refreshExpiresAt;
		this.updatedAt = Instant.now();
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
		this.updatedAt = Instant.now();
	}

	public boolean isAuthorized() {
		return accessTokenEnc != null && !accessTokenEnc.isBlank();
	}
}
