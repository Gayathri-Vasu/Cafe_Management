package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Payload for the counter "badge" sign-in: staff code + numeric PIN. */
public record PinLoginRequest(
        @NotBlank(message = "Staff code is required")
        String staffCode,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits")
        String pin
) {
}
