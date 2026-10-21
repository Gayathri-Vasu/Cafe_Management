package com.cafe.management.service;

import com.cafe.management.dto.CategoryDto;
import com.cafe.management.dto.CategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAll(String search);

    CategoryDto getById(Long id);

    CategoryDto create(CategoryRequest request);

    CategoryDto update(Long id, CategoryRequest request);

    void delete(Long id);
}
