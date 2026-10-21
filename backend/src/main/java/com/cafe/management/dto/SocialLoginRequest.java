package com.cafe.management.dto;

import jakarta.validation.constraints.NotBlank;

/** Carries the ID token that the Google or Apple JavaScript SDK returned to the browser. */
public record SocialLoginRequest(
        @NotBlank(message = "Identity token is required")
        String idToken
) {
}
