package com.cafe.management.repository;

import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    /**
     * Loads orders together with their line items and the creating user.
     * The fetch joins are what stop the list endpoint from issuing one extra
     * statement per row (items + createdBy); on a remote database that is one
     * network round trip per row, which is what made long lists slow.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.createdBy
            WHERE (:search IS NULL
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerContact) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.createdAt DESC
            """)
    List<Order> search(@Param("search") String search, @Param("status") OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Same fetch-join detail load, but restricted to a known set of ids. Used as the
     * second step of pagination: fetch the page of ids first (a fetch join cannot be
     * combined with a Pageable without Hibernate pulling the whole table into memory),
     * then load the details for just those ids.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.createdBy
            WHERE o.id IN :ids
            ORDER BY o.createdAt DESC
            """)
    List<Order> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.createdBy
            WHERE o.id = :id
            """)
    Optional<Order> findDetailedById(@Param("id") Long id);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.cafe.management.entity.OrderStatus.CANCELLED")
    java.math.BigDecimal sumTotalAmountExcludingCancelled();

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :from AND o.status <> com.cafe.management.entity.OrderStatus.CANCELLED ORDER BY o.createdAt DESC")
    List<Order> findRecent(@Param("from") LocalDateTime from, Pageable pageable);
}
