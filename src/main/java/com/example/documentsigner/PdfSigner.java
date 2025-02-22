package com.example.documentsigner;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class PdfSigner {
    public void signPdf(String inputPdf, String outputP7s, String pfxPath, String pfxPassword) {
        try {
            // Read PDF file
            PDDocument document = PDDocument.load(new File(inputPdf));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            document.close();
            
            // Sign the PDF bytes
            DocumentSigner signer = new DocumentSigner();
            byte[] signedData = signer.signDocument(baos.toByteArray(), pfxPath, pfxPassword);
            
            // Save as P7S
            FileOutputStream fos = new FileOutputStream(outputP7s);
            fos.write(signedData);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}