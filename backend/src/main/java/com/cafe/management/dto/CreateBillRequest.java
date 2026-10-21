package com.cafe.management.dto;

import com.cafe.management.entity.PaymentMethod;
import com.cafe.management.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/** Payload to generate a bill for an existing order. */
public record CreateBillRequest(
        @NotNull(message = "Order id is required")
        Long orderId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        PaymentStatus paymentStatus
) {
}
