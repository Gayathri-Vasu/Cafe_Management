package com.cafe.management.service;

import com.cafe.management.dto.DashboardStats;

public interface DashboardService {

    /** Aggregates categories, products, orders, bills and revenue for the dashboard. */
    DashboardStats getStats();
}
