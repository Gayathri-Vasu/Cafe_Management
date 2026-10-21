package com.cafe.management.repository;

import com.cafe.management.entity.Bill;
import com.cafe.management.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    boolean existsByBillNumber(String billNumber);

    List<Bill> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("""
            SELECT b FROM Bill b
            LEFT JOIN b.order o
            WHERE (:search IS NULL
                   OR LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR b.paymentStatus = :status)
            ORDER BY b.createdAt DESC
            """)
    List<Bill> search(@Param("search") String search, @Param("status") PaymentStatus status);

    /** Revenue = sum of PAID bills only. */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.paymentStatus = com.cafe.management.entity.PaymentStatus.PAID")
    BigDecimal calculatePaidRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.paymentStatus = com.cafe.management.entity.PaymentStatus.PAID AND b.createdAt >= :from")
    BigDecimal calculatePaidRevenueSince(@Param("from") java.time.LocalDateTime from);
}
