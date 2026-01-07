package com.example.documentsigner.api.dto;

public class SignResponse {
    public final boolean success;
    public final String signature;
    public final String filename;
    public final String timestamp;

    public SignResponse(boolean success, String signature, String filename, String timestamp) {
        this.success = success;
        this.signature = signature;
        this.filename = filename;
        this.timestamp = timestamp;
    }
}
