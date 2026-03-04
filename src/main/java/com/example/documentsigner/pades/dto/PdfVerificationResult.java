package com.example.documentsigner.pades.dto;

import java.util.Date;

/**
 * Result of PDF signature verification.
 */
public class PdfVerificationResult {
    private boolean valid;
    private String signerName;
    private Date signingTime;
    private String reason;
    private boolean certificateValid;
    private boolean integrityValid;
    private boolean coversWholeDocument;
    private String details;

    public PdfVerificationResult() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public Date getSigningTime() {
        return signingTime;
    }

    public void setSigningTime(Date signingTime) {
        this.signingTime = signingTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isCertificateValid() {
        return certificateValid;
    }

    public void setCertificateValid(boolean certificateValid) {
        this.certificateValid = certificateValid;
    }

    public boolean isIntegrityValid() {
        return integrityValid;
    }

    public void setIntegrityValid(boolean integrityValid) {
        this.integrityValid = integrityValid;
    }

    public boolean isCoversWholeDocument() {
        return coversWholeDocument;
    }

    public void setCoversWholeDocument(boolean coversWholeDocument) {
        this.coversWholeDocument = coversWholeDocument;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PdfVerificationResult result = new PdfVerificationResult();

        public Builder valid(boolean valid) {
            result.setValid(valid);
            return this;
        }

        public Builder signerName(String signerName) {
            result.setSignerName(signerName);
            return this;
        }

        public Builder signingTime(Date signingTime) {
            result.setSigningTime(signingTime);
            return this;
        }

        public Builder reason(String reason) {
            result.setReason(reason);
            return this;
        }

        public Builder certificateValid(boolean certificateValid) {
            result.setCertificateValid(certificateValid);
            return this;
        }

        public Builder integrityValid(boolean integrityValid) {
            result.setIntegrityValid(integrityValid);
            return this;
        }

        public Builder coversWholeDocument(boolean coversWholeDocument) {
            result.setCoversWholeDocument(coversWholeDocument);
            return this;
        }

        public Builder details(String details) {
            result.setDetails(details);
            return this;
        }

        public PdfVerificationResult build() {
            return result;
        }
    }
}
