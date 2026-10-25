package com.cafe.management.repository;

import com.cafe.management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    /**
     * The fetch join loads each product's category in the same statement. Product
     * responses expose the category id and name, which previously triggered one
     * lazy load per row.
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.name ASC
            """)
    List<Product> search(@Param("categoryId") Long categoryId, @Param("search") String search);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    /**
     * Product counts for every category in a single statement, returned as
     * {@code [categoryId, count]} rows. Replaces the count query that was issued
     * once per category when building the category list.
     */
    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.category IS NOT NULL GROUP BY p.category.id")
    List<Object[]> countGroupedByCategory();
}
