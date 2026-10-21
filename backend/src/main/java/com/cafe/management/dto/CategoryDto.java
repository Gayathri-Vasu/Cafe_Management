package com.cafe.management.dto;

/** Read-only view of a category. */
public record CategoryDto(
        Long id,
        String name,
        String description,
        long productCount
) {
}
