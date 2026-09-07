package pl.returns.apilo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pl.returns.common.ApiException;
import pl.returns.common.HttpJson;
import pl.returns.domain.ApiloConnection;
import pl.returns.settings.SettingsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ApiloClient {

	private final HttpClient httpClient;
	private final JsonMapper jsonMapper;
	private final ApiloRateLimiter rateLimiter;
	private final StringRedisTemplate redis;
	private final SettingsService settingsService;

	public ApiloClient(
			HttpClient httpClient,
			JsonMapper jsonMapper,
			ApiloRateLimiter rateLimiter,
			StringRedisTemplate redis,
			SettingsService settingsService) {
		this.httpClient = httpClient;
		this.jsonMapper = jsonMapper;
		this.rateLimiter = rateLimiter;
		this.redis = redis;
		this.settingsService = settingsService;
	}

	public JsonNode exchangeToken(ApiloConnection connection, String clientSecret, String grantType, String token) {
		String payload = """
				{"grantType":"%s","token":"%s"}
				""".formatted(escape(grantType), escape(token));
		HttpRequest request = HttpRequest.newBuilder(URI.create(trimBase(connection.getBaseUrl()) + "/rest/auth/token/"))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", HttpJson.basicAuth(connection.getClientId(), clientSecret))
				.header("Accept", "application/json")
				.header("Content-Type", "application/json")
				.header("User-Agent", settingsService.userAgent())
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();
		return send(request, false);
	}

	public Optional<ApiloOrder> findByExternalId(ApiloConnection connection, String accessToken, String idExternal) {
		if (idExternal == null || idExternal.isBlank()) {
			return Optional.empty();
		}
		String cacheKey = "apilo:order:" + idExternal;
		String cached = cacheGet(cacheKey);
		if (cached != null) {
			return Optional.ofNullable(parseOrder(jsonMapper.readTree(cached)));
		}
		rateLimiter.acquire();
		String uri = trimBase(connection.getBaseUrl()) + "/rest/api/orders/?idExternal="
				+ URLEncoder.encode(idExternal, StandardCharsets.UTF_8) + "&limit=5";
		JsonNode json = send(authorizedGet(uri, accessToken), true);
		JsonNode orders = json.get("orders");
		if (orders == null || !orders.isArray() || orders.isEmpty()) {
			return Optional.empty();
		}
		JsonNode first = orders.get(0);
		String apiloId = text(first, "id");
		JsonNode detail = first;
		if (apiloId != null && (first.get("orderItems") == null || !first.get("orderItems").isArray())) {
			rateLimiter.acquire();
			detail = send(authorizedGet(trimBase(connection.getBaseUrl()) + "/rest/api/orders/"
					+ URLEncoder.encode(apiloId, StandardCharsets.UTF_8) + "/", accessToken), true);
		}
		cachePut(cacheKey, jsonMapper.writeValueAsString(detail));
		return Optional.ofNullable(parseOrder(detail));
	}

	public void testConnection(ApiloConnection connection, String accessToken) {
		rateLimiter.acquire();
		String uri = trimBase(connection.getBaseUrl()) + "/rest/api/orders/?limit=1";
		send(authorizedGet(uri, accessToken), true);
	}

	private ApiloOrder parseOrder(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		JsonNode address = node.get("addressCustomer");
		String name = address != null ? text(address, "name") : null;
		return new ApiloOrder(
				text(node, "id"),
				text(node, "idExternal"),
				ApiloOrder.parseTime(first(text(node, "orderedAt"), text(node, "createdAt"))),
				name,
				ApiloOrder.parseItems(node.get("orderItems")));
	}

	private HttpRequest authorizedGet(String uri, String accessToken) {
		return HttpRequest.newBuilder(URI.create(uri))
				.timeout(Duration.ofSeconds(45))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/json")
				.header("Content-Type", "application/json")
				.header("User-Agent", settingsService.userAgent())
				.GET()
				.build();
	}

	private JsonNode send(HttpRequest request, boolean retry429) {
		HttpResponse<String> response = raw(request);
		if (retry429 && response.statusCode() == 429) {
			sleepRetry(response);
			rateLimiter.acquire();
			response = raw(request);
		}
		int status = response.statusCode();
		if (status == 401) {
			throw new ApiloUnauthorizedException("Token Apilo wygasł");
		}
		if (status >= 400) {
			throw ApiException.serviceUnavailable("Apilo API: HTTP " + status + " " + shorten(response.body()));
		}
		return jsonMapper.readTree(response.body());
	}

	private HttpResponse<String> raw(HttpRequest request) {
		try {
			return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			throw ApiException.serviceUnavailable("Błąd połączenia z Apilo: " + ex.getMessage());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw ApiException.serviceUnavailable("Przerwano połączenie z Apilo");
		}
	}

	private void sleepRetry(HttpResponse<String> response) {
		long seconds = response.headers().firstValue("Retry-After").map(v -> {
			try {
				return Long.parseLong(v.trim());
			} catch (NumberFormatException ex) {
				return 60L;
			}
		}).orElse(60L);
		try {
			Thread.sleep(Duration.ofSeconds(Math.min(Math.max(seconds, 1), 120)));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private String cacheGet(String key) {
		try {
			return redis.opsForValue().get(key);
		} catch (Exception ex) {
			return null;
		}
	}

	private void cachePut(String key, String value) {
		try {
			redis.opsForValue().set(key, value, Duration.ofDays(7));
		} catch (Exception ignored) {
			// Redis jest przyspieszeniem, nie wymaganiem.
		}
	}

	private static String trimBase(String base) {
		if (base == null || base.isBlank()) {
			return "https://api.apilo.com";
		}
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private static String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text;
	}

	private static String first(String a, String b) {
		return a != null ? a : b;
	}

	private static String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String shorten(String body) {
		if (body == null) {
			return "";
		}
		return body.length() > 240 ? body.substring(0, 240) : body;
	}
}
