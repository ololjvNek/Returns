package pl.returns.returnsapp;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecognizeRequest(
		String apiloOrderNumber,
		LocalDate saleDate,
		String buyerName,
		String productCodes,
		@NotNull LocalDate returnDate,
		@NotNull BigDecimal grossAmount,
		String currency,
		boolean amountEstimated,
		BigDecimal vatAmount,
		String notes) {
}
