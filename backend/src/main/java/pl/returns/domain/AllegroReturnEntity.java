package pl.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "allegro_return")
public class AllegroReturnEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private AllegroAccount account;

	@Column(name = "reference_number")
	private String referenceNumber;

	@Column(name = "order_id")
	private String orderId;

	@Column(name = "created_at")
	private Instant createdAt;

	private String status;

	@Column(name = "buyer_login")
	private String buyerLogin;

	@Column(name = "buyer_email")
	private String buyerEmail;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String items = "[]";

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String parcels = "[]";

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String raw;

	@Enumerated(EnumType.STRING)
	@Column(name = "local_state", nullable = false)
	private LocalReturnState localState = LocalReturnState.NEW;

	@Column(name = "apilo_order_number")
	private String apiloOrderNumber;

	@Column(name = "first_seen_at", nullable = false)
	private Instant firstSeenAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected AllegroReturnEntity() {
	}

	public AllegroReturnEntity(UUID id, AllegroAccount account) {
		this.id = id;
		this.account = account;
	}

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public AllegroAccount getAccount() {
		return account;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getBuyerLogin() {
		return buyerLogin;
	}

	public void setBuyerLogin(String buyerLogin) {
		this.buyerLogin = buyerLogin;
	}

	public String getBuyerEmail() {
		return buyerEmail;
	}

	public void setBuyerEmail(String buyerEmail) {
		this.buyerEmail = buyerEmail;
	}

	public String getItems() {
		return items;
	}

	public void setItems(String items) {
		this.items = items == null ? "[]" : items;
	}

	public String getParcels() {
		return parcels;
	}

	public void setParcels(String parcels) {
		this.parcels = parcels == null ? "[]" : parcels;
	}

	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
	}

	public LocalReturnState getLocalState() {
		return localState;
	}

	public void setLocalState(LocalReturnState localState) {
		this.localState = localState;
	}

	public String getApiloOrderNumber() {
		return apiloOrderNumber;
	}

	public void setApiloOrderNumber(String apiloOrderNumber) {
		this.apiloOrderNumber = apiloOrderNumber;
	}

	public Instant getFirstSeenAt() {
		return firstSeenAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
