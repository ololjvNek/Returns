package pl.returns.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VatCalculatorTest {

	@Test
	void liczyVatZBrutto23Procent() {
		BigDecimal vat = VatCalculator.fromGross(new BigDecimal("123.00"), new BigDecimal("23"));
		assertEquals(new BigDecimal("23.00"), vat);
	}

	@Test
	void liczyVatDlaKwotyZArkusz() {
		BigDecimal vat = VatCalculator.fromGross(new BigDecimal("17.49"), new BigDecimal("23"));
		assertEquals(new BigDecimal("3.27"), vat);
	}
}
