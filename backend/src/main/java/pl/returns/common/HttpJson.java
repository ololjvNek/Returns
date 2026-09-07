package pl.returns.common;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class HttpJson {

	private HttpJson() {
	}

	public static String basicAuth(String user, String password) {
		String raw = user + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	public static HttpRequest.Builder jsonGet(URI uri, String userAgent) {
		return HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(45))
				.GET()
				.header("User-Agent", userAgent)
				.header("Accept", "application/json");
	}

	public static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asString();
	}

	public static JsonMapper mapper() {
		return JsonMapper.builder().build();
	}
}
