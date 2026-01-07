package com.example.documentsigner.api.dto;

public class ErrorResponse {
    public final boolean success = false;
    public final String error;
    public final String code;

    public ErrorResponse(String error, String code) {
        this.error = error;
        this.code = code;
    }
}
