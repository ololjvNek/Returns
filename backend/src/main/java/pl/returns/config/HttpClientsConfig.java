package pl.returns.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientsConfig {

	@Bean
	HttpClient httpClient(ExecutorService virtualThreadExecutor) {
		return HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(20))
				.executor(virtualThreadExecutor)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}
}
