package com.cafe.management.dto;

import java.math.BigDecimal;

/** Read-only view of a product. */
public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        boolean availability
) {
}
