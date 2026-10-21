package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;

/** Asks the server to text a one-time code to this number. */
public record OtpRequestBody(
        @NotBlank(message = "Phone number is required")
        String phone
) {
}
