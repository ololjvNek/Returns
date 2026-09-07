package pl.returns.ledger;

import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.returns.returnsapp.LedgerPageDto;
import pl.returns.returnsapp.LedgerRowDto;
import pl.returns.returnsapp.RecognizeRequest;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

	private final LedgerService ledgerService;
	private final XlsxExportService xlsxExportService;

	public LedgerController(LedgerService ledgerService, XlsxExportService xlsxExportService) {
		this.ledgerService = ledgerService;
		this.xlsxExportService = xlsxExportService;
	}

	@GetMapping
	public LedgerPageDto page(@RequestParam String period) {
		return ledgerService.page(YearMonth.parse(period));
	}

	@PostMapping
	public LedgerRowDto create(@Valid @RequestBody RecognizeRequest request) {
		return ledgerService.createManual(request);
	}

	@PutMapping("/{id}")
	public LedgerRowDto update(@PathVariable Long id, @Valid @RequestBody RecognizeRequest request) {
		return ledgerService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		ledgerService.delete(id);
	}

	@PostMapping("/fill-vat")
	public LedgerPageDto fillVat(@RequestParam String period) {
		return ledgerService.fillVat(YearMonth.parse(period));
	}

	@GetMapping("/export.xlsx")
	public ResponseEntity<byte[]> export(@RequestParam String period) {
		var page = ledgerService.page(YearMonth.parse(period));
		byte[] body = xlsxExportService.export(page);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		headers.setContentDisposition(ContentDisposition.attachment()
				.filename("ewidencja-zwrotow-" + period + ".xlsx")
				.build());
		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}
}
