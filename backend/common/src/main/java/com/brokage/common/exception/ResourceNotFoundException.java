package com.brokage.common.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String identifier) {
        super("NOT_FOUND", String.format("%s not found with identifier: %s", resource, identifier));
    }

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
