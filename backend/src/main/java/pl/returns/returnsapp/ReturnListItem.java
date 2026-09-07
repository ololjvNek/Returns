package pl.returns.returnsapp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.returns.domain.LocalReturnState;

public record ReturnListItem(
		UUID id,
		Long accountId,
		String accountName,
		String referenceNumber,
		String orderId,
		Instant createdAt,
		String status,
		String buyerLogin,
		List<String> itemNames,
		LocalReturnState localState,
		String apiloOrderNumber) {
}
