package com.example.documentsigner.api.dto;

public class VerifyResponse {
    public final boolean valid;
    public final String filename;

    public VerifyResponse(boolean valid, String filename) {
        this.valid = valid;
        this.filename = filename;
    }
}
