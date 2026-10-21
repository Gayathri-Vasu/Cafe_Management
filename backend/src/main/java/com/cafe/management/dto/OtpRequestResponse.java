package com.cafe.management.dto;

/**
 * Confirms that a code was sent. The code is never echoed back - only how long
 * it stays valid and a masked version of the number that received it.
 */
public record OtpRequestResponse(
        int expiresInSeconds,
        String maskedPhone
) {
}
