package pl.returns.common;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * Prosty token bucket. Blokuje bieżący wątek (wirtualny) aż pojawi się żeton.
 */
public final class TokenBucketLimiter {

	private final long capacity;
	private final long refillNanos;
	private double tokens;
	private long lastRefillNanos;

	public TokenBucketLimiter(long capacity, Duration refillPeriod) {
		if (capacity < 1) {
			throw new IllegalArgumentException("Pojemność musi być dodatnia");
		}
		this.capacity = capacity;
		this.refillNanos = refillPeriod.toNanos();
		this.tokens = capacity;
		this.lastRefillNanos = System.nanoTime();
	}

	public void acquire() {
		while (true) {
			long waitNanos;
			synchronized (this) {
				refill();
				if (tokens >= 1d) {
					tokens -= 1d;
					return;
				}
				double missing = 1d - tokens;
				waitNanos = (long) Math.ceil(missing * refillNanos / capacity);
			}
			LockSupport.parkNanos(Math.max(waitNanos, 1_000_000L));
		}
	}

	private void refill() {
		long now = System.nanoTime();
		long elapsed = now - lastRefillNanos;
		if (elapsed <= 0) {
			return;
		}
		double add = (elapsed / (double) refillNanos) * capacity;
		tokens = Math.min(capacity, tokens + add);
		lastRefillNanos = now;
	}
}
