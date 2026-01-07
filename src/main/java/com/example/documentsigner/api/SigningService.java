package com.example.documentsigner.api;

import com.example.documentsigner.PdfSigner;
import org.springframework.stereotype.Service;

@Service
public class SigningService {

    private final PdfSigner pdfSigner;

    public SigningService() {
        this.pdfSigner = new PdfSigner();
    }

    /**
     * Sign a PDF document with a certificate.
     *
     * @param pdfBytes The PDF document bytes
     * @param certBytes The PFX certificate bytes
     * @param password The certificate password
     * @return The P7S signature bytes
     */
    public byte[] signDocument(byte[] pdfBytes, byte[] certBytes, String password) {
        return pdfSigner.signPdfBytes(pdfBytes, certBytes, password);
    }

    /**
     * Verify a signature against the original document.
     *
     * @param signatureBytes The P7S signature bytes
     * @param originalPdfBytes The original PDF bytes
     * @return true if signature is valid
     */
    public boolean verifySignature(byte[] signatureBytes, byte[] originalPdfBytes) {
        return pdfSigner.verifySignature(signatureBytes, originalPdfBytes);
    }
}
