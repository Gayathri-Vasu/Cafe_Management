package com.cafe.management.repository;

import com.cafe.management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN p.category c
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.name ASC
            """)
    List<Product> search(@Param("categoryId") Long categoryId, @Param("search") String search);
}
