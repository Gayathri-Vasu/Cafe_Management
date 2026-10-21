package com.cafe.management.dto;

/** Simple JSON message wrapper, e.g. {@code {"message":"Order created"}}. */
public record ApiMessage(String message) {
}
