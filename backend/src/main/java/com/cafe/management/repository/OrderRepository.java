package com.cafe.management.repository;

import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Query("""
            SELECT o FROM Order o
            WHERE (:search IS NULL
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerContact) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.createdAt DESC
            """)
    List<Order> search(@Param("search") String search, @Param("status") OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.cafe.management.entity.OrderStatus.CANCELLED")
    java.math.BigDecimal sumTotalAmountExcludingCancelled();

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :from AND o.status <> com.cafe.management.entity.OrderStatus.CANCELLED ORDER BY o.createdAt DESC")
    List<Order> findRecent(@Param("from") LocalDateTime from, Pageable pageable);
}
