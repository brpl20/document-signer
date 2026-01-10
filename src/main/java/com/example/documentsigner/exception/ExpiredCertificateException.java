package com.example.documentsigner.exception;

import java.util.Date;

/**
 * Exception thrown when attempting to sign with an expired certificate.
 */
public class ExpiredCertificateException extends SigningException {

    private final Date expirationDate;

    public ExpiredCertificateException(String message) {
        super(message, "CERTIFICATE_EXPIRED");
        this.expirationDate = null;
    }

    public ExpiredCertificateException(String message, Date expirationDate) {
        super(message, "CERTIFICATE_EXPIRED");
        this.expirationDate = expirationDate;
    }

    public ExpiredCertificateException(String message, Date expirationDate, Throwable cause) {
        super(message, "CERTIFICATE_EXPIRED", cause);
        this.expirationDate = expirationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }
}
