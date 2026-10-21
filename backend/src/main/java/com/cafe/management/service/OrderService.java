package com.cafe.management.service;

import com.cafe.management.dto.CreateOrderRequest;
import com.cafe.management.dto.OrderResponse;
import com.cafe.management.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    /** Validates the cart, calculates the total from DB prices and saves the order. */
    OrderResponse create(CreateOrderRequest request);

    List<OrderResponse> getAll(String search, OrderStatus status);

    OrderResponse getById(Long id);

    /** Cancels an order (and its bill, if any). */
    OrderResponse cancel(Long id);

    List<OrderResponse> getRecent(int limit);
}
