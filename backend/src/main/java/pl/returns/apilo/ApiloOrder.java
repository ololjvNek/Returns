package pl.returns.apilo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import pl.returns.allegro.ProductCodeFormatter;

public record ApiloOrder(
		String id,
		String idExternal,
		Instant orderedAt,
		String customerName,
		List<ProductCodeFormatter.CatalogItem> items) {

	public LocalDate saleDate() {
		return orderedAt == null ? null : orderedAt.atZone(java.time.ZoneId.of("Europe/Warsaw")).toLocalDate();
	}

	public static Instant parseTime(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (Exception ignored) {
			try {
				return OffsetDateTime.parse(value).toInstant();
			} catch (Exception ex) {
				return null;
			}
		}
	}

	public static List<ProductCodeFormatter.CatalogItem> parseItems(tools.jackson.databind.JsonNode itemsNode) {
		List<ProductCodeFormatter.CatalogItem> items = new ArrayList<>();
		if (itemsNode == null || !itemsNode.isArray()) {
			return items;
		}
		for (var item : itemsNode) {
			items.add(new ProductCodeFormatter.CatalogItem(
					text(item, "idExternal"),
					text(item, "ean"),
					text(item, "sku"),
					text(item, "originalCode"),
					first(text(item, "originalName"), text(item, "name"))));
		}
		return items;
	}

	private static String text(tools.jackson.databind.JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		var value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text;
	}

	private static String first(String a, String b) {
		return a != null ? a : b;
	}
}
