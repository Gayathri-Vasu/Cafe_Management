package com.cafe.management.service.impl;

import com.cafe.management.dto.DashboardStats;
import com.cafe.management.entity.PaymentStatus;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.repository.CategoryRepository;
import com.cafe.management.repository.OrderRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.service.DashboardService;
import com.cafe.management.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final OrderService orderService;

    public DashboardServiceImpl(CategoryRepository categoryRepository,
                                ProductRepository productRepository,
                                OrderRepository orderRepository,
                                BillRepository billRepository,
                                OrderService orderService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        return new DashboardStats(
                categoryRepository.count(),
                productRepository.count(),
                orderRepository.count(),
                billRepository.count(),
                billRepository.calculatePaidRevenue(),
                billRepository.calculatePaidRevenueSince(startOfToday),
                billRepository.countByPaymentStatus(PaymentStatus.PENDING),
                billRepository.countByPaymentStatus(PaymentStatus.PAID),
                orderService.getRecent(5));
    }
}
