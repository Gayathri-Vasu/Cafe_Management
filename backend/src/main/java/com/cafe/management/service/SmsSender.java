package com.cafe.management.service;

/**
 * Delivers a short text message to a mobile number.
 *
 * <p>Two implementations ship with the application and are selected with the
 * {@code app.otp.sms.provider} property:</p>
 * <ul>
 *   <li>{@code log} (default) - the code is written to the application log. Useful
 *       for local development where no gateway account exists yet.</li>
 *   <li>{@code http} - the code is POSTed as JSON to a gateway webhook URL, which
 *       works with Twilio, MSG91, Fast2SMS or any custom relay.</li>
 * </ul>
 */
public interface SmsSender {

    /** Sends the message, or throws when the gateway rejects it. */
    void send(String phoneNumber, String message);

    /** Short label used in the log line and in {@code GET /api/auth/providers}. */
    String providerName();
}
