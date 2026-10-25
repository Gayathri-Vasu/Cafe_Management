package com.cafe.management.service.impl;

import com.cafe.management.dto.CategoryDto;
import com.cafe.management.dto.CategoryRequest;
import com.cafe.management.entity.Category;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.DuplicateResourceException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.CategoryRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.service.CategoryService;
import com.cafe.management.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(String search) {
        List<Category> categories = (search == null || search.isBlank())
                ? categoryRepository.findAll(org.springframework.data.domain.Sort.by("name").ascending())
                : categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());

        if (categories.isEmpty()) {
            return List.of();
        }

        // One grouped count for every category, instead of one count query per category.
        Map<Long, Long> productCounts = productRepository.countGroupedByCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return categories.stream()
                .map(category -> DtoMapper.toCategoryDto(category,
                        productCounts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        Category category = findCategory(id);
        return DtoMapper.toCategoryDto(category, productRepository.countByCategoryId(id));
    }

    @Override
    @Transactional
    public CategoryDto create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("A category named '" + request.name().trim() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.name().trim())
                .description(request.description())
                .build();
        Category saved = categoryRepository.save(category);
        return DtoMapper.toCategoryDto(saved, 0);
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String newName = request.name().trim();

        categoryRepository.findByNameIgnoreCase(newName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A category named '" + newName + "' already exists");
                });

        category.setName(newName);
        category.setDescription(request.description());
        Category saved = categoryRepository.save(category);
        return DtoMapper.toCategoryDto(saved, productRepository.countByCategoryId(saved.getId()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException(
                    "Cannot delete category '" + category.getName() + "' because it still has products. "
                            + "Move or delete those products first.");
        }
        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
