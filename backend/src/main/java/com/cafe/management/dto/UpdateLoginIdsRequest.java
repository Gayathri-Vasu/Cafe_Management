package com.cafe.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The two identifiers an account can be signed in with: the email address and
 * the staff code printed on the counter badge.
 *
 * <p>Both are administrator-only because they decide how an account is found at
 * sign-in, so an ordinary staff member can never rename their own.</p>
 */
public record UpdateLoginIdsRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Staff code is required")
        @Pattern(regexp = "[0-9]{4,12}", message = "Staff code must be 4 to 12 digits")
        String staffCode
) {
}
