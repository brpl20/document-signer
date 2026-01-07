package com.example.documentsigner.exception;

public class InvalidCertificateException extends SigningException {

    public InvalidCertificateException(String message) {
        super(message, "INVALID_CERTIFICATE");
    }

    public InvalidCertificateException(String message, Throwable cause) {
        super(message, "INVALID_CERTIFICATE", cause);
    }
}
