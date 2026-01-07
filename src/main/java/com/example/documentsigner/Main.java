package com.example.documentsigner;

import com.example.documentsigner.api.ApiApplication;
import org.springframework.boot.SpringApplication;

public class Main {

    public static void main(String[] args) {
        if (hasFlag(args, "--api") || hasFlag(args, "-a")) {
            // Start API server mode
            System.out.println("Starting Document Signer API...");
            SpringApplication.run(ApiApplication.class, args);
        } else if (hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printHelp();
        } else {
            // Default: Start GUI mode
            try {
                DocumentSignerUI.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        System.out.println("ProcStudio Document Signer");
        System.out.println("");
        System.out.println("Usage: java -jar document-signer.jar [options]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  (no options)    Start GUI mode (default)");
        System.out.println("  --api, -a       Start API server mode (port 8080)");
        System.out.println("  --help, -h      Show this help message");
        System.out.println("");
        System.out.println("API Endpoints (when running in API mode):");
        System.out.println("  GET  /api/v1/health       Health check");
        System.out.println("  POST /api/v1/sign         Sign a PDF document");
        System.out.println("  POST /api/v1/sign/batch   Sign multiple PDFs");
        System.out.println("  POST /api/v1/verify       Verify a signature");
        System.out.println("");
        System.out.println("Example API usage:");
        System.out.println("  curl -X POST http://localhost:8080/api/v1/sign \\");
        System.out.println("    -F \"document=@document.pdf\" \\");
        System.out.println("    -F \"certificate=@certificate.pfx\" \\");
        System.out.println("    -F \"password=your_password\" \\");
        System.out.println("    -o document.pdf.p7s");
    }
}
