package com.example.documentsigner;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Client for the ITI Verificador API - Official Brazilian Government signature validator.
 *
 * Production: https://verificador.iti.gov.br
 * Staging: https://verificador.staging.iti.br
 *
 * Documentation: https://validar.iti.gov.br/guia-desenvolvedor.html
 */
public class ItiVerificador {

    private static final String PRODUCTION_URL = "https://verificador.iti.gov.br/report";
    private static final String STAGING_URL = "https://verificador.staging.iti.br/report";

    private final String baseUrl;
    private final int timeoutMs;

    /**
     * Create a new ITI Verificador client using the production endpoint.
     */
    public ItiVerificador() {
        this(PRODUCTION_URL, 30000);
    }

    /**
     * Create a new ITI Verificador client.
     *
     * @param useStaging true to use staging environment, false for production
     */
    public ItiVerificador(boolean useStaging) {
        this(useStaging ? STAGING_URL : PRODUCTION_URL, 30000);
    }

    /**
     * Create a new ITI Verificador client with custom URL and timeout.
     *
     * @param baseUrl The API base URL
     * @param timeoutMs Connection timeout in milliseconds
     */
    public ItiVerificador(String baseUrl, int timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Verify a detached signature (.p7s) against its original document.
     *
     * @param signatureBytes The .p7s signature bytes
     * @param documentBytes The original document bytes
     * @param signatureFilename Filename for the signature (e.g., "document.pdf.p7s")
     * @param documentFilename Filename for the document (e.g., "document.pdf")
     * @return ItiVerificationResult containing the validation response
     * @throws IOException if the request fails
     */
    public ItiVerificationResult verifyDetachedSignature(
            byte[] signatureBytes,
            byte[] documentBytes,
            String signatureFilename,
            String documentFilename) throws IOException {

        String boundary = "----Boundary" + UUID.randomUUID().toString().replace("-", "");

        URL url = new URL(baseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // report_type field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"report_type\"\r\n\r\n");
                writer.append("json").append("\r\n");

                // signature_files[] field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"signature_files[]\"; filename=\"")
                      .append(signatureFilename).append("\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n\r\n");
                writer.flush();
                os.write(signatureBytes);
                os.flush();
                writer.append("\r\n");

                // detached_files[] field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"detached_files[]\"; filename=\"")
                      .append(documentFilename).append("\"\r\n");
                writer.append("Content-Type: application/pdf\r\n\r\n");
                writer.flush();
                os.write(documentBytes);
                os.flush();
                writer.append("\r\n");

                // verify_incremental_updates field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"verify_incremental_updates\"\r\n\r\n");
                writer.append("true").append("\r\n");

                // End boundary
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody;

            InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    responseBody = sb.toString();
                }
            } else {
                responseBody = "";
            }

            return new ItiVerificationResult(responseCode, responseBody);

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Verify an embedded signature (signed PDF).
     *
     * @param signedDocumentBytes The signed document bytes
     * @param filename Filename for the document (e.g., "document_signed.pdf")
     * @return ItiVerificationResult containing the validation response
     * @throws IOException if the request fails
     */
    public ItiVerificationResult verifyEmbeddedSignature(
            byte[] signedDocumentBytes,
            String filename) throws IOException {

        String boundary = "----Boundary" + UUID.randomUUID().toString().replace("-", "");

        URL url = new URL(baseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // report_type field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"report_type\"\r\n\r\n");
                writer.append("json").append("\r\n");

                // signature_files[] field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"signature_files[]\"; filename=\"")
                      .append(filename).append("\"\r\n");
                writer.append("Content-Type: application/pdf\r\n\r\n");
                writer.flush();
                os.write(signedDocumentBytes);
                os.flush();
                writer.append("\r\n");

                // verify_incremental_updates field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"verify_incremental_updates\"\r\n\r\n");
                writer.append("true").append("\r\n");

                // End boundary
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody;

            InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    responseBody = sb.toString();
                }
            } else {
                responseBody = "";
            }

            return new ItiVerificationResult(responseCode, responseBody);

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Result from ITI Verificador API.
     */
    public static class ItiVerificationResult {
        private final int httpStatus;
        private final String jsonResponse;

        public ItiVerificationResult(int httpStatus, String jsonResponse) {
            this.httpStatus = httpStatus;
            this.jsonResponse = jsonResponse;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getJsonResponse() {
            return jsonResponse;
        }

        public boolean isSuccess() {
            return httpStatus >= 200 && httpStatus < 300;
        }

        /**
         * Check if the signature is valid based on the response.
         * This is a simple check - for production use, parse the full JSON response.
         */
        public boolean isSignatureValid() {
            if (!isSuccess()) {
                return false;
            }
            // The ITI response contains validation status in the JSON
            // A valid signature will have "valido": true or similar indicators
            // This is a simplified check - enhance based on actual API response structure
            return jsonResponse != null &&
                   (jsonResponse.contains("\"aprovado\"") ||
                    jsonResponse.contains("\"valido\"") ||
                    jsonResponse.contains("\"valid\""));
        }

        @Override
        public String toString() {
            return String.format("ItiVerificationResult{httpStatus=%d, success=%s, response=%s}",
                httpStatus, isSuccess(),
                jsonResponse != null && jsonResponse.length() > 200
                    ? jsonResponse.substring(0, 200) + "..."
                    : jsonResponse);
        }
    }

    // Getters
    public String getBaseUrl() {
        return baseUrl;
    }

    public static String getProductionUrl() {
        return PRODUCTION_URL;
    }

    public static String getStagingUrl() {
        return STAGING_URL;
    }
}
