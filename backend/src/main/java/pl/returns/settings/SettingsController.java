package pl.returns.settings;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping
	public List<SettingDto> list() {
		return settingsService.list();
	}

	@PutMapping("/{key}")
	public SettingDto update(@PathVariable String key, @Valid @RequestBody SettingUpdateRequest request) {
		return settingsService.update(key, request.value());
	}
}
