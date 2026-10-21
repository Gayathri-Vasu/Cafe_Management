package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Self-service mobile number change, used to switch on OTP sign-in. The current
 * password is required for the same reason as {@link SetPinRequest}.
 */
public record SetPhoneRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\+?[0-9]{7,15}", message = "Enter a valid phone number, e.g. +919876543210")
        String phone
) {
}
