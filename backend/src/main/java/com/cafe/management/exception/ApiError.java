package com.cafe.management.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Standard error body returned by the global exception handler. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
