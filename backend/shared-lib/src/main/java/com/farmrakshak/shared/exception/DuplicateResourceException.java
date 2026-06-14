package com.farmrakshak.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {
    public DuplicateResourceException(String resource, String field, String value) {
        super(
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE",
            String.format("%s already exists with %s: %s", resource, field, value)
        );
    }
}
