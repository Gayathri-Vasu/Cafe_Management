package com.cafe.management.service;

/** Issues and checks the one-time codes used by phone sign-in. */
public interface OtpService {

    /** {@code false} when phone sign-in is switched off by configuration. */
    boolean isEnabled();

    /** How long a freshly issued code stays valid, in seconds. */
    int ttlSeconds();

    /**
     * The active delivery channel, e.g. {@code log} or {@code http}.
     *
     * <p>Exposed to the login screen so it can say where a code actually went
     * instead of always claiming an SMS was sent.</p>
     */
    String smsProvider();

    /**
     * Generates a code for the number, stores it and sends it by SMS.
     *
     * @return the number of seconds the code stays valid for
     * @throws com.cafe.management.exception.BadRequestException when sign-in is
     *         disabled or the per-number resend cooldown has not elapsed yet
     */
    int request(String phoneNumber);

    /**
     * Consumes the code. A correct code is removed immediately so it cannot be
     * replayed.
     *
     * @throws com.cafe.management.exception.InvalidCredentialsException when the
     *         code is wrong, expired, already used, or tried too many times
     */
    void verify(String phoneNumber, String code);
}
