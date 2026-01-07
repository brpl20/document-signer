package com.example.documentsigner.exception;

public class InvalidDocumentException extends SigningException {

    public InvalidDocumentException(String message) {
        super(message, "INVALID_DOCUMENT");
    }

    public InvalidDocumentException(String message, Throwable cause) {
        super(message, "INVALID_DOCUMENT", cause);
    }
}
