package com.vigilonix.applicationnadministrativeservice.exception;

public class AuthProviderException extends RuntimeException {
    public AuthProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProviderException(String message) {
        super(message);
    }
}
