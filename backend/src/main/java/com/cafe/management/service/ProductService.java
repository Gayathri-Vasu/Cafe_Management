package com.cafe.management.service;

import com.cafe.management.dto.ProductDto;
import com.cafe.management.dto.ProductRequest;

import java.util.List;

public interface ProductService {

    /** Returns products filtered by optional category and free-text search. */
    List<ProductDto> search(Long categoryId, String search);

    ProductDto getById(Long id);

    ProductDto create(ProductRequest request);

    ProductDto update(Long id, ProductRequest request);

    ProductDto updateAvailability(Long id, boolean availability);

    void delete(Long id);
}
