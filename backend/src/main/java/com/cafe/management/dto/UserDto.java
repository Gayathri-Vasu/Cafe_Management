package com.cafe.management.dto;

import com.cafe.management.entity.Gender;
import com.cafe.management.entity.Role;
import com.cafe.management.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only view of a user. Never contains the password or PIN hash. */
public record UserDto(
        Long id,
        String name,
        String lastName,
        String email,
        String staffCode,
        String phone,
        Gender gender,
        LocalDate dateOfBirth,
        String place,
        String address,
        boolean pinSet,
        boolean googleLinked,
        boolean appleLinked,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
