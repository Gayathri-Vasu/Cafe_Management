package com.cafe.management.security;

import com.cafe.management.exception.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory throttle that slows down guessing attacks against the
 * alternative sign-in methods.
 *
 * <p>A staff PIN is only 4-8 digits, so without this an attacker could walk
 * through every combination. After {@code app.login.max-attempts} failures a key
 * (a staff code or a phone number) is locked for {@code app.login.lockout-minutes}.</p>
 */
@Component
public class AttemptThrottle {

    private final int maxAttempts;
    private final Duration lockout;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public AttemptThrottle(@Value("${app.login.max-attempts:10}") int maxAttempts,
                           @Value("${app.login.lockout-minutes:15}") int lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
    }

    /** Rejects the attempt outright when the key is currently locked out. */
    public void checkAllowed(String key) {
        Counter counter = counters.get(key);
        if (counter == null) {
            return;
        }
        if (counter.lockedUntil != null && Instant.now().isBefore(counter.lockedUntil)) {
            long minutes = Math.max(1, Duration.between(Instant.now(), counter.lockedUntil).toMinutes());
            counters.remove(key);
            throw new InvalidCredentialsException(
                    "Too many failed attempts. Please try again in " + minutes + " minute(s) or sign in with your password.");
        }
    }

    public void recordFailure(String key) {
        counters.compute(key, (ignored, existing) -> {
            Counter counter = existing == null ? new Counter() : existing;
            counter.failures++;
            if (counter.failures >= maxAttempts) {
                counter.lockedUntil = Instant.now().plus(lockout);
            }
            return counter;
        });
    }

    public void reset(String key) {
        counters.remove(key);
    }

    private static final class Counter {
        private int failures;
        private Instant lockedUntil;
    }
}
