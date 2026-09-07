package pl.returns.allegro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.returns.settings.SettingsService;
import tools.jackson.databind.json.JsonMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class AllegroClientWireMockTest {

	@RegisterExtension
	static WireMockExtension wm = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@Test
	void pobieraListeZwrotowZNaglowkiemBeta() {
		UUID id = UUID.fromString("a3405c27-b01c-4357-9bea-e13925708b46");
		wm.stubFor(get(urlPathEqualTo("/order/customer-returns"))
				.withHeader("Accept", equalTo("application/vnd.allegro.beta.v1+json"))
				.withHeader("Authorization", equalTo("Bearer token-test"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/vnd.allegro.beta.v1+json")
						.withBody("""
								{
								  "customerReturns": [
								    {
								      "id": "%s",
								      "createdAt": "2026-09-04T10:00:00Z",
								      "referenceNumber": "1234/Z04A",
								      "orderId": "order-1",
								      "status": "DELIVERED",
								      "buyer": { "login": "kupujacy", "email": "a@b.pl" },
								      "items": [
								        {
								          "offerId": "111",
								          "quantity": 2,
								          "name": "Kubek",
								          "price": { "amount": "17.49", "currency": "PLN" }
								        }
								      ],
								      "parcels": []
								    }
								  ]
								}
								""".formatted(id))));

		SettingsService settings = mock(SettingsService.class);
		when(settings.userAgent()).thenReturn("Returns-Ewidencja/1.0.0");
		AllegroClient client = new AllegroClient(HttpClient.newHttpClient(), JsonMapper.builder().build(), settings);
		List<CustomerReturnDto> result = client.listCustomerReturns(
				1L, wm.getRuntimeInfo().getHttpBaseUrl(), "token-test", Instant.parse("2026-09-01T00:00:00Z"), 100, 0);
		assertEquals(1, result.size());
		assertEquals(id, result.getFirst().id());
		assertEquals("1234/Z04A", result.getFirst().referenceNumber());
		assertEquals(2, result.getFirst().items().getFirst().quantity());
	}
}
