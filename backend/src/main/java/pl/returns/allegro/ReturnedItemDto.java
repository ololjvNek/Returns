package pl.returns.allegro;

import java.math.BigDecimal;

public record ReturnedItemDto(
		String offerId,
		int quantity,
		String name,
		BigDecimal amount,
		String currency) {
}
