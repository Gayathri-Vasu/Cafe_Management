package com.cafe.management.service.impl;

import com.cafe.management.dto.CreateOrderRequest;
import com.cafe.management.dto.OrderResponse;
import com.cafe.management.entity.Category;
import com.cafe.management.entity.Order;
import com.cafe.management.entity.Product;
import com.cafe.management.entity.Role;
import com.cafe.management.entity.User;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.repository.OrderRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the order total calculation logic - the most critical business rule
 * in the application.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BillRepository billRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Product cappuccino;
    private Product sandwich;

    @BeforeEach
    void setUp() {
        Category coffee = Category.builder().id(1L).name("Coffee").build();

        cappuccino = Product.builder()
                .id(10L)
                .name("Cappuccino")
                .price(new BigDecimal("120.00"))
                .category(coffee)
                .availability(true)
                .build();

        sandwich = Product.builder()
                .id(20L)
                .name("Veg Sandwich")
                .price(new BigDecimal("100.00"))
                .category(coffee)
                .availability(true)
                .build();

        User staff = User.builder().id(5L).name("Staff One").email("staff@cafe.com").role(Role.STAFF).build();
        when(currentUser.get()).thenReturn(staff);
    }

    @Test
    @DisplayName("Grand total = sum(unitPrice * quantity) using database prices")
    void calculatesGrandTotalFromDatabasePrices() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(cappuccino));
        when(productRepository.findById(20L)).thenReturn(Optional.of(sandwich));
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest("Rahul", "9876543210", List.of(
                new CreateOrderRequest.OrderItemRequest(10L, 2),
                new CreateOrderRequest.OrderItemRequest(20L, 1)));

        OrderResponse response = orderService.create(request);

        // 120 * 2 = 240 ; 100 * 1 = 100 ; total 340
        assertThat(response.totalAmount()).isEqualByComparingTo("340.00");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("240.00");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo("100.00");
        assertThat(response.createdByName()).isEqualTo("Staff One");
    }

    @Test
    @DisplayName("A price snapshot is stored on every order line")
    void storesPriceSnapshotOnEachItem() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(cappuccino));
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(null, null,
                List.of(new CreateOrderRequest.OrderItemRequest(10L, 3)));

        OrderResponse response = orderService.create(request);

        assertThat(response.items().get(0).productName()).isEqualTo("Cappuccino");
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("120.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("360.00");
    }

    @Test
    @DisplayName("Ordering an unavailable product is rejected")
    void rejectsUnavailableProduct() {
        cappuccino.setAvailability(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(cappuccino));

        CreateOrderRequest request = new CreateOrderRequest("Rahul", null,
                List.of(new CreateOrderRequest.OrderItemRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unavailable");
    }
}
