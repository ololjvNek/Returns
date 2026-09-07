package pl.returns.ledger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import pl.returns.common.ApiException;

/**
 * Miesiąc ewidencji wynika wyłącznie z daty zwrotu (kolumna „Data zwrotu”).
 */
public final class LedgerPeriod {

	private static final Locale PL = Locale.forLanguageTag("pl-PL");

	private LedgerPeriod() {
	}

	public static LocalDate fromReturnDate(LocalDate returnDate) {
		if (returnDate == null) {
			throw ApiException.badRequest("Data zwrotu jest wymagana do przypisania miesiąca ewidencji");
		}
		return returnDate.withDayOfMonth(1);
	}

	public static String isoMonth(LocalDate returnDate) {
		return YearMonth.from(fromReturnDate(returnDate)).toString();
	}

	public static String title(LocalDate returnDate) {
		return title(YearMonth.from(fromReturnDate(returnDate)));
	}

	public static String title(YearMonth period) {
		return period.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, PL).toUpperCase(PL);
	}

	public static String titleWithYear(LocalDate returnDate) {
		YearMonth month = YearMonth.from(fromReturnDate(returnDate));
		return title(month) + " " + month.getYear();
	}
}
