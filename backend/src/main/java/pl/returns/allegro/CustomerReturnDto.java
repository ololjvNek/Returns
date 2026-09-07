package pl.returns.allegro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerReturnDto(
		UUID id,
		Instant createdAt,
		String referenceNumber,
		String orderId,
		String status,
		String buyerLogin,
		String buyerEmail,
		List<ReturnedItemDto> items,
		String itemsJson,
		String parcelsJson,
		String rawJson) {
}
