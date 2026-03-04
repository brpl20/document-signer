/**
 * ProcStudio Document Signer
 *
 * A comprehensive document signing utility for PDF files using ICP-Brasil A1 certificates.
 * Supports GUI mode, API mode, and ITI Verificador integration.
 *
 * Version: 2.0
 * Last Updated: 2026-01-12
 *
 * Dependencies:
 * - BouncyCastle (bcprov-jdk18on, bcpkix-jdk18on)
 * - Apache PDFBox (pdfbox)
 * - Spring Boot (for API mode)
 *
 * Usage:
 *   GUI Mode: java -jar ProcStudioSigner.jar
 *   API Mode: java -jar ProcStudioSigner.jar --api
 *   Help:     java -jar ProcStudioSigner.jar --help
 */

package com.example.documentsigner;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main Document Signer class containing all functionality for signing PDF documents
 * with ICP-Brasil A1 certificates (.pfx/.p12 files).
 */
public class DocumentSigner {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // ============================================================================
    // Core Signing Methods
    // ============================================================================

    /**
     * Sign a document using a certificate file path.
     *
     * @param document    The document bytes to sign
     * @param pfxPath     Path to the PFX/PKCS12 certificate file
     * @param pfxPassword The certificate password
     * @return The signed data in CMS/PKCS#7 format (.p7s)
     * @throws Exception if signing fails
     */
    public byte[] signDocument(byte[] document, String pfxPath, String pfxPassword) throws Exception {
        return signDocumentWithStream(document, new FileInputStream(pfxPath), pfxPassword);
    }

    /**
     * Sign a document using certificate bytes.
     *
     * @param document    The document bytes to sign
     * @param certBytes   The PFX/PKCS12 certificate bytes
     * @param pfxPassword The certificate password
     * @return The signed data in CMS/PKCS#7 format (.p7s)
     * @throws Exception if signing fails
     */
    public byte[] signDocumentWithCertBytes(byte[] document, byte[] certBytes, String pfxPassword) throws Exception {
        return signDocumentWithStream(document, new ByteArrayInputStream(certBytes), pfxPassword);
    }

    /**
     * Internal method to sign document with certificate from input stream.
     */
    private byte[] signDocumentWithStream(byte[] document, InputStream certStream, String pfxPassword) throws Exception {
        // Load the PFX/PKCS12 keystore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(certStream, pfxPassword.toCharArray());

        // Get the private key and certificate
        String alias = keystore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, pfxPassword.toCharArray());
        Certificate[] certificateChain = keystore.getCertificateChain(alias);
        X509Certificate signingCert = (X509Certificate) certificateChain[0];

        // Create certificate store with full chain
        List<Certificate> certList = new ArrayList<>();
        for (Certificate cert : certificateChain) {
            certList.add(cert);
        }
        Store certStore = new JcaCertStore(certList);

        // Create CMS SignedData generator
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

