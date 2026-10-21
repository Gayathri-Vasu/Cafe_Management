package com.cafe.management.dto;

import com.cafe.management.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/** Payload to enable/disable a user account. */
public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
