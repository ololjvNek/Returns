package pl.returns.apilo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.http.HttpClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pl.returns.domain.ApiloConnection;
import pl.returns.settings.SettingsService;
import tools.jackson.databind.json.JsonMapper;

class ApiloClientWireMockTest {

	@RegisterExtension
	static WireMockExtension wm = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@Test
	void znajdujeZamowieniePoIdExternal() {
		wm.stubFor(get(urlPathEqualTo("/rest/api/orders/"))
				.withQueryParam("idExternal", equalTo("order-1"))
				.withHeader("Authorization", equalTo("Bearer apilo-token"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{
								  "orders": [
								    {
								      "id": "AL0012345",
								      "idExternal": "order-1",
								      "orderedAt": "2026-08-10T12:00:00+02:00",
								      "addressCustomer": { "name": "Wioleta Glik" },
								      "orderItems": [
								        { "idExternal": "111", "ean": "5903682320891", "quantity": 2 }
								      ]
								    }
								  ]
								}
								""")));

		SettingsService settings = mock(SettingsService.class);
		when(settings.userAgent()).thenReturn("Returns-Ewidencja/1.0.0");
		when(settings.effectiveApiloLimitPerMinute()).thenReturn(200);
		ApiloRateLimiter limiter = new ApiloRateLimiter(settings);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> values = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(values);
		when(values.get("apilo:order:order-1")).thenReturn(null);

		ApiloClient client = new ApiloClient(HttpClient.newHttpClient(), JsonMapper.builder().build(), limiter, redis, settings);
		ApiloConnection connection = new ApiloConnection();
		connection.setBaseUrl(wm.getRuntimeInfo().getHttpBaseUrl());
		Optional<ApiloOrder> order = client.findByExternalId(connection, "apilo-token", "order-1");
		assertTrue(order.isPresent());
		assertEquals("AL0012345", order.get().id());
		assertEquals("Wioleta Glik", order.get().customerName());
	}
}
