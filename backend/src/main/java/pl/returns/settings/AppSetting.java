package pl.returns.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_setting")
public class AppSetting {

	@Id
	@Column(name = "key")
	private String key;

	@Column(nullable = false)
	private String value;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SettingType type;

	@Column(nullable = false)
	private String label;

	private String description;

	protected AppSetting() {
	}

	public AppSetting(String key, String value, SettingType type, String label, String description) {
		this.key = key;
		this.value = value;
		this.type = type;
		this.label = label;
		this.description = description;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public SettingType getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}
}
