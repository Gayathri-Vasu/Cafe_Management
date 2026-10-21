package com.cafe.management.dto;

/**
 * Tells the login screen which sign-in methods are actually available, so a
 * button is only offered when the server can complete the flow.
 *
 * <p>The client ids are public by design - they are already visible in the
 * browser when the provider's JavaScript SDK is loaded.</p>
 *
 * @param smsProvider the active OTP channel: {@code log} means the code is
 *                    written to the backend log rather than texted, which the
 *                    login screen reports instead of promising an SMS
 */
public record AuthProvidersResponse(
        boolean passwordEnabled,
        boolean pinEnabled,
        boolean phoneOtpEnabled,
        boolean googleEnabled,
        boolean appleEnabled,
        String googleClientId,
        String appleClientId,
        int otpLength,
        String smsProvider
) {
}
