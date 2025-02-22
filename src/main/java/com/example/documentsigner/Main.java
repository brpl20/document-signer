package com.example.documentsigner;

public class Main {
    public static void main(String[] args) {
        try {
            String inputPdf = "/Users/brpl20/code/signer/document-signer/CS.pdf";
            String outputP7s = "/Users/brpl20/code/signer/document-signer/CS.p7s";
            String pfxPath = "/Users/brpl20/code/signer/document-signer/bp.pfx";
            String password = "12345678";

            PdfSigner signer = new PdfSigner();
            signer.signPdf(inputPdf, outputP7s, pfxPath, password);
            
            System.out.println("P7S file created successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}