package com.cafe.management.controller;

import com.cafe.management.dto.CreateOrderRequest;
import com.cafe.management.dto.OrderResponse;
import com.cafe.management.entity.OrderStatus;
import com.cafe.management.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Order management. Both ADMIN and STAFF can create and view orders.
 * Cancelling an order is restricted to ADMIN.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAll(search, status));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<OrderResponse>> getRecent(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(orderService.getRecent(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
