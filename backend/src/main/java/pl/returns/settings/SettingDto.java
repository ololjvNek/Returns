package pl.returns.settings;

public record SettingDto(String key, String value, SettingType type, String label, String description) {

	public static SettingDto from(AppSetting setting) {
		return new SettingDto(
				setting.getKey(),
				setting.getValue(),
				setting.getType(),
				setting.getLabel(),
				setting.getDescription());
	}
}
