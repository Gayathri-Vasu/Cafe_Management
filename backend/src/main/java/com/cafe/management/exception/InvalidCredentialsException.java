package com.cafe.management.exception;

/**
 * Raised when an alternative sign-in attempt (staff PIN, SMS code, Google or
 * Apple) fails. Always answered with HTTP 401 so the client can show the
 * message without treating it as a server fault.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
