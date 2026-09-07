package pl.returns.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenBucketLimiterTest {

	@Test
	void drugiRequestCzekaOkołoSekundy() {
		TokenBucketLimiter limiter = new TokenBucketLimiter(1, Duration.ofSeconds(1));
		limiter.acquire();
		long start = System.nanoTime();
		limiter.acquire();
		long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
		assertTrue(elapsedMs >= 700, "Oczekiwano ok. 1 s, było " + elapsedMs + " ms");
	}
}
