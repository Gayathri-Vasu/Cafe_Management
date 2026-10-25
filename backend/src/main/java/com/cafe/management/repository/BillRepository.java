package com.cafe.management.repository;

import com.cafe.management.entity.Bill;
import com.cafe.management.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    boolean existsByBillNumber(String billNumber);

    List<Bill> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Loads the bills and, in the same statement, their order, the order's line items
     * and the creating user. Mapping a bill response reads all four, so without the
     * fetch joins each row cost three additional round trips.
     */
    @Query("""
            SELECT DISTINCT b FROM Bill b
            JOIN FETCH b.order o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.createdBy
            WHERE (:search IS NULL
                   OR LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR b.paymentStatus = :status)
            ORDER BY b.createdAt DESC
            """)
    List<Bill> search(@Param("search") String search, @Param("status") PaymentStatus status);

    /** Single statement that replaces the per-order {@link #findByOrderId(Long)} call in list endpoints. */
    List<Bill> findByOrderIdIn(Collection<Long> orderIds);

    @Query("""
            SELECT DISTINCT b FROM Bill b
            JOIN FETCH b.order o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.createdBy
            WHERE b.id = :id
            """)
    Optional<Bill> findDetailedById(@Param("id") Long id);

    /** Revenue = sum of PAID bills only. */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.paymentStatus = com.cafe.management.entity.PaymentStatus.PAID")
    BigDecimal calculatePaidRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.paymentStatus = com.cafe.management.entity.PaymentStatus.PAID AND b.createdAt >= :from")
    BigDecimal calculatePaidRevenueSince(@Param("from") java.time.LocalDateTime from);
}
