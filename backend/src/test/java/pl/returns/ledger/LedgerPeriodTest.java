package pl.returns.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LedgerPeriodTest {

	@Test
	void wrzesniowaDataZwrotuTrafiaDoWrzesnia() {
		LocalDate returnDate = LocalDate.of(2026, 9, 4);
		assertEquals(LocalDate.of(2026, 9, 1), LedgerPeriod.fromReturnDate(returnDate));
		assertEquals("2026-09", LedgerPeriod.isoMonth(returnDate));
		assertEquals("WRZESIEŃ", LedgerPeriod.title(returnDate));
	}

	@Test
	void uznanieWPazdziernikuZDataSierpniaTrafiaDoSierpnia() {
		LocalDate returnDate = LocalDate.of(2026, 8, 28);
		assertEquals("2026-08", LedgerPeriod.isoMonth(returnDate));
		assertEquals("SIERPIEŃ", LedgerPeriod.title(returnDate));
	}
}
