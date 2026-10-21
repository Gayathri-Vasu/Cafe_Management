package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;

/** Exchanges a received one-time code for a session. */
public record OtpVerifyRequest(
        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Code is required")
        String code
) {
}
