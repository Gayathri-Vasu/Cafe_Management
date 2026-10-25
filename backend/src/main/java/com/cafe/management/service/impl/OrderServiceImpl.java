package com.cafe.management.service.impl;

import com.cafe.management.dto.CreateOrderRequest;
import com.cafe.management.dto.OrderResponse;
import com.cafe.management.entity.Bill;
import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderItem;
import com.cafe.management.entity.OrderStatus;
import com.cafe.management.entity.PaymentStatus;
import com.cafe.management.entity.Product;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.repository.OrderRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.security.CurrentUser;
import com.cafe.management.service.OrderService;
import com.cafe.management.util.DtoMapper;
import com.cafe.management.util.NumberGenerator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final CurrentUser currentUser;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            BillRepository billRepository,
                            CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order();
        order.setOrderNumber(nextOrderNumber());
        order.setCustomerName(blankToNull(request.customerName()));
        order.setCustomerContact(blankToNull(request.customerContact()));
        order.setCreatedBy(currentUser.get());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", line.productId()));

            if (!product.isAvailability()) {
                throw new BadRequestException("'" + product.getName() + "' is currently unavailable and cannot be ordered.");
            }

            int quantity = line.quantity();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .productNameSnapshot(product.getName())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);
            grandTotal = grandTotal.add(subtotal);
        }

        order.setTotalAmount(grandTotal);

        Order saved = orderRepository.save(order);
        return DtoMapper.toOrderResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll(String search, OrderStatus status) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        List<Order> orders = orderRepository.search(normalized, status);
        Map<Long, Bill> billsByOrderId = billsByOrderId(orders);
        return orders.stream()
                .map(order -> DtoMapper.toOrderResponse(order, billsByOrderId.get(order.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = findDetailedOrder(id);
        return DtoMapper.toOrderResponse(order, billRepository.findByOrderId(id).orElse(null));
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findDetailedOrder(id);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order " + order.getOrderNumber() + " is already cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);

        Optional<Bill> bill = billRepository.findByOrderId(id);
        bill.ifPresent(existing -> {
            existing.setPaymentStatus(PaymentStatus.CANCELLED);
            billRepository.save(existing);
        });

        Order saved = orderRepository.save(order);
        return DtoMapper.toOrderResponse(saved, bill.orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getRecent(int limit) {
        // Page of ids first, then one detail query - a fetch join combined with a
        // Pageable would make Hibernate apply the limit in memory.
        List<Long> ids = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(Order::getId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Order> orders = orderRepository.findDetailedByIdIn(ids);
        Map<Long, Bill> billsByOrderId = billsByOrderId(orders);
        return orders.stream()
                .map(order -> DtoMapper.toOrderResponse(order, billsByOrderId.get(order.getId())))
                .toList();
    }

    /**
     * Loads the bills for a batch of orders in a single statement. Reading the order id
     * from the lazy association returns the identifier already held by the proxy, so no
     * query is issued per bill.
     */
    private Map<Long, Bill> billsByOrderId(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        return billRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(bill -> bill.getOrder().getId(), Function.identity()));
    }

    private Order findDetailedOrder(Long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", id));
    }

    private String nextOrderNumber() {
        String candidate = NumberGenerator.orderNumber();
        int attempts = 0;
        while (orderRepository.existsByOrderNumber(candidate) && attempts < 5) {
            candidate = NumberGenerator.orderNumber();
            attempts++;
        }
        return candidate;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
