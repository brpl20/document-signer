package com.example.documentsigner.exception;

public class SigningException extends RuntimeException {

    private final String errorCode;

    public SigningException(String message) {
        super(message);
        this.errorCode = "SIGNING_ERROR";
    }

    public SigningException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SigningException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SIGNING_ERROR";
    }

    public SigningException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
