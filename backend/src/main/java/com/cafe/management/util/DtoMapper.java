package com.cafe.management.util;

import com.cafe.management.dto.BillResponse;
import com.cafe.management.dto.CategoryDto;
import com.cafe.management.dto.OrderResponse;
import com.cafe.management.dto.ProductDto;
import com.cafe.management.dto.UserDto;
import com.cafe.management.entity.Bill;
import com.cafe.management.entity.Category;
import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderItem;
import com.cafe.management.entity.Product;
import com.cafe.management.entity.User;

import java.util.List;

/**
 * Converts JPA entities into DTOs. Keeping this in one place guarantees that a
 * password hash (or any other internal field) can never leak into an API response.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserDto toUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getLastName(),
                user.getEmail(),
                user.getStaffCode(),
                user.getPhone(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getPlace(),
                user.getAddress(),
                user.getPinHash() != null,
                user.getGoogleSubject() != null,
                user.getAppleSubject() != null,
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    public static List<UserDto> toUserDtos(List<User> users) {
        return users.stream().map(DtoMapper::toUserDto).toList();
    }

    public static CategoryDto toCategoryDto(Category category, long productCount) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription(), productCount);
    }

    public static ProductDto toProductDto(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        return new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), categoryId, categoryName, product.isAvailability());
    }

    public static OrderResponse toOrderResponse(Order order, Bill bill) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(DtoMapper::toOrderItemResponse)
                .toList();

        Long createdById = order.getCreatedBy() != null ? order.getCreatedBy().getId() : null;
        String createdByName = order.getCreatedBy() != null ? order.getCreatedBy().getName() : null;

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerContact(),
                createdById,
                createdByName,
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                items,
                bill != null ? bill.getId() : null,
                bill != null ? bill.getBillNumber() : null);
    }

    public static OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item) {
        Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
        return new OrderResponse.OrderItemResponse(
                item.getId(),
                productId,
                item.getProductNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }

    public static BillResponse toBillResponse(Bill bill) {
        Order order = bill.getOrder();
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(DtoMapper::toOrderItemResponse)
                .toList();

        return new BillResponse(
                bill.getId(),
                bill.getBillNumber(),
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerContact(),
                order.getCreatedBy() != null ? order.getCreatedBy().getName() : null,
                bill.getTotalAmount(),
                bill.getPaymentMethod(),
                bill.getPaymentStatus(),
                bill.getCreatedAt(),
                items);
    }
}
