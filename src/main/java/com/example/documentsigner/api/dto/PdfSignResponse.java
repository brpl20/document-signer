package com.example.documentsigner.api.dto;

/**
 * Response DTO for PAdES PDF signing operations returning JSON.
 */
public class PdfSignResponse {
    public boolean success;
    public String signedPdfBase64;
    public String filename;
    public String originalFilename;
    public SignatureInfo signatureInfo;
    public String timestamp;
    public String error;

    public PdfSignResponse() {
    }

    public static PdfSignResponse success(String signedPdfBase64, String filename,
                                           String originalFilename, SignatureInfo signatureInfo,
                                           String timestamp) {
        PdfSignResponse response = new PdfSignResponse();
        response.success = true;
        response.signedPdfBase64 = signedPdfBase64;
        response.filename = filename;
        response.originalFilename = originalFilename;
        response.signatureInfo = signatureInfo;
        response.timestamp = timestamp;
        return response;
    }

    public static PdfSignResponse error(String error) {
        PdfSignResponse response = new PdfSignResponse();
        response.success = false;
        response.error = error;
        return response;
    }

    /**
     * Information about the signature.
     */
    public static class SignatureInfo {
        public String signerName;
        public String signingTime;
        public String reason;
        public boolean visibleSignature;

        public SignatureInfo() {
        }

        public SignatureInfo(String signerName, String signingTime, String reason, boolean visibleSignature) {
            this.signerName = signerName;
            this.signingTime = signingTime;
            this.reason = reason;
            this.visibleSignature = visibleSignature;
        }
    }
}
