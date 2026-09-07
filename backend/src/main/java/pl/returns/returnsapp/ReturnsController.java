package pl.returns.returnsapp;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.returns.domain.LocalReturnState;
import pl.returns.ledger.LedgerService;

@RestController
@RequestMapping("/api/returns")
public class ReturnsController {

	private final RecognizeReturnService recognizeReturnService;
	private final LedgerService ledgerService;

	public ReturnsController(RecognizeReturnService recognizeReturnService, LedgerService ledgerService) {
		this.recognizeReturnService = recognizeReturnService;
		this.ledgerService = ledgerService;
	}

	@GetMapping
	public List<ReturnListItem> list(
			@RequestParam(required = false) Long accountId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) LocalReturnState localState) {
		return recognizeReturnService.list(accountId, status, localState);
	}

	@PostMapping("/{id}/prefill")
	public PrefillResponse prefill(@PathVariable UUID id) {
		return recognizeReturnService.buildPrefill(id);
	}

	@PostMapping("/{id}/recognize")
	public LedgerRowDto recognize(@PathVariable UUID id, @Valid @RequestBody RecognizeRequest request) {
		return ledgerService.toRow(recognizeReturnService.recognize(id, request), 0);
	}

	@PostMapping("/{id}/ignore")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void ignore(@PathVariable UUID id) {
		recognizeReturnService.ignore(id);
	}

	@PostMapping("/{id}/refresh-amount")
	public PrefillResponse refreshAmount(@PathVariable UUID id) {
		return recognizeReturnService.refreshAmount(id);
	}
}
