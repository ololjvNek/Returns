package pl.returns.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VatCalculator {

	private VatCalculator() {
	}

	public static BigDecimal fromGross(BigDecimal gross, BigDecimal ratePercent) {
		if (gross == null) {
			return null;
		}
		BigDecimal rate = ratePercent.movePointLeft(2);
		BigDecimal net = gross.divide(BigDecimal.ONE.add(rate), 10, RoundingMode.HALF_UP);
		return gross.subtract(net).setScale(2, RoundingMode.HALF_UP);
	}
}
