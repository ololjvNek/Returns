package pl.returns.apilo;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import org.springframework.stereotype.Component;
import pl.returns.common.ApiException;
import pl.returns.settings.SettingsService;

@Component
public class ApiloRateLimiter {

	private final SettingsService settingsService;
	private volatile Bucket bucket;
	private volatile int lastLimit = -1;

	public ApiloRateLimiter(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public void acquire() {
		try {
			boolean ok = bucket().asBlocking().tryConsume(1, Duration.ofMinutes(2));
			if (!ok) {
				throw ApiException.serviceUnavailable("Limit zapytań Apilo został wyczerpany – spróbuj za chwilę");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw ApiException.serviceUnavailable("Przerwano oczekiwanie na limit Apilo");
		}
	}

	private Bucket bucket() {
		int limit = settingsService.effectiveApiloLimitPerMinute();
		if (bucket == null || limit != lastLimit) {
			synchronized (this) {
				if (bucket == null || limit != lastLimit) {
					Bandwidth bandwidth = Bandwidth.builder()
							.capacity(limit)
							.refillGreedy(limit, Duration.ofMinutes(1))
							.build();
					bucket = Bucket.builder().addLimit(bandwidth).build();
					lastLimit = limit;
				}
			}
		}
		return bucket;
	}
}
