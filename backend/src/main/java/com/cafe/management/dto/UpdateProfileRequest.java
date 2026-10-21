package com.cafe.management.dto;

import com.cafe.management.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The personal details a signed-in user may edit about themselves.
 *
 * <p>Everything except the first name is optional and may be cleared by sending
 * null or a blank value. The age shown on the profile is derived from
 * {@link #dateOfBirth} rather than stored, so it can never drift.</p>
 */
public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String name,

        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        Gender gender,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 150, message = "Place must be at most 150 characters")
        String place,

        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @Pattern(regexp = "\\+?[0-9]{7,15}|", message = "Enter a valid phone number, e.g. +919876543210")
        String phone
) {
}
