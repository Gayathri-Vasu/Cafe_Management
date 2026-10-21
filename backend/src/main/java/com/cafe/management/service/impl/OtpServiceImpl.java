package com.cafe.management.service.impl;

import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.InvalidCredentialsException;
import com.cafe.management.service.OtpService;
import com.cafe.management.service.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory one-time-password service.
 *
 * <p>Codes are deliberately kept in memory rather than in MySQL: they live for a
 * few minutes, must never be re-usable, and there is no operational reason to
 * persist them. A restart simply invalidates the codes that were in flight.</p>
 *
 * <p>Three safeguards protect the endpoint:</p>
 * <ol>
 *   <li>a resend cooldown, so the same number cannot be spammed;</li>
 *   <li>an attempt counter, so a code cannot be brute forced;</li>
 *   <li>single use - a correct code is deleted the moment it is accepted.</li>
 * </ol>
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsSender smsSender;
    private final boolean enabled;
    private final int length;
    private final int ttlSeconds;
    private final int resendCooldownSeconds;
    private final int maxAttempts;
    private final String cafeName;

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public OtpServiceImpl(SmsSender smsSender,
                          @Value("${app.otp.enabled:true}") boolean enabled,
                          @Value("${app.otp.length:6}") int length,
                          @Value("${app.otp.ttl-seconds:300}") int ttlSeconds,
                          @Value("${app.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds,
                          @Value("${app.otp.max-attempts:5}") int maxAttempts,
                          @Value("${app.cafe.name:Kettle & Kitchen}") String cafeName) {
        this.smsSender = smsSender;
        this.enabled = enabled;
        this.length = length;
        this.ttlSeconds = ttlSeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
        this.cafeName = cafeName;

        if (enabled && "log".equals(smsSender.providerName())) {
            log.warn("Phone sign-in is using the 'log' SMS provider - codes are written to this log file. "
                    + "Configure a real gateway before going live.");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int ttlSeconds() {
        return ttlSeconds;
    }

    @Override
    public String smsProvider() {
        return smsSender.providerName();
    }

    @Override
    public int request(String phoneNumber) {
        requireEnabled();

        String phone = normalize(phoneNumber);
        purgeExpired();

        Instant now = Instant.now();
        Challenge existing = challenges.get(phone);
        if (existing != null && existing.lastSentAt.plusSeconds(resendCooldownSeconds).isAfter(now)) {
            long waitFor = existing.lastSentAt.plusSeconds(resendCooldownSeconds).getEpochSecond() - now.getEpochSecond();
            throw new BadRequestException("Please wait " + Math.max(waitFor, 1) + " seconds before requesting another code");
        }

        String code = generateCode();
        challenges.put(phone, new Challenge(code, now.plusSeconds(ttlSeconds), now));

        smsSender.send(phone, "Your " + cafeName + " sign-in code is " + code
                + ". It expires in " + (ttlSeconds / 60) + " minutes.");

        return ttlSeconds;
    }

    @Override
    public void verify(String phoneNumber, String code) {
        requireEnabled();

        String phone = normalize(phoneNumber);
        Challenge challenge = challenges.get(phone);

        if (challenge == null || challenge.isExpired()) {
            challenges.remove(phone);
            throw new InvalidCredentialsException("That code has expired. Please request a new one.");
        }
        if (challenge.attempts >= maxAttempts) {
            challenges.remove(phone);
            throw new InvalidCredentialsException("Too many incorrect codes. Please request a new one.");
        }
        if (!challenge.code.equals(code == null ? "" : code.trim())) {
            challenge.attempts++;
            throw new InvalidCredentialsException("That code is not correct.");
        }

        challenges.remove(phone);
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new BadRequestException("Phone sign-in is not enabled");
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private void purgeExpired() {
        challenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /** Keeps digits and a single leading plus so the same number always maps to one entry. */
    private String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        String trimmed = phoneNumber.trim().replaceAll("[\\s()-]", "");
        return trimmed.startsWith("+")
                ? "+" + trimmed.substring(1).replaceAll("\\D", "")
                : trimmed.replaceAll("\\D", "");
    }

    private static final class Challenge {
        private final String code;
        private final Instant expiresAt;
        private final Instant lastSentAt;
        private int attempts;

        private Challenge(String code, Instant expiresAt, Instant lastSentAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.lastSentAt = lastSentAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
