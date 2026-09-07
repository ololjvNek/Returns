package pl.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "return_record")
public class ReturnRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "allegro_return_id")
	private AllegroReturnEntity allegroReturn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private AllegroAccount account;

	@Column(name = "apilo_order_number")
	private String apiloOrderNumber;

	@Column(name = "sale_date")
	private LocalDate saleDate;

	@Column(name = "buyer_name")
	private String buyerName;

	@Column(name = "product_codes")
	private String productCodes;

	@Column(name = "return_date", nullable = false)
	private LocalDate returnDate;

	@Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossAmount;

	@Column(nullable = false, length = 8)
	private String currency = "PLN";

	@Column(name = "amount_estimated", nullable = false)
	private boolean amountEstimated;

	@Column(name = "vat_amount", precision = 12, scale = 2)
	private BigDecimal vatAmount;

	private String notes;

	@Column(nullable = false)
	private LocalDate period;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public AllegroReturnEntity getAllegroReturn() {
		return allegroReturn;
	}

	public void setAllegroReturn(AllegroReturnEntity allegroReturn) {
		this.allegroReturn = allegroReturn;
	}

	public AllegroAccount getAccount() {
		return account;
	}

	public void setAccount(AllegroAccount account) {
		this.account = account;
	}

	public String getApiloOrderNumber() {
		return apiloOrderNumber;
	}

	public void setApiloOrderNumber(String apiloOrderNumber) {
		this.apiloOrderNumber = apiloOrderNumber;
	}

	public LocalDate getSaleDate() {
		return saleDate;
	}

	public void setSaleDate(LocalDate saleDate) {
		this.saleDate = saleDate;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public String getProductCodes() {
		return productCodes;
	}

	public void setProductCodes(String productCodes) {
		this.productCodes = productCodes;
	}

	public LocalDate getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(LocalDate returnDate) {
		this.returnDate = returnDate;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public void setGrossAmount(BigDecimal grossAmount) {
		this.grossAmount = grossAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public boolean isAmountEstimated() {
		return amountEstimated;
	}

	public void setAmountEstimated(boolean amountEstimated) {
		this.amountEstimated = amountEstimated;
	}

	public BigDecimal getVatAmount() {
		return vatAmount;
	}

	public void setVatAmount(BigDecimal vatAmount) {
		this.vatAmount = vatAmount;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDate getPeriod() {
		return period;
	}

	public void setPeriod(LocalDate period) {
		this.period = period;
	}
}
