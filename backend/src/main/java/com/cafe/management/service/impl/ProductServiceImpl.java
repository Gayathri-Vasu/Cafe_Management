package com.cafe.management.service.impl;

import com.cafe.management.dto.ProductDto;
import com.cafe.management.dto.ProductRequest;
import com.cafe.management.entity.Category;
import com.cafe.management.entity.Product;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.CategoryRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.service.ProductService;
import com.cafe.management.util.DtoMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> search(Long categoryId, String search) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return productRepository.search(categoryId, normalized).stream()
                .map(DtoMapper::toProductDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        return DtoMapper.toProductDto(findProduct(id));
    }

    @Override
    @Transactional
    public ProductDto create(ProductRequest request) {
        Category category = findCategory(request.categoryId());

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .category(category)
                .availability(request.availability() == null || request.availability())
                .build();

        return DtoMapper.toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        Category category = findCategory(request.categoryId());

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        if (request.availability() != null) {
            product.setAvailability(request.availability());
        }

        return DtoMapper.toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDto updateAvailability(Long id, boolean availability) {
        Product product = findProduct(id);
        product.setAvailability(availability);
        return DtoMapper.toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException(
                    "Cannot delete product '" + product.getName() + "' because it is used in existing orders. "
                            + "Mark it unavailable instead.");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
