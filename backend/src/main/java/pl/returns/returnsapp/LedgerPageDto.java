package pl.returns.returnsapp;

import java.math.BigDecimal;
import java.util.List;

public record LedgerPageDto(
		String period,
		String monthTitle,
		List<LedgerRowDto> rows,
		BigDecimal totalGross,
		BigDecimal totalVat,
		BigDecimal vatRatePercent) {
}
