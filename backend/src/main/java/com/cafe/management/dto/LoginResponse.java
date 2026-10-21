package com.cafe.management.dto;

/** Response returned after a successful login. The password hash is never included. */
public record LoginResponse(
        String token,
        String tokenType,
        Long id,
        String name,
        String email,
        String role
) {
}