        // Add signing parameters
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        cmsGenerator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                                .setProvider("BC")
                                .build())
                        .build(contentSigner, signingCert));

        // Add certificates to the signature
        cmsGenerator.addCertificates(certStore);

        // Create signed data
        CMSTypedData cmsData = new CMSProcessableByteArray(document);
        CMSSignedData signedData = cmsGenerator.generate(cmsData, true);

        return signedData.getEncoded();
    }

    /**
     * Verify a signature against the original document data.
     *
     * @param signedData   The signed data (.p7s content)
     * @param originalData The original document bytes
     * @return true if signature is valid
     * @throws Exception if verification fails
     */
    public boolean verifySignature(byte[] signedData, byte[] originalData) throws Exception {
        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(originalData), signedData);
        Store<X509CertificateHolder> certStore = cms.getCertificates();
        SignerInformationStore signers = cms.getSignerInfos();

        for (SignerInformation signer : signers.getSigners()) {
            Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
            X509CertificateHolder cert = certCollection.iterator().next();

            if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(new JcaX509CertificateConverter().getCertificate(cert)))) {
                return false;
            }
        }
        return true;
    }

    // ============================================================================
    // Exception Classes
    // ============================================================================

    /**
     * Base exception for all signing-related errors.
     */
    public static class SigningException extends RuntimeException {
        private final String errorCode;

        public SigningException(String message) {
            super(message);
            this.errorCode = "SIGNING_ERROR";
        }

        public SigningException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public SigningException(String message, Throwable cause) {
            super(message, cause);
            this.errorCode = "SIGNING_ERROR";
        }

        public SigningException(String message, String errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Exception thrown when the document is invalid or cannot be processed.
     */
    public static class InvalidDocumentException extends SigningException {
        public InvalidDocumentException(String message) {
            super(message, "INVALID_DOCUMENT");
        }

        public InvalidDocumentException(String message, Throwable cause) {
            super(message, "INVALID_DOCUMENT", cause);
        }
    }

    /**
     * Exception thrown when the certificate is invalid or cannot be loaded.
     */
    public static class InvalidCertificateException extends SigningException {
        public InvalidCertificateException(String message) {
            super(message, "INVALID_CERTIFICATE");
        }

        public InvalidCertificateException(String message, Throwable cause) {
            super(message, "INVALID_CERTIFICATE", cause);
        }
    }

    /**
     * Exception thrown when the certificate password is incorrect.
     */
    public static class InvalidPasswordException extends SigningException {
        public InvalidPasswordException(String message) {
            super(message, "INVALID_PASSWORD");
        }

        public InvalidPasswordException(String message, Throwable cause) {
            super(message, "INVALID_PASSWORD", cause);
        }
    }

    /**
     * Exception thrown when attempting to sign with an expired certificate.
     */
    public static class ExpiredCertificateException extends SigningException {
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

    // ============================================================================
    // Certificate Info DTO
    // ============================================================================

    /**
     * DTO containing certificate details for display and validation.
     */
    public static class CertificateInfo {
        private boolean valid;
        private String subject;
        private String commonName;
        private String issuer;
        private String serialNumber;
        private Date notBefore;
        private Date notAfter;
        private boolean expired;
        private long daysUntilExpiry;
        private String algorithm;
        private String error;

        public CertificateInfo() {
        }

        public CertificateInfo(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public CertificateInfo(boolean valid, String subject, String commonName, String issuer,
                               String serialNumber, Date notBefore, Date notAfter,
                               boolean expired, long daysUntilExpiry, String algorithm) {
            this.valid = valid;
            this.subject = subject;
            this.commonName = commonName;
            this.issuer = issuer;
            this.serialNumber = serialNumber;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            this.expired = expired;
            this.daysUntilExpiry = daysUntilExpiry;
            this.algorithm = algorithm;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getCommonName() { return commonName; }
        public void setCommonName(String commonName) { this.commonName = commonName; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public Date getNotBefore() { return notBefore; }
        public void setNotBefore(Date notBefore) { this.notBefore = notBefore; }
        public Date getNotAfter() { return notAfter; }
        public void setNotAfter(Date notAfter) { this.notAfter = notAfter; }
        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }
        public long getDaysUntilExpiry() { return daysUntilExpiry; }
        public void setDaysUntilExpiry(long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        @Override
        public String toString() {
            if (!valid) {
                return "Certificate Error: " + error;
            }
            return String.format(
                    "Subject: %s\nCommon Name: %s\nIssuer: %s\nSerial Number: %s\n" +
                            "Valid From: %s\nValid Until: %s\nStatus: %s\nDays Until Expiry: %d\nAlgorithm: %s",
                    subject, commonName, issuer, serialNumber,
                    notBefore, notAfter,
                    expired ? "EXPIRED" : "VALID",
                    daysUntilExpiry, algorithm
            );
        }
    }

    // ============================================================================
    // Certificate Validator
    // ============================================================================

    /**
     * Utility class for validating A1 certificates and extracting their details.
     */
    public static class CertificateValidator {

        /**
         * Validate certificate password from file path.
         */
        public static boolean validatePassword(String certPath, String password) {
            try (FileInputStream fis = new FileInputStream(certPath)) {
                return validatePassword(fis, password);
            } catch (FileNotFoundException e) {
                throw new InvalidCertificateException("Certificate file not found: " + certPath);
            } catch (IOException e) {
                throw new InvalidCertificateException("Error reading certificate file: " + e.getMessage());
            }
        }

        /**
         * Validate certificate password from bytes.
         */
        public static boolean validatePassword(byte[] certBytes, String password) {
            return validatePassword(new ByteArrayInputStream(certBytes), password);
        }

        /**
         * Validate certificate password from input stream.
         */
        private static boolean validatePassword(InputStream certStream, String password) {
            try {
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(certStream, password.toCharArray());

                String alias = keystore.aliases().nextElement();
                keystore.getKey(alias, password.toCharArray());

                return true;
            } catch (java.security.UnrecoverableKeyException e) {
                throw new InvalidPasswordException("Incorrect certificate password");
            } catch (IOException e) {
                if (e.getCause() instanceof java.security.UnrecoverableKeyException ||
                        e.getMessage().contains("password")) {
                    throw new InvalidPasswordException("Incorrect certificate password");
                }
                throw new InvalidCertificateException("Invalid certificate format: " + e.getMessage());
            } catch (Exception e) {
                throw new InvalidCertificateException("Error loading certificate: " + e.getMessage());
            }
        }

        /**
         * Check if certificate is expired from file path.
         */
        public static void checkExpiry(String certPath, String password) {
            try (FileInputStream fis = new FileInputStream(certPath)) {
                checkExpiry(fis, password);
            } catch (ExpiredCertificateException | InvalidPasswordException | InvalidCertificateException e) {
                throw e;
            } catch (FileNotFoundException e) {
                throw new InvalidCertificateException("Certificate file not found: " + certPath);
            } catch (IOException e) {
                throw new InvalidCertificateException("Error reading certificate file: " + e.getMessage());
            }
        }

        /**
         * Check if certificate is expired from bytes.
         */
        public static void checkExpiry(byte[] certBytes, String password) {
            checkExpiry(new ByteArrayInputStream(certBytes), password);
        }

        /**
         * Check if certificate is expired from input stream.
         */
        private static void checkExpiry(InputStream certStream, String password) {
            try {
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(certStream, password.toCharArray());

                String alias = keystore.aliases().nextElement();
                X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

                Date now = new Date();
                if (now.after(cert.getNotAfter())) {
                    throw new ExpiredCertificateException(
                            "Certificate expired on " + cert.getNotAfter(),
                            cert.getNotAfter()
                    );
                }
                if (now.before(cert.getNotBefore())) {
                    throw new InvalidCertificateException(
                            "Certificate is not yet valid. Valid from: " + cert.getNotBefore()
                    );
                }
            } catch (ExpiredCertificateException | InvalidCertificateException e) {
                throw e;
            } catch (IOException e) {
                if (e.getCause() instanceof java.security.UnrecoverableKeyException ||
                        e.getMessage().contains("password")) {
                    throw new InvalidPasswordException("Incorrect certificate password");
                }
                throw new InvalidCertificateException("Invalid certificate format: " + e.getMessage());
            } catch (Exception e) {
                throw new InvalidCertificateException("Error loading certificate: " + e.getMessage());
            }
        }

        /**
         * Get certificate details from file path.
         */
        public static CertificateInfo getCertificateInfo(String certPath, String password) {
            try (FileInputStream fis = new FileInputStream(certPath)) {
                return getCertificateInfo(fis, password);
            } catch (FileNotFoundException e) {
                return new CertificateInfo(false, "Certificate file not found: " + certPath);
            } catch (IOException e) {
                return new CertificateInfo(false, "Error reading certificate file: " + e.getMessage());
            }
        }

        /**
         * Get certificate details from bytes.
         */
        public static CertificateInfo getCertificateInfo(byte[] certBytes, String password) {
            return getCertificateInfo(new ByteArrayInputStream(certBytes), password);
        }

        /**
         * Get certificate details from input stream.
         */
        private static CertificateInfo getCertificateInfo(InputStream certStream, String password) {
            try {
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(certStream, password.toCharArray());

                String alias = keystore.aliases().nextElement();
                X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

                String subject = cert.getSubjectX500Principal().getName();
                String commonName = extractCN(subject);
                String issuer = cert.getIssuerX500Principal().getName();
                String serialNumber = cert.getSerialNumber().toString(16).toUpperCase();

                Date notBefore = cert.getNotBefore();
                Date notAfter = cert.getNotAfter();
                Date now = new Date();

                boolean expired = now.after(notAfter);
                long daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(notAfter.getTime() - now.getTime());

                String algorithm = cert.getSigAlgName();

                return new CertificateInfo(
                        true, subject, commonName, issuer, serialNumber,
                        notBefore, notAfter, expired, daysUntilExpiry, algorithm
                );

            } catch (IOException e) {
                if (e.getCause() instanceof java.security.UnrecoverableKeyException ||
                        e.getMessage().contains("password")) {
                    return new CertificateInfo(false, "Incorrect certificate password");
                }
                return new CertificateInfo(false, "Invalid certificate format: " + e.getMessage());
            } catch (Exception e) {
                return new CertificateInfo(false, "Error loading certificate: " + e.getMessage());
            }
        }

        /**
         * Full validation: password, expiry, and certificate validity.
         */
        public static void validateCertificate(String certPath, String password) {
            validatePassword(certPath, password);
            checkExpiry(certPath, password);
        }

        /**
         * Full validation from bytes.
         */
        public static void validateCertificate(byte[] certBytes, String password) {
            validatePassword(certBytes, password);
            checkExpiry(certBytes, password);
        }

        /**
         * Extract Common Name (CN) from X.500 distinguished name.
         */
        private static String extractCN(String dn) {
            Pattern pattern = Pattern.compile("CN=([^,]+)");
            Matcher matcher = pattern.matcher(dn);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return dn;
        }
    }

    // ============================================================================
    // PDF Signer
    // ============================================================================

    /**
     * High-level PDF signing utility with validation.
     */
    public static class PdfSigner {

        private final DocumentSigner documentSigner;

        public PdfSigner() {
            this.documentSigner = new DocumentSigner();
        }

        /**
         * Sign a PDF file and save the signature to a .p7s file.
         */
        public void signPdf(String inputPdf, String outputP7s, String pfxPath, String pfxPassword) {
            try {
                PDDocument document = PDDocument.load(new File(inputPdf));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);
                document.close();

                byte[] signedData = documentSigner.signDocument(baos.toByteArray(), pfxPath, pfxPassword);

                FileOutputStream fos = new FileOutputStream(outputP7s);
                fos.write(signedData);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Sign PDF bytes using certificate bytes.
         *
         * @param pdfBytes  The PDF document as byte array
         * @param certBytes The PFX/PKCS12 certificate as byte array
         * @param password  The certificate password
         * @return The P7S signature as byte array
         * @throws SigningException if signing fails
         */
        public byte[] signPdfBytes(byte[] pdfBytes, byte[] certBytes, String password) {
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new InvalidDocumentException("PDF document is empty or null");
            }
            if (certBytes == null || certBytes.length == 0) {
                throw new InvalidCertificateException("Certificate is empty or null");
            }
            if (password == null || password.isEmpty()) {
                throw new InvalidPasswordException("Password is required");
            }

            try {
                PDDocument document = PDDocument.load(pdfBytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);
                document.close();

                KeyStore keystore = KeyStore.getInstance("PKCS12");
                try {
                    keystore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
                } catch (IOException e) {
                    if (e.getCause() instanceof java.security.UnrecoverableKeyException) {
                        throw new InvalidPasswordException("Incorrect certificate password", e);
                    }
                    throw new InvalidCertificateException("Invalid certificate format", e);
                }

                String alias = keystore.aliases().nextElement();
                X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
                Date now = new Date();
                if (now.after(cert.getNotAfter())) {
                    throw new ExpiredCertificateException(
                            "Certificate expired on " + cert.getNotAfter(),
                            cert.getNotAfter()
                    );
                }
                if (now.before(cert.getNotBefore())) {
                    throw new InvalidCertificateException(
                            "Certificate is not yet valid. Valid from: " + cert.getNotBefore()
                    );
                }

                return documentSigner.signDocumentWithCertBytes(baos.toByteArray(), certBytes, password);

            } catch (InvalidDocumentException | InvalidCertificateException | InvalidPasswordException | ExpiredCertificateException e) {
                throw e;
            } catch (Exception e) {
                throw new SigningException("Failed to sign document: " + e.getMessage(), e);
            }
        }

        /**
         * Verify a signature against the original document.
         */
        public boolean verifySignature(byte[] signatureBytes, byte[] originalPdfBytes) {
            try {
                PDDocument document = PDDocument.load(originalPdfBytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);
                document.close();

                return documentSigner.verifySignature(signatureBytes, baos.toByteArray());
            } catch (Exception e) {
                throw new SigningException("Failed to verify signature: " + e.getMessage(), e);
            }
        }
    }

    // ============================================================================
    // ITI Verificador Integration
    // ============================================================================

    /**
     * Client for the ITI Verificador API - Official Brazilian Government signature validator.
     *
     * Production: https://verificador.iti.gov.br
     * Staging: https://verificador.staging.iti.br
     */
    public static class ItiVerificador {

        private static final String PRODUCTION_URL = "https://verificador.iti.gov.br/report";
        private static final String STAGING_URL = "https://verificador.staging.iti.br/report";

        private final String baseUrl;
        private final int timeoutMs;

        public ItiVerificador() {
            this(PRODUCTION_URL, 30000);
        }

        public ItiVerificador(boolean useStaging) {
            this(useStaging ? STAGING_URL : PRODUCTION_URL, 30000);
        }

        public ItiVerificador(String baseUrl, int timeoutMs) {
            this.baseUrl = baseUrl;
            this.timeoutMs = timeoutMs;
        }

        /**
         * Verify a detached signature (.p7s) against its original document.
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

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"report_type\"\r\n\r\n");
                    writer.append("json").append("\r\n");

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"signature_files[]\"; filename=\"")
                            .append(signatureFilename).append("\"\r\n");
                    writer.append("Content-Type: application/octet-stream\r\n\r\n");
                    writer.flush();
                    os.write(signatureBytes);
                    os.flush();
                    writer.append("\r\n");

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"detached_files[]\"; filename=\"")
                            .append(documentFilename).append("\"\r\n");
                    writer.append("Content-Type: application/pdf\r\n\r\n");
                    writer.flush();
                    os.write(documentBytes);
                    os.flush();
                    writer.append("\r\n");

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"verify_incremental_updates\"\r\n\r\n");
                    writer.append("true").append("\r\n");

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

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"report_type\"\r\n\r\n");
                    writer.append("json").append("\r\n");

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"signature_files[]\"; filename=\"")
                            .append(filename).append("\"\r\n");
                    writer.append("Content-Type: application/pdf\r\n\r\n");
                    writer.flush();
                    os.write(signedDocumentBytes);
                    os.flush();
                    writer.append("\r\n");

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"verify_incremental_updates\"\r\n\r\n");
                    writer.append("true").append("\r\n");

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

        public String getBaseUrl() {
            return baseUrl;
        }

        public static String getProductionUrl() {
            return PRODUCTION_URL;
        }

        public static String getStagingUrl() {
            return STAGING_URL;
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

            public boolean isSignatureValid() {
                if (!isSuccess()) {
                    return false;
                }
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
    }

    // ============================================================================
    // GUI Application
    // ============================================================================

    /**
     * Swing-based GUI for document signing.
     */
    public static class DocumentSignerUI {

        private static final String PREF_LAST_DIRECTORY = "lastDirectory";
        private static final String PREF_LAST_CERTIFICATE = "lastCertificate";

        private JFrame frame;
        private JTextField pfxPathField;
        private JPasswordField passwordField;
        private JTextArea logArea;
        private PdfSigner signer;
        private Preferences prefs;

        public static void launch(String[] args) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                DocumentSignerUI ui = new DocumentSignerUI();
                ui.createAndShowGUI();
            });
        }

        public void createAndShowGUI() {
            signer = new PdfSigner();
            prefs = Preferences.userNodeForPackage(DocumentSignerUI.class);

            frame = new JFrame("ProcStudio Signer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 500);

            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(0, 51, 102));

            boolean imagesLoaded = false;
            File iconFile = new File("procstudio_símbolo_sem_fundo.png");
            File logoFile = new File("procstudio_logotipo_horizontal_fundo_azul.png");

            if (iconFile.exists() && logoFile.exists()) {
                try {
                    Image appIcon = ImageIO.read(iconFile);
                    frame.setIconImage(appIcon);

                    ImageIcon logoIcon = new ImageIcon(logoFile.getAbsolutePath());
                    Image scaledImage = logoIcon.getImage().getScaledInstance(300, -1, Image.SCALE_SMOOTH);
                    JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                    logoLabel.setBorder(new EmptyBorder(10, 20, 10, 0));
                    headerPanel.add(logoLabel, BorderLayout.WEST);

                    imagesLoaded = true;
                } catch (Exception e) {
                    System.err.println("Error loading images from files: " + e.getMessage());
                }
            }

            if (!imagesLoaded) {
                JLabel titleLabel = new JLabel("ProcStudio Signer");
                titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setBorder(new EmptyBorder(10, 20, 10, 0));
                headerPanel.add(titleLabel, BorderLayout.CENTER);
            } else {
                JLabel titleLabel = new JLabel("ProcStudio Signer");
                titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setBorder(new EmptyBorder(0, 20, 0, 0));
                headerPanel.add(titleLabel, BorderLayout.EAST);
            }

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            JLabel pfxLabel = new JLabel("Certificate File (.pfx):");
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(pfxLabel, gbc);

            pfxPathField = new JTextField(20);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            formPanel.add(pfxPathField, gbc);

            JButton pfxBrowseButton = new JButton("Browse");
            pfxBrowseButton.addActionListener(e -> browsePfxFile());
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 0.0;
            formPanel.add(pfxBrowseButton, gbc);

            JLabel passwordLabel = new JLabel("Certificate Password:");
            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(passwordLabel, gbc);

            passwordField = new JPasswordField(20);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            formPanel.add(passwordField, gbc);

            JButton checkCertButton = new JButton("Check Certificate");
            checkCertButton.addActionListener(e -> showCertificateDetails());
            gbc.gridx = 2;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            formPanel.add(checkCertButton, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            JButton signFileButton = new JButton("Sign File");
            signFileButton.addActionListener(e -> signSingleFile());
            buttonPanel.add(signFileButton);

            JButton signMultipleButton = new JButton("Sign Multiple Files");
            signMultipleButton.addActionListener(e -> signMultipleFiles());
            buttonPanel.add(signMultipleButton);

            JButton signFolderButton = new JButton("Sign Folder");
            signFolderButton.addActionListener(e -> signFolder());
            buttonPanel.add(signFolderButton);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 3;
            formPanel.add(buttonPanel, gbc);

            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setLineWrap(true);
            logArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(logArea);
            scrollPane.setPreferredSize(new Dimension(500, 200));

            mainPanel.add(formPanel, BorderLayout.NORTH);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            frame.getContentPane().add(headerPanel, BorderLayout.NORTH);
            frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);

            String savedCertPath = prefs.get(PREF_LAST_CERTIFICATE, "");
            if (!savedCertPath.isEmpty() && new File(savedCertPath).exists()) {
                pfxPathField.setText(savedCertPath);
                log("Loaded saved certificate: " + savedCertPath);
            }

            setupDragAndDrop(mainPanel);

            frame.setVisible(true);
        }

        private void setupDragAndDrop(JPanel panel) {
            new DropTarget(panel, new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                        panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(0, 120, 215), 3),
                                new EmptyBorder(7, 17, 17, 17)
                        ));
                    } else {
                        dtde.rejectDrag();
                    }
                }

                @Override
                public void dragOver(DropTargetDragEvent dtde) {}

                @Override
                public void dropActionChanged(DropTargetDragEvent dtde) {}

                @Override
                public void dragExit(DropTargetEvent dte) {
                    panel.setBorder(new EmptyBorder(10, 20, 20, 20));
                }

                @Override
                @SuppressWarnings("unchecked")
                public void drop(DropTargetDropEvent dtde) {
                    panel.setBorder(new EmptyBorder(10, 20, 20, 20));
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable transferable = dtde.getTransferable();
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        if (!validateCertificateForSigning()) {
                            log("Please select a certificate and enter password before dropping files.");
                            dtde.dropComplete(false);
                            return;
                        }

                        int pdfCount = 0;
                        for (File file : files) {
                            if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                                signFile(file);
                                pdfCount++;
                            } else if (file.isDirectory()) {
                                File[] pdfFiles = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                                if (pdfFiles != null) {
                                    for (File pdfFile : pdfFiles) {
                                        signFile(pdfFile);
                                        pdfCount++;
                                    }
                                }
                            }
                        }

                        if (pdfCount == 0) {
                            log("No PDF files found in dropped items.");
                        } else {
                            log("Processed " + pdfCount + " PDF file(s) via drag-and-drop.");
                        }
                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        log("Error processing dropped files: " + e.getMessage());
                        dtde.dropComplete(false);
                    }
                }
            });
        }

        private void browsePfxFile() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Certificate File");

            String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
            fileChooser.setCurrentDirectory(new File(lastDir));

            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PFX Files", "pfx"));
            fileChooser.setAcceptAllFileFilterUsed(true);

            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                pfxPathField.setText(selectedFile.getAbsolutePath());

                prefs.put(PREF_LAST_CERTIFICATE, selectedFile.getAbsolutePath());
                prefs.put(PREF_LAST_DIRECTORY, selectedFile.getParent());
            }
        }

        private void signSingleFile() {
            if (!validateCertificateForSigning()) return;

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select PDF to Sign");

            String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
            fileChooser.setCurrentDirectory(new File(lastDir));

            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
            fileChooser.setAcceptAllFileFilterUsed(true);

            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                prefs.put(PREF_LAST_DIRECTORY, selectedFile.getParent());
                signFile(selectedFile);
            }
        }

        private void signMultipleFiles() {
            if (!validateCertificateForSigning()) return;

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select PDFs to Sign");

            String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
            fileChooser.setCurrentDirectory(new File(lastDir));

            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setMultiSelectionEnabled(true);

            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles.length > 0) {
                    prefs.put(PREF_LAST_DIRECTORY, selectedFiles[0].getParent());
                }
                for (File file : selectedFiles) {
                    signFile(file);
                }
            }
        }

        private void signFolder() {
            if (!validateCertificateForSigning()) return;

            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setDialogTitle("Select Folder with PDFs");
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            directoryChooser.setAcceptAllFileFilterUsed(true);

            String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
            directoryChooser.setCurrentDirectory(new File(lastDir));

            if (directoryChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = directoryChooser.getSelectedFile();
                prefs.put(PREF_LAST_DIRECTORY, selectedDirectory.getAbsolutePath());

                File[] files = selectedDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

                if (files != null && files.length > 0) {
                    for (File file : files) {
                        signFile(file);
                    }
                    log("Processed " + files.length + " files from folder: " + selectedDirectory.getAbsolutePath());
                } else {
                    log("No PDF files found in the selected folder.");
                }
            }
        }

        private boolean validateCertificate() {
            String pfxPath = pfxPathField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (pfxPath.isEmpty()) {
                log("Error: Please select a certificate file.");
                return false;
            }

            if (password.isEmpty()) {
                log("Error: Please enter the certificate password.");
                return false;
            }

            File pfxFile = new File(pfxPath);
            if (!pfxFile.exists() || !pfxFile.isFile()) {
                log("Error: Certificate file does not exist.");
                return false;
            }

            return true;
        }

        private void signFile(File pdfFile) {
            try {
                String inputPath = pdfFile.getAbsolutePath();
                String outputPath = inputPath + ".p7s";
                String pfxPath = pfxPathField.getText().trim();
                String password = new String(passwordField.getPassword());

                signer.signPdf(inputPath, outputPath, pfxPath, password);
                log("Successfully signed: " + pdfFile.getName() + " -> " + new File(outputPath).getName());
            } catch (Exception e) {
                log("Error signing " + pdfFile.getName() + ": " + e.getMessage());
            }
        }

        private void log(String message) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }

        private void showCertificateDetails() {
            String pfxPath = pfxPathField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (pfxPath.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please select a certificate file first.",
                        "No Certificate Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter the certificate password.",
                        "Password Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            File pfxFile = new File(pfxPath);
            if (!pfxFile.exists()) {
                JOptionPane.showMessageDialog(frame,
                        "Certificate file not found: " + pfxPath,
                        "File Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            CertificateInfo info = CertificateValidator.getCertificateInfo(pfxPath, password);

            if (!info.isValid()) {
                JOptionPane.showMessageDialog(frame,
                        info.getError(),
                        "Certificate Error",
                        JOptionPane.ERROR_MESSAGE);
                log("Certificate check failed: " + info.getError());
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            StringBuilder details = new StringBuilder();
            details.append("=== CERTIFICATE DETAILS ===\n\n");
            details.append("Common Name: ").append(info.getCommonName()).append("\n\n");
            details.append("Subject: ").append(info.getSubject()).append("\n\n");
            details.append("Issuer: ").append(info.getIssuer()).append("\n\n");
            details.append("Serial Number: ").append(info.getSerialNumber()).append("\n\n");
            details.append("Valid From: ").append(dateFormat.format(info.getNotBefore())).append("\n");
            details.append("Valid Until: ").append(dateFormat.format(info.getNotAfter())).append("\n\n");

            if (info.isExpired()) {
                details.append("STATUS: EXPIRED\n");
            } else if (info.getDaysUntilExpiry() <= 30) {
                details.append("STATUS: EXPIRING SOON (").append(info.getDaysUntilExpiry()).append(" days left)\n");
            } else {
                details.append("STATUS: VALID (").append(info.getDaysUntilExpiry()).append(" days until expiry)\n");
            }

            details.append("\nAlgorithm: ").append(info.getAlgorithm());

            JTextArea textArea = new JTextArea(details.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 350));

            int messageType = info.isExpired() ? JOptionPane.ERROR_MESSAGE :
                    (info.getDaysUntilExpiry() <= 30 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

            JOptionPane.showMessageDialog(frame, scrollPane, "Certificate Details", messageType);

            log("Certificate loaded: " + info.getCommonName() +
                    (info.isExpired() ? " [EXPIRED]" : " [Valid for " + info.getDaysUntilExpiry() + " days]"));
        }

        private boolean validateCertificateForSigning() {
            String pfxPath = pfxPathField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (!validateCertificate()) {
                return false;
            }

            try {
                CertificateValidator.validatePassword(pfxPath, password);
                CertificateValidator.checkExpiry(pfxPath, password);
                return true;

            } catch (InvalidPasswordException e) {
                log("Error: Incorrect certificate password.");
                JOptionPane.showMessageDialog(frame,
                        "The certificate password is incorrect.\nPlease check and try again.",
                        "Invalid Password",
                        JOptionPane.ERROR_MESSAGE);
                return false;

            } catch (ExpiredCertificateException e) {
                log("Error: Certificate has expired.");
                int choice = JOptionPane.showConfirmDialog(frame,
                        "This certificate has expired!\n\n" + e.getMessage() +
                                "\n\nExpired certificates should not be used for new signatures.\nDo you want to continue anyway?",
                        "Certificate Expired",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                return choice == JOptionPane.YES_OPTION;

            } catch (Exception e) {
                log("Error validating certificate: " + e.getMessage());
                JOptionPane.showMessageDialog(frame,
                        "Error validating certificate:\n" + e.getMessage(),
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }

    // ============================================================================
    // Main Entry Point
    // ============================================================================

    /**
     * Main method - entry point for the application.
     *
     * Usage:
     *   GUI Mode: java -jar ProcStudioSigner.jar
     *   API Mode: java -jar ProcStudioSigner.jar --api
     *   Help:     java -jar ProcStudioSigner.jar --help
     */
    public static void main(String[] args) {
        if (hasFlag(args, "--api") || hasFlag(args, "-a")) {
            System.out.println("Starting Document Signer API...");
            System.out.println("API mode requires Spring Boot - run the full JAR with Spring dependencies.");
            System.out.println("Or use: mvn spring-boot:run -Dspring-boot.run.arguments=--api");
        } else if (hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printHelp();
        } else {
            // Default: Start GUI mode
            DocumentSignerUI.launch(args);
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("ProcStudio Document Signer v2.0");
        System.out.println("");
        System.out.println("Usage: java -jar ProcStudioSigner.jar [options]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  (no options)    Start GUI mode (default)");
        System.out.println("  --api, -a       Start API server mode (port 8080)");
        System.out.println("  --help, -h      Show this help message");
        System.out.println("");
        System.out.println("Features:");
        System.out.println("  - Sign PDF documents with ICP-Brasil A1 certificates (.pfx/.p12)");
        System.out.println("  - Certificate validation (password, expiry, validity)");
        System.out.println("  - ITI Verificador integration for official signature validation");
        System.out.println("  - Drag-and-drop support for easy file signing");
        System.out.println("  - Batch signing (multiple files or folders)");
        System.out.println("");
        System.out.println("API Endpoints (when running in API mode):");
        System.out.println("  GET  /api/v1/health           Health check");
        System.out.println("  POST /api/v1/sign             Sign a PDF document");
        System.out.println("  POST /api/v1/sign/json        Sign and return base64 signature");
        System.out.println("  POST /api/v1/sign/batch       Sign multiple PDFs");
        System.out.println("  POST /api/v1/sign/verified    Sign and verify with ITI");
        System.out.println("  POST /api/v1/verify           Verify a signature");
        System.out.println("  POST /api/v1/verify/iti       Verify with ITI Verificador");
        System.out.println("  POST /api/v1/certificate/info Get certificate details");
        System.out.println("  POST /api/v1/certificate/validate  Validate certificate");
        System.out.println("");
        System.out.println("Example API usage:");
        System.out.println("  curl -X POST http://localhost:8080/api/v1/sign \\");
        System.out.println("    -F \"document=@document.pdf\" \\");
        System.out.println("    -F \"certificate=@certificate.pfx\" \\");
        System.out.println("    -F \"password=your_password\" \\");
        System.out.println("    -o document.pdf.p7s");
        System.out.println("");
        System.out.println("ITI Verificador:");
        System.out.println("  Production: https://verificador.iti.gov.br");
        System.out.println("  Staging:    https://verificador.staging.iti.br");
        System.out.println("  Documentation: https://validar.iti.gov.br/guia-desenvolvedor.html");
    }
}
