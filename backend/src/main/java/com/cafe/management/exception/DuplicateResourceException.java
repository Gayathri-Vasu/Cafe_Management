package com.cafe.management.exception;

/** Thrown when a unique value (email, category name, ...) already exists. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
