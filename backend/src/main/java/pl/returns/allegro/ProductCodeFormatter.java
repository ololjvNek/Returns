package pl.returns.allegro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductCodeFormatter {

	private ProductCodeFormatter() {
	}

	public record CatalogItem(String idExternal, String ean, String sku, String originalCode, String name) {
	}

	public static String format(List<ReturnedItemDto> returnedItems, List<CatalogItem> catalog) {
		if (returnedItems == null || returnedItems.isEmpty()) {
			return "";
		}
		Map<String, Integer> quantities = new LinkedHashMap<>();
		for (ReturnedItemDto item : returnedItems) {
			String code = resolveCode(item, catalog);
			quantities.merge(code, Math.max(item.quantity(), 1), Integer::sum);
		}
		List<String> parts = new ArrayList<>();
		quantities.forEach((code, qty) -> {
			if (qty > 1) {
				parts.add(code + " x" + qty);
			} else {
				parts.add(code);
			}
		});
		return String.join(", ", parts);
	}

	public static BigDecimal sumItemValues(List<ReturnedItemDto> returnedItems) {
		if (returnedItems == null || returnedItems.isEmpty()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (ReturnedItemDto item : returnedItems) {
			BigDecimal amount = item.amount() == null ? BigDecimal.ZERO : item.amount();
			int qty = Math.max(item.quantity(), 1);
			sum = sum.add(amount.multiply(BigDecimal.valueOf(qty)));
		}
		return sum.setScale(2, RoundingMode.HALF_UP);
	}

	private static String resolveCode(ReturnedItemDto item, List<CatalogItem> catalog) {
		if (catalog != null && item.offerId() != null) {
			for (CatalogItem cat : catalog) {
				if (item.offerId().equals(cat.idExternal())) {
					String code = firstNonBlank(cat.ean(), cat.sku(), cat.originalCode(), cat.name());
					if (code != null) {
						return code;
					}
				}
			}
		}
		return firstNonBlank(item.name(), item.offerId(), "brak kodu");
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}
}
