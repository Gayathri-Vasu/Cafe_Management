package com.cafe.management.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Creates human-readable, unique document numbers such as
 * {@code ORD-20261020-153012-482} and {@code BILL-20261020-153045-119}.
 */
public final class NumberGenerator {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private NumberGenerator() {
    }

    public static String orderNumber() {
        return "ORD-" + stamp() + "-" + randomSuffix();
    }

    public static String billNumber() {
        return "BILL-" + stamp() + "-" + randomSuffix();
    }

    private static String stamp() {
        return LocalDateTime.now().format(STAMP);
    }

    private static String randomSuffix() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }
}
