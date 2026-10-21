package com.cafe.management.service.impl;

import com.cafe.management.dto.CreateBillRequest;
import com.cafe.management.entity.Bill;
import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderStatus;
import com.cafe.management.entity.PaymentMethod;
import com.cafe.management.entity.PaymentStatus;
import com.cafe.management.exception.DuplicateResourceException;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.repository.OrderRepository;
import com.cafe.management.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Verifies the "no duplicate bills per order" rule. */
@ExtendWith(MockitoExtension.class)
class BillServiceImplTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PdfService pdfService;

    @InjectMocks
    private BillServiceImpl billService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .orderNumber("ORD-20261020-100000-001")
                .totalAmount(new BigDecimal("340.00"))
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("A second bill for the same order is rejected")
    void rejectsDuplicateBill() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(billRepository.existsByOrderId(1L)).thenReturn(true);

        CreateBillRequest request = new CreateBillRequest(1L, PaymentMethod.CASH, PaymentStatus.PAID);

        assertThatThrownBy(() -> billService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("A bill cannot be generated for a cancelled order")
    void rejectsCancelledOrder() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(billRepository.existsByOrderId(1L)).thenReturn(false);

        CreateBillRequest request = new CreateBillRequest(1L, PaymentMethod.UPI, PaymentStatus.PENDING);

        assertThatThrownBy(() -> billService.create(request))
                .hasMessageContaining("cancelled");
    }
}
