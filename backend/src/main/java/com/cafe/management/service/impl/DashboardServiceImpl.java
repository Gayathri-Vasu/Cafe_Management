package com.cafe.management.service.impl;

import com.cafe.management.dto.DashboardStats;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.service.DashboardService;
import com.cafe.management.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final BillRepository billRepository;
    private final OrderService orderService;

    public DashboardServiceImpl(BillRepository billRepository, OrderService orderService) {
        this.billRepository = billRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Object[]> rows = billRepository.loadDashboardTotals(startOfToday);
        Object[] totals = rows.isEmpty() ? new Object[8] : rows.getFirst();

        return new DashboardStats(
                toLong(totals, 0),
                toLong(totals, 1),
                toLong(totals, 2),
                toLong(totals, 3),
                toMoney(totals, 4),
                toMoney(totals, 5),
                toLong(totals, 6),
                toLong(totals, 7),
                orderService.getRecent(5));
    }

    private static long toLong(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private static BigDecimal toMoney(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return BigDecimal.ZERO;
        }
        if (row[index] instanceof BigDecimal money) {
            return money;
        }
        return new BigDecimal(row[index].toString());
    }
}
