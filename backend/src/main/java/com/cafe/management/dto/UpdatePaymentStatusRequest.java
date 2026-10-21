package com.cafe.management.dto;

import com.cafe.management.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/** Payload to update the payment status of a bill (e.g. mark as PAID). */
public record UpdatePaymentStatusRequest(
        @NotNull(message = "Payment status is required")
        PaymentStatus paymentStatus
) {
}
