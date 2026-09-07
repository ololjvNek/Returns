package pl.returns.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.returns.common.ApiException;
import pl.returns.domain.AllegroReturnEntity;
import pl.returns.domain.LocalReturnState;
import pl.returns.domain.ReturnRecord;
import pl.returns.domain.ReturnRecordRepository;
import pl.returns.returnsapp.LedgerPageDto;
import pl.returns.returnsapp.LedgerRowDto;
import pl.returns.returnsapp.RecognizeRequest;
import pl.returns.returnsapp.RecognizeReturnService;
import pl.returns.settings.SettingsService;

@Service
public class LedgerService {

	private final ReturnRecordRepository recordRepository;
	private final SettingsService settingsService;

	public LedgerService(ReturnRecordRepository recordRepository, SettingsService settingsService) {
		this.recordRepository = recordRepository;
		this.settingsService = settingsService;
	}

	@Transactional(readOnly = true)
	public LedgerPageDto page(YearMonth period) {
		LocalDate first = period.atDay(1);
		List<ReturnRecord> records = recordRepository.findByPeriodOrderByIdAsc(first);
		List<LedgerRowDto> rows = new java.util.ArrayList<>();
		int lp = 1;
		BigDecimal gross = BigDecimal.ZERO;
		BigDecimal vat = BigDecimal.ZERO;
		for (ReturnRecord record : records) {
			rows.add(toRow(record, lp++));
			if (record.getGrossAmount() != null) {
				gross = gross.add(record.getGrossAmount());
			}
			if (record.getVatAmount() != null) {
				vat = vat.add(record.getVatAmount());
			}
		}
		return new LedgerPageDto(
				period.toString(),
				monthTitle(period),
				rows,
				gross,
				vat,
				settingsService.vatRatePercent());
	}

	@Transactional
	public LedgerRowDto update(Long id, RecognizeRequest request) {
		ReturnRecord record = recordRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono wiersza ewidencji"));
		RecognizeReturnService.applyRequest(record, request);
		return toRow(record, 0);
	}

	@Transactional
	public LedgerRowDto createManual(RecognizeRequest request) {
		ReturnRecord record = new ReturnRecord();
		RecognizeReturnService.applyRequest(record, request);
		recordRepository.save(record);
		return toRow(record, 0);
	}

	@Transactional
	public void delete(Long id) {
		ReturnRecord record = recordRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono wiersza ewidencji"));
		AllegroReturnEntity allegroReturn = record.getAllegroReturn();
		if (allegroReturn != null) {
			allegroReturn.setLocalState(LocalReturnState.NEW);
		}
		recordRepository.delete(record);
	}

	@Transactional
	public LedgerPageDto fillVat(YearMonth period) {
		LocalDate first = period.atDay(1);
		BigDecimal rate = settingsService.vatRatePercent();
		for (ReturnRecord record : recordRepository.findByPeriodOrderByIdAsc(first)) {
			if (record.getVatAmount() == null && record.getGrossAmount() != null) {
				record.setVatAmount(VatCalculator.fromGross(record.getGrossAmount(), rate));
			}
		}
		return page(period);
	}

	public LedgerRowDto toRow(ReturnRecord record, int lp) {
		return new LedgerRowDto(
				record.getId(),
				lp,
				record.getApiloOrderNumber(),
				record.getSaleDate(),
				record.getBuyerName(),
				record.getProductCodes(),
				record.getReturnDate(),
				record.getGrossAmount(),
				record.getCurrency(),
				record.isAmountEstimated(),
				record.getVatAmount(),
				record.getNotes(),
				record.getAllegroReturn() == null ? null : record.getAllegroReturn().getId(),
				LedgerPeriod.isoMonth(record.getReturnDate()));
	}

	public static String monthTitle(YearMonth period) {
		return LedgerPeriod.title(period);
	}
}
