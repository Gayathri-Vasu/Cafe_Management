package com.cafe.management.dto;

import com.cafe.management.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only view of an order together with its items. */
public record OrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        String customerContact,
        Long createdById,
        String createdByName,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        Long billId,
        String billNumber
) {

    /** A single order line as returned to the client. */
    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}
