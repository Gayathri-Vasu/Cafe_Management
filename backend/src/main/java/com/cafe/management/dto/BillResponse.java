package com.cafe.management.dto;

import com.cafe.management.entity.PaymentMethod;
import com.cafe.management.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only view of a bill (used for the billing page and the PDF invoice). */
public record BillResponse(
        Long id,
        String billNumber,
        Long orderId,
        String orderNumber,
        String customerName,
        String customerContact,
        String createdByName,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        List<OrderResponse.OrderItemResponse> items
) {
}
