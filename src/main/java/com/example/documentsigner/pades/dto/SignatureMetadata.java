package com.example.documentsigner.pades.dto;

/**
 * Metadata for PAdES signature.
 */
public class SignatureMetadata {
    private String reason;
    private String location;
    private String contactInfo;

    public SignatureMetadata() {
    }

    public SignatureMetadata(String reason, String location, String contactInfo) {
        this.reason = reason;
        this.location = location;
        this.contactInfo = contactInfo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String reason;
        private String location;
        private String contactInfo;

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder contactInfo(String contactInfo) {
            this.contactInfo = contactInfo;
            return this;
        }

        public SignatureMetadata build() {
            return new SignatureMetadata(reason, location, contactInfo);
        }
    }
}
