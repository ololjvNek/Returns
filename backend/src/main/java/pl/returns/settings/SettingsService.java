package pl.returns.settings;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.returns.common.ApiException;

@Service
public class SettingsService {

	public static final String SYNC_INTERVAL_MINUTES = "SYNC_INTERVAL_MINUTES";
	public static final String VAT_RATE_PERCENT = "VAT_RATE_PERCENT";
	public static final String APILO_LIMIT_PER_MINUTE = "APILO_LIMIT_PER_MINUTE";
	public static final String APILO_LIMIT_MARGIN_PERCENT = "APILO_LIMIT_MARGIN_PERCENT";
	public static final String APILO_PREFILL_BACKGROUND = "APILO_PREFILL_BACKGROUND";
	public static final String TIMEZONE = "TIMEZONE";
	public static final String USER_AGENT = "USER_AGENT";

	private final AppSettingRepository repository;

	public SettingsService(AppSettingRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<SettingDto> list() {
		return repository.findAll().stream().map(SettingDto::from).toList();
	}

	@Transactional
	public SettingDto update(String key, String value) {
		AppSetting setting = repository.findById(key)
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono ustawienia: " + key));
		validate(setting, value);
		setting.setValue(value.trim());
		return SettingDto.from(setting);
	}

	public int syncIntervalMinutes() {
		return intValue(SYNC_INTERVAL_MINUTES, 15);
	}

	public Duration syncInterval() {
		return Duration.ofMinutes(Math.max(1, syncIntervalMinutes()));
	}

	public BigDecimal vatRatePercent() {
		return decimalValue(VAT_RATE_PERCENT, new BigDecimal("23"));
	}

	public int apiloLimitPerMinute() {
		return intValue(APILO_LIMIT_PER_MINUTE, 200);
	}

	public int apiloMarginPercent() {
		return intValue(APILO_LIMIT_MARGIN_PERCENT, 80);
	}

	public int effectiveApiloLimitPerMinute() {
		int limit = Math.max(1, apiloLimitPerMinute());
		int margin = Math.min(100, Math.max(10, apiloMarginPercent()));
		return Math.max(1, limit * margin / 100);
	}

	public boolean apiloPrefillBackground() {
		return booleanValue(APILO_PREFILL_BACKGROUND, true);
	}

	public ZoneId zoneId() {
		try {
			return ZoneId.of(stringValue(TIMEZONE, "Europe/Warsaw"));
		} catch (Exception ex) {
			return ZoneId.of("Europe/Warsaw");
		}
	}

	public String userAgent() {
		return stringValue(USER_AGENT, "Returns-Ewidencja/1.0.0");
	}

	private void validate(AppSetting setting, String raw) {
		if (raw == null || raw.isBlank()) {
			throw ApiException.badRequest("Wartość nie może być pusta");
		}
		String value = raw.trim();
		switch (setting.getType()) {
			case INTEGER -> {
				int parsed = parseInt(value);
				if (SYNC_INTERVAL_MINUTES.equals(setting.getKey()) && parsed < 1) {
					throw ApiException.badRequest("Interwał musi być co najmniej 1 minuta");
				}
				if (APILO_LIMIT_PER_MINUTE.equals(setting.getKey()) && parsed < 1) {
					throw ApiException.badRequest("Limit Apilo musi być dodatni");
				}
				if (APILO_LIMIT_MARGIN_PERCENT.equals(setting.getKey()) && (parsed < 10 || parsed > 100)) {
					throw ApiException.badRequest("Margines musi być z zakresu 10–100");
				}
			}
			case DECIMAL -> {
				BigDecimal parsed = parseDecimal(value);
				if (VAT_RATE_PERCENT.equals(setting.getKey())
						&& (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(new BigDecimal("100")) > 0)) {
					throw ApiException.badRequest("Stawka VAT musi być z zakresu 0–100");
				}
			}
			case BOOLEAN -> {
				if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
					throw ApiException.badRequest("Wartość logiczna musi być true lub false");
				}
			}
			case STRING -> {
				if (TIMEZONE.equals(setting.getKey())) {
					try {
						ZoneId.of(value);
					} catch (Exception ex) {
						throw ApiException.badRequest("Nieznana strefa czasowa");
					}
				}
			}
		}
	}

	private int intValue(String key, int fallback) {
		return repository.findById(key).map(s -> parseInt(s.getValue())).orElse(fallback);
	}

	private boolean booleanValue(String key, boolean fallback) {
		return repository.findById(key).map(s -> Boolean.parseBoolean(s.getValue())).orElse(fallback);
	}

	private String stringValue(String key, String fallback) {
		return repository.findById(key).map(AppSetting::getValue).orElse(fallback);
	}

	private BigDecimal decimalValue(String key, BigDecimal fallback) {
		return repository.findById(key).map(s -> parseDecimal(s.getValue())).orElse(fallback);
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			throw ApiException.badRequest("Oczekiwano liczby całkowitej");
		}
	}

	private static BigDecimal parseDecimal(String value) {
		try {
			return new BigDecimal(value.trim().replace(',', '.'));
		} catch (NumberFormatException ex) {
			throw ApiException.badRequest("Oczekiwano liczby");
		}
	}
}
