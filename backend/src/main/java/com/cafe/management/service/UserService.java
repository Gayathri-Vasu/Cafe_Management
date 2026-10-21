package com.cafe.management.service;

import com.cafe.management.dto.CreateUserRequest;
import com.cafe.management.dto.UserDto;
import com.cafe.management.entity.UserStatus;

import java.util.List;

public interface UserService {

    /** Lists staff accounts; {@code search} matches name or email (may be null). */
    List<UserDto> getAllUsers(String search);

    /** Admin creates a new staff (or admin) account. */
    UserDto createUser(CreateUserRequest request);

    /** Enables or disables an account. */
    UserDto updateStatus(Long id, UserStatus status);

    UserDto getById(Long id);
}
