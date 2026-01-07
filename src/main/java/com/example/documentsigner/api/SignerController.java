package com.example.documentsigner.api;

import com.example.documentsigner.api.dto.ErrorResponse;
import com.example.documentsigner.api.dto.SignResponse;
import com.example.documentsigner.api.dto.VerifyResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SignerController {

    private final SigningService signingService;

    public SignerController(SigningService signingService) {
        this.signingService = signingService;
    }

    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok().body(new Object() {
            public final String status = "ok";
            public final String service = "document-signer";
            public final String timestamp = Instant.now().toString();
        });
    }

    @PostMapping("/sign")
    public ResponseEntity<?> signDocument(
            @RequestParam("document") MultipartFile document,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam("password") String password) {

        try {
            byte[] pdfBytes = document.getBytes();
            byte[] certBytes = certificate.getBytes();

            byte[] signature = signingService.signDocument(pdfBytes, certBytes, password);

            String originalFilename = document.getOriginalFilename();
            String outputFilename = (originalFilename != null ? originalFilename : "document") + ".p7s";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", outputFilename);
            headers.setContentLength(signature.length);

            return new ResponseEntity<>(signature, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to read uploaded files", "FILE_READ_ERROR"));
        }
    }

    @PostMapping("/sign/json")
    public ResponseEntity<?> signDocumentJson(
            @RequestParam("document") MultipartFile document,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam("password") String password) {

        try {
            byte[] pdfBytes = document.getBytes();
            byte[] certBytes = certificate.getBytes();

            byte[] signature = signingService.signDocument(pdfBytes, certBytes, password);

            String originalFilename = document.getOriginalFilename();

            SignResponse response = new SignResponse(
                    true,
                    java.util.Base64.getEncoder().encodeToString(signature),
                    originalFilename,
                    Instant.now().toString()
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to read uploaded files", "FILE_READ_ERROR"));
        }
    }

    @PostMapping("/sign/batch")
    public ResponseEntity<?> signBatch(
            @RequestParam("documents") MultipartFile[] documents,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam("password") String password) {

        try {
            byte[] certBytes = certificate.getBytes();
            List<SignResponse> results = new ArrayList<>();

            for (MultipartFile document : documents) {
                try {
                    byte[] pdfBytes = document.getBytes();
                    byte[] signature = signingService.signDocument(pdfBytes, certBytes, password);

                    results.add(new SignResponse(
                            true,
                            java.util.Base64.getEncoder().encodeToString(signature),
                            document.getOriginalFilename(),
                            Instant.now().toString()
                    ));
                } catch (Exception e) {
                    results.add(new SignResponse(
                            false,
                            null,
                            document.getOriginalFilename(),
                            e.getMessage()
                    ));
                }
            }

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final List<SignResponse> documents = results;
                public final int total = results.size();
                public final long signed = results.stream().filter(r -> r.success).count();
            });

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to read certificate", "FILE_READ_ERROR"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifySignature(
            @RequestParam("document") MultipartFile document,
            @RequestParam("signature") MultipartFile signature) {

        try {
            byte[] pdfBytes = document.getBytes();
            byte[] signatureBytes = signature.getBytes();

            boolean isValid = signingService.verifySignature(signatureBytes, pdfBytes);

            return ResponseEntity.ok(new VerifyResponse(isValid, document.getOriginalFilename()));

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to read uploaded files", "FILE_READ_ERROR"));
        }
    }
}
