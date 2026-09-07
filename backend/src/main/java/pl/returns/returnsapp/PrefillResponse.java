package pl.returns.returnsapp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrefillResponse(
		UUID returnId,
		String apiloOrderNumber,
		LocalDate saleDate,
		String buyerName,
		String productCodes,
		LocalDate returnDate,
		BigDecimal grossAmount,
		String currency,
		boolean amountEstimated,
		String notes,
		String warning) {
}
