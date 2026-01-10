package com.example.documentsigner;

import com.example.documentsigner.exception.ExpiredCertificateException;
import com.example.documentsigner.exception.InvalidCertificateException;
import com.example.documentsigner.exception.InvalidDocumentException;
import com.example.documentsigner.exception.InvalidPasswordException;
import com.example.documentsigner.exception.SigningException;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

public class PdfSigner {

    private final DocumentSigner documentSigner;

    public PdfSigner() {
        this.documentSigner = new DocumentSigner();
    }

    /**
     * Sign a PDF file and save the signature to a .p7s file.
     * (Original method for GUI compatibility)
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
     * (New method for API usage - no file paths needed)
     *
     * @param pdfBytes The PDF document as byte array
     * @param certBytes The PFX/PKCS12 certificate as byte array
     * @param password The certificate password
     * @return The P7S signature as byte array
     * @throws SigningException if signing fails
     */
    public byte[] signPdfBytes(byte[] pdfBytes, byte[] certBytes, String password) {
        // Validate inputs
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
            // Validate PDF format
            PDDocument document = PDDocument.load(pdfBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            document.close();

            // Validate certificate format and password
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try {
                keystore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            } catch (java.io.IOException e) {
                if (e.getCause() instanceof java.security.UnrecoverableKeyException) {
                    throw new InvalidPasswordException("Incorrect certificate password", e);
                }
                throw new InvalidCertificateException("Invalid certificate format", e);
            }

            // Check certificate expiry
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

            // Sign the document
            return documentSigner.signDocumentWithCertBytes(baos.toByteArray(), certBytes, password);

        } catch (InvalidDocumentException | InvalidCertificateException | InvalidPasswordException | ExpiredCertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new SigningException("Failed to sign document: " + e.getMessage(), e);
        }
    }

    /**
     * Verify a signature against the original document.
     *
     * @param signatureBytes The P7S signature bytes
     * @param originalPdfBytes The original PDF bytes
     * @return true if signature is valid
     * @throws SigningException if verification fails
     */
    public boolean verifySignature(byte[] signatureBytes, byte[] originalPdfBytes) {
        try {
            // Load and normalize the PDF
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
