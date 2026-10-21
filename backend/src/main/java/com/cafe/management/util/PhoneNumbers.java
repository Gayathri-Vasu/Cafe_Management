package com.cafe.management.util;

/**
 * Single definition of "the same phone number" for the whole application.
 *
 * <p>Numbers are stored and looked up in a normalised form (digits only, with a
 * leading {@code +} when the caller supplied one) so that
 * {@code +91 98765 43210} and {@code 919876543210} map to one account.</p>
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replaceAll("[\\s()\\-.]", "");
        return trimmed.startsWith("+")
                ? "+" + trimmed.substring(1).replaceAll("\\D", "")
                : trimmed.replaceAll("\\D", "");
    }

    /** {@code +919876543210} becomes {@code +********3210} for display. */
    public static String mask(String normalized) {
        if (normalized == null || normalized.length() <= 4) {
            return "****";
        }
        String prefix = normalized.startsWith("+") ? "+" : "";
        String tail = normalized.substring(normalized.length() - 4);
        return prefix + "*".repeat(normalized.length() - 4 - prefix.length()) + tail;
    }
}
