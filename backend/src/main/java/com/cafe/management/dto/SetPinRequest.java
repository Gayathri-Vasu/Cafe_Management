package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Self-service PIN change from the profile screen. The current password is
 * required so that a stolen, already-unlocked session cannot silently add a
 * backdoor PIN.
 */
public record SetPinRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits")
        String pin
) {
}
