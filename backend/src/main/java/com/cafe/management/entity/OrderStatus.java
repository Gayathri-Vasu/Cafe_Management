package com.cafe.management.entity;

/**
 * Lifecycle of an order.
 *
 * <ul>
 *   <li>CREATED  - order was placed by staff/admin, not billed yet.</li>
 *   <li>BILLED   - a bill has been generated for this order.</li>
 *   <li>CANCELLED- order (and its bill) was cancelled.</li>
 * </ul>
 */
public enum OrderStatus {
    CREATED,
    BILLED,
    CANCELLED
}
