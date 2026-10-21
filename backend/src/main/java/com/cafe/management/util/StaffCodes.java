package com.cafe.management.util;

/**
 * Badge numbers used by the counter PIN sign-in.
 *
 * <p>A staff code is derived from the account id (row id + 1000) so it is always
 * unique, never needs a collision check, and stays stable for the lifetime of
 * the account. The offset keeps codes four digits long for a long time and stops
 * a badge from doubling as the database primary key.</p>
 */
public final class StaffCodes {

    private static final int OFFSET = 1000;

    private StaffCodes() {
    }

    public static String forUserId(Long userId) {
        return String.valueOf(OFFSET + userId);
    }
}
