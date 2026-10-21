package com.cafe.management.exception;

/** Thrown when a business rule is violated. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
