package pl.returns.settings;

import jakarta.validation.constraints.NotBlank;

public record SettingUpdateRequest(@NotBlank String value) {
}
