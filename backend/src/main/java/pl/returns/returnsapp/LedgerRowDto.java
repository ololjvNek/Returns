package pl.returns.returnsapp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerRowDto(
		Long id,
		int lp,
		String apiloOrderNumber,
		LocalDate saleDate,
		String buyerName,
		String productCodes,
		LocalDate returnDate,
		BigDecimal grossAmount,
		String currency,
		boolean amountEstimated,
		BigDecimal vatAmount,
		String notes,
		UUID allegroReturnId,
		String ledgerPeriod) {
}
