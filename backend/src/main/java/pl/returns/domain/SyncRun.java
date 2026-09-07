package pl.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sync_run")
public class SyncRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private AllegroAccount account;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt = Instant.now();

	@Column(name = "finished_at")
	private Instant finishedAt;

	private Integer fetched;

	@Column(name = "new_count")
	private Integer newCount;

	private String error;

	protected SyncRun() {
	}

	public SyncRun(AllegroAccount account) {
		this.account = account;
	}

	public Long getId() {
		return id;
	}

	public void setFinishedAt(Instant finishedAt) {
		this.finishedAt = finishedAt;
	}

	public void setFetched(Integer fetched) {
		this.fetched = fetched;
	}

	public void setNewCount(Integer newCount) {
		this.newCount = newCount;
	}

	public void setError(String error) {
		this.error = error;
	}
}
