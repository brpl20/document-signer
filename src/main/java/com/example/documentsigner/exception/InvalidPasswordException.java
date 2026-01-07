package com.example.documentsigner.exception;

public class InvalidPasswordException extends SigningException {

    public InvalidPasswordException(String message) {
        super(message, "INVALID_PASSWORD");
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, "INVALID_PASSWORD", cause);
    }
}
