package com.cafe.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Payload to create a new order (cart contents + optional customer details). */
public record CreateOrderRequest(
        @Size(max = 120, message = "Customer name must be at most 120 characters")
        String customerName,

        @Size(max = 20, message = "Customer contact must be at most 20 characters")
        String customerContact,

        @NotEmpty(message = "An order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {

    /** A single line of the cart. */
    public record OrderItemRequest(
            @jakarta.validation.constraints.NotNull(message = "Product id is required")
            Long productId,

            @jakarta.validation.constraints.NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be greater than zero")
            Integer quantity
    ) {
    }
}
