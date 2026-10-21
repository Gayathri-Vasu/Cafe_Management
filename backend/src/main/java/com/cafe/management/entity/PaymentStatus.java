package com.cafe.management.entity;

/**
 * Payment status of a bill.
 *
 * <p>Revenue is only counted for bills that are {@link #PAID}; {@link #PENDING}
 * and {@link #CANCELLED} bills are excluded from all revenue calculations.</p>
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    CANCELLED
}
