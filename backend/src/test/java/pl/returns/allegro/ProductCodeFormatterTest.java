package pl.returns.allegro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductCodeFormatterTest {

	@Test
	void laczyKodyIIlosciJakWArkuszu() {
		var items = List.of(
				new ReturnedItemDto("111", 2, "Produkt A", new BigDecimal("10.00"), "PLN"),
				new ReturnedItemDto("222", 1, "Produkt B", new BigDecimal("5.00"), "PLN"));
		var catalog = List.of(
				new ProductCodeFormatter.CatalogItem("111", "5903682320891", "SKU-A", null, "Produkt A"),
				new ProductCodeFormatter.CatalogItem("222", "5903682320000", null, null, "Produkt B"));
		assertEquals("5903682320891 x2, 5903682320000", ProductCodeFormatter.format(items, catalog));
	}

	@Test
	void sumujeWartoscPozycji() {
		var items = List.of(
				new ReturnedItemDto("111", 2, "A", new BigDecimal("10.00"), "PLN"),
				new ReturnedItemDto("222", 1, "B", new BigDecimal("5.50"), "PLN"));
		assertEquals(new BigDecimal("25.50"), ProductCodeFormatter.sumItemValues(items));
	}

	@Test
	void uzywaNazwyGdyBrakEan() {
		var items = List.of(new ReturnedItemDto("999", 1, "Nazwa z Allegro", new BigDecimal("1"), "PLN"));
		assertEquals("Nazwa z Allegro", ProductCodeFormatter.format(items, List.of()));
	}
}
