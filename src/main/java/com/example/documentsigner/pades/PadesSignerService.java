package com.example.documentsigner.pades;

import com.example.documentsigner.exception.ExpiredCertificateException;
import com.example.documentsigner.exception.InvalidCertificateException;
import com.example.documentsigner.exception.InvalidDocumentException;
import com.example.documentsigner.exception.InvalidPasswordException;
import com.example.documentsigner.exception.SigningException;
import com.example.documentsigner.pades.dto.PdfVerificationResult;
import com.example.documentsigner.pades.dto.SignatureMetadata;
import com.example.documentsigner.pades.dto.SignaturePosition;
import com.example.documentsigner.pades.dto.SignerDisplayInfo;
import com.example.documentsigner.pades.dto.VisualSignatureConfig;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.PDAppearanceContentStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core PAdES (PDF Advanced Electronic Signatures) signing service.
 *
 * This service implements PAdES-B (Basic) profile signatures conforming to
 * ETSI EN 319 142-1. Signatures are embedded directly into the PDF document
 * using the incremental update mechanism.
 *
 * Features:
 * - Invisible signatures (signature panel only)
 * - Visual signatures with signer information
 * - Certificate chain embedding
 * - SHA-256 with RSA signature algorithm
 * - SubFilter: ETSI.CAdES.detached
 */
public class PadesSignerService {

    // Preferred signature container size (32KB should be enough for most signatures with chain)
    private static final int PREFERRED_SIGNATURE_SIZE = 32768;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Sign PDF with invisible signature (PAdES-B).
     *
     * @param pdfBytes The PDF document bytes
     * @param certBytes The PFX/PKCS12 certificate bytes
     * @param password The certificate password
     * @param metadata Optional signature metadata (reason, location, contact)
     * @return Signed PDF bytes
     * @throws SigningException if signing fails
     */
    public byte[] signPdf(byte[] pdfBytes, byte[] certBytes, String password,
                          SignatureMetadata metadata) throws SigningException {
        validateInputs(pdfBytes, certBytes, password);

        try {
            // Load certificate and private key
            KeyStore keystore = loadKeyStore(certBytes, password);
            String alias = keystore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());
            Certificate[] certificateChain = keystore.getCertificateChain(alias);
            X509Certificate signingCert = (X509Certificate) certificateChain[0];

            // Validate certificate
            validateCertificate(signingCert);

            // Load PDF document
            PDDocument document = PDDocument.load(pdfBytes);

            try {
                // Create signature dictionary
                PDSignature signature = createSignature(signingCert, metadata);

                // Create signature interface
                PadesSignatureInterface signatureInterface =
                    new PadesSignatureInterface(privateKey, certificateChain);

                // Configure signature options
                SignatureOptions signatureOptions = new SignatureOptions();
                signatureOptions.setPreferredSignatureSize(PREFERRED_SIGNATURE_SIZE);

                // Add signature to document
                document.addSignature(signature, signatureInterface, signatureOptions);

                // Save incrementally
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                document.saveIncremental(output);

                return output.toByteArray();

            } finally {
                document.close();
            }

        } catch (InvalidDocumentException | InvalidCertificateException |
                 InvalidPasswordException | ExpiredCertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new SigningException("Failed to sign PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Sign PDF with visible signature.
     *
     * @param pdfBytes The PDF document bytes
     * @param certBytes The PFX/PKCS12 certificate bytes
     * @param password The certificate password
     * @param metadata Optional signature metadata
     * @param visualConfig Visual signature configuration
     * @return Signed PDF bytes
     * @throws SigningException if signing fails
     */
    public byte[] signPdfVisible(byte[] pdfBytes, byte[] certBytes, String password,
                                  SignatureMetadata metadata, VisualSignatureConfig visualConfig)
            throws SigningException {

        if (visualConfig == null || !visualConfig.isEnabled()) {
            return signPdf(pdfBytes, certBytes, password, metadata);
        }

        validateInputs(pdfBytes, certBytes, password);

        try {
            // Load certificate and private key
            KeyStore keystore = loadKeyStore(certBytes, password);
            String alias = keystore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());
            Certificate[] certificateChain = keystore.getCertificateChain(alias);
            X509Certificate signingCert = (X509Certificate) certificateChain[0];

            // Validate certificate
            validateCertificate(signingCert);

            // Extract signer info for visual appearance
            SignerDisplayInfo signerInfo = extractSignerInfo(signingCert);

            // Load PDF document
            PDDocument document = PDDocument.load(pdfBytes);

            try {
                // Validate page number
                int pageIndex = visualConfig.getPage() - 1;
                if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                    throw new InvalidDocumentException(
                        "Invalid page number: " + visualConfig.getPage() +
                        ". Document has " + document.getNumberOfPages() + " pages.");
                }

                // Create signature dictionary
                PDSignature signature = createSignature(signingCert, metadata);

                // Create signature interface
                PadesSignatureInterface signatureInterface =
                    new PadesSignatureInterface(privateKey, certificateChain);

                // Calculate signature rectangle position
                PDPage page = document.getPage(pageIndex);
                PDRectangle pageRect = page.getMediaBox();
                float width = visualConfig.getWidth();
                float height = visualConfig.getHeight();
                PDRectangle signatureRect = calculateSignatureRectangle(
                    pageRect, visualConfig, width, height);

                // Create signature options with proper rectangle
                SignatureOptions signatureOptions = new SignatureOptions();
                signatureOptions.setPreferredSignatureSize(PREFERRED_SIGNATURE_SIZE);
                signatureOptions.setPage(pageIndex);

                // Create the visual signature template
                byte[] visualTemplate = createVisualSignatureTemplate(
                    document, pageIndex, signatureRect, signerInfo);
                signatureOptions.setVisualSignature(new ByteArrayInputStream(visualTemplate));

                // Add signature to document
                document.addSignature(signature, signatureInterface, signatureOptions);

                // Save incrementally
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                document.saveIncremental(output);

                return output.toByteArray();

            } finally {
                document.close();
            }

        } catch (InvalidDocumentException | InvalidCertificateException |
                 InvalidPasswordException | ExpiredCertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new SigningException("Failed to sign PDF with visible signature: " + e.getMessage(), e);
        }
    }

    private PDRectangle calculateSignatureRectangle(PDRectangle pageRect,
                                                     VisualSignatureConfig config,
                                                     float width, float height) {
        float x, y;
        float margin = 20;

        if (config.getPosition() == SignaturePosition.CUSTOM &&
            config.getX() != null && config.getY() != null) {
            x = config.getX();
            y = config.getY();
        } else {
            switch (config.getPosition()) {
                case TOP_LEFT:
                    x = margin;
                    y = pageRect.getHeight() - height - margin;
                    break;
                case TOP_RIGHT:
                    x = pageRect.getWidth() - width - margin;
                    y = pageRect.getHeight() - height - margin;
                    break;
                case BOTTOM_LEFT:
                    x = margin;
                    y = margin;
                    break;
                case BOTTOM_RIGHT:
                default:
                    x = pageRect.getWidth() - width - margin;
                    y = margin;
                    break;
            }
        }
        return new PDRectangle(x, y, width, height);
    }

    private byte[] createVisualSignatureTemplate(PDDocument srcDoc, int pageIndex,
                                                  PDRectangle signatureRect,
                                                  SignerDisplayInfo signerInfo) throws IOException {
        // Create a new document for the template
        PDDocument template = new PDDocument();

        try {
            // BUG FIX: Create empty pages up to and including the target page index.
            // This ensures the signature widget is placed on the correct page when
            // PDFBox merges the template with the original document.
            // Previously, only one page was imported, causing signatures to always
            // appear on page 1 regardless of the selected page.
            for (int i = 0; i <= pageIndex; i++) {
                PDPage srcPage = srcDoc.getPage(i);
                PDPage newPage = new PDPage(srcPage.getMediaBox());
                template.addPage(newPage);
            }

            // Get the target page at the correct index
            PDPage templatePage = template.getPage(pageIndex);

            // Create AcroForm
            PDAcroForm acroForm = new PDAcroForm(template);
            template.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);

            // Create signature field
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("Signature1");

            // Get widget and set rectangle
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            widget.setRectangle(signatureRect);
            widget.setPage(templatePage);

            // Create appearance stream
            PDAppearanceStream appearanceStream = new PDAppearanceStream(template);
            appearanceStream.setResources(new PDResources());
            appearanceStream.setBBox(new PDRectangle(signatureRect.getWidth(), signatureRect.getHeight()));

            float w = signatureRect.getWidth();
            float h = signatureRect.getHeight();

            // Draw the signature content using PDAppearanceContentStream
            PDAppearanceContentStream cs = new PDAppearanceContentStream(appearanceStream);

            // Draw border
            cs.setStrokingColor(0, 0, 0);
            cs.setLineWidth(0.5f);
            cs.addRect(1, 1, w - 2, h - 2);
            cs.stroke();

            // Draw title
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            cs.newLineAtOffset(5, h - 14);
            cs.showText("ASSINADO DIGITALMENTE");
            cs.endText();

            // Draw separator line
            cs.moveTo(5, h - 18);
            cs.lineTo(w - 5, h - 18);
            cs.stroke();

            // Draw content
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 7);
            cs.setLeading(10);
            cs.newLineAtOffset(5, h - 30);

            // Name
            String name = signerInfo.getName() != null ? signerInfo.getName() : "N/A";
            if (name.length() > 35) {
                name = name.substring(0, 32) + "...";
            }
            cs.showText("Nome: " + name);
            cs.newLine();

            // CPF (masked)
            String cpf = signerInfo.getMaskedCpf();
            if (cpf != null) {
                cs.showText("CPF: " + cpf);
                cs.newLine();
            }

            // Issuer CA
            String issuer = signerInfo.getIssuerCA() != null ? signerInfo.getIssuerCA() : "N/A";
            if (issuer.length() > 35) {
                issuer = issuer.substring(0, 32) + "...";
            }
            cs.showText("AC: " + issuer);
            cs.newLine();

            // Date/Time
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateStr = sdf.format(signerInfo.getSigningTime());
            cs.showText("Data: " + dateStr);

            cs.endText();
            cs.close();

            // Set appearance
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);

            // Add field to form and annotations to page
            acroForm.getFields().add(signatureField);
            templatePage.getAnnotations().add(widget);

            // Save template to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            template.save(baos);
            return baos.toByteArray();

        } finally {
            template.close();
        }
    }

    /**
     * Verify embedded PDF signature.
     *
     * @param signedPdfBytes The signed PDF bytes
     * @return Verification result
     * @throws SigningException if verification fails
     */
    public PdfVerificationResult verifyPdfSignature(byte[] signedPdfBytes) throws SigningException {
        try {
            PDDocument document = PDDocument.load(signedPdfBytes);

            try {
                List<PDSignature> signatures = document.getSignatureDictionaries();

                if (signatures.isEmpty()) {
                    return PdfVerificationResult.builder()
                        .valid(false)
                        .details("No signatures found in document")
                        .build();
                }

                // Verify the last (most recent) signature
                PDSignature signature = signatures.get(signatures.size() - 1);

                // Extract signature content
                byte[] signatureContent = signature.getContents(signedPdfBytes);
                byte[] signedContent = signature.getSignedContent(signedPdfBytes);

                // Parse CMS signature
                CMSSignedData cms = new CMSSignedData(
                    new CMSProcessableByteArray(signedContent),
                    signatureContent
                );

                Store<X509CertificateHolder> certStore = cms.getCertificates();
                SignerInformationStore signers = cms.getSignerInfos();

                boolean valid = true;
                String signerName = null;

                for (SignerInformation signer : signers.getSigners()) {
                    Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
                    if (!certCollection.isEmpty()) {
                        X509CertificateHolder certHolder = certCollection.iterator().next();
                        X509Certificate cert = new JcaX509CertificateConverter()
                            .setProvider("BC")
                            .getCertificate(certHolder);

                        signerName = extractCN(cert);

                        // Verify signature
                        if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider("BC")
                                .build(cert))) {
                            valid = false;
                        }
                    }
                }

                // Check if signature covers whole document
                int[] byteRange = signature.getByteRange();
                boolean coversWholeDocument = false;
                if (byteRange != null && byteRange.length == 4) {
                    int lastByte = byteRange[2] + byteRange[3];
                    coversWholeDocument = lastByte >= signedPdfBytes.length - 1;
                }

                return PdfVerificationResult.builder()
                    .valid(valid)
                    .signerName(signerName)
                    .signingTime(signature.getSignDate() != null ?
                        signature.getSignDate().getTime() : null)
                    .reason(signature.getReason())
                    .integrityValid(valid)
                    .certificateValid(true)
                    .coversWholeDocument(coversWholeDocument)
                    .details(valid ? "Signature is valid" : "Signature verification failed")
                    .build();

            } finally {
                document.close();
            }

        } catch (Exception e) {
            throw new SigningException("Failed to verify PDF signature: " + e.getMessage(), e);
        }
    }

    /**
     * Extract signer information from certificate for visual display.
     */
    public SignerDisplayInfo extractSignerInfo(X509Certificate cert) {
        SignerDisplayInfo info = new SignerDisplayInfo();
        info.setSigningTime(new Date());

        // Extract CN (name)
        info.setName(extractCN(cert));

        // Extract CPF from ICP-Brasil certificate
        info.setCpf(extractCpf(cert));

        // Extract organization
        info.setOrganization(extractAttribute(cert.getSubjectX500Principal().getName(), "O"));

        // Extract issuer CA name
        info.setIssuerCA(extractCN(cert.getIssuerX500Principal().getName()));

        return info;
    }

    // ==================== Private Helper Methods ====================

    private void validateInputs(byte[] pdfBytes, byte[] certBytes, String password) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidDocumentException("PDF document is empty or null");
        }
        if (certBytes == null || certBytes.length == 0) {
            throw new InvalidCertificateException("Certificate is empty or null");
        }
        if (password == null || password.isEmpty()) {
            throw new InvalidPasswordException("Password is required");
        }

        // Validate PDF format
        try {
            PDDocument doc = PDDocument.load(pdfBytes);
            doc.close();
        } catch (IOException e) {
            throw new InvalidDocumentException("Invalid PDF format: " + e.getMessage(), e);
        }
    }

    private KeyStore loadKeyStore(byte[] certBytes, String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try {
            keystore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
        } catch (IOException e) {
            if (e.getCause() instanceof java.security.UnrecoverableKeyException) {
                throw new InvalidPasswordException("Incorrect certificate password", e);
            }
            throw new InvalidCertificateException("Invalid certificate format", e);
        }
        return keystore;
    }

    private void validateCertificate(X509Certificate cert) {
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
    }

    private PDSignature createSignature(X509Certificate cert, SignatureMetadata metadata) {
        PDSignature signature = new PDSignature();

        // Set filter and subfilter for PAdES-B
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);

        // Set signer name from certificate CN
        signature.setName(extractCN(cert));

        // Set signing time
        signature.setSignDate(Calendar.getInstance());

        // Set optional metadata
        if (metadata != null) {
            if (metadata.getReason() != null && !metadata.getReason().isEmpty()) {
                signature.setReason(metadata.getReason());
            }
            if (metadata.getLocation() != null && !metadata.getLocation().isEmpty()) {
                signature.setLocation(metadata.getLocation());
            }
            if (metadata.getContactInfo() != null && !metadata.getContactInfo().isEmpty()) {
                signature.setContactInfo(metadata.getContactInfo());
            }
        }

        return signature;
    }

    private String extractCN(X509Certificate cert) {
        return extractCN(cert.getSubjectX500Principal().getName());
    }

    private String extractCN(String dn) {
        return extractAttribute(dn, "CN");
    }

    private String extractAttribute(String dn, String attribute) {
        Pattern pattern = Pattern.compile(attribute + "=([^,]+)");
        Matcher matcher = pattern.matcher(dn);
        if (matcher.find()) {
            String value = matcher.group(1);
            // Handle quoted values and colons (ICP-Brasil format)
            if (value.contains(":")) {
                value = value.split(":")[0];
            }
            return value.trim();
        }
        return null;
    }

    /**
     * Extract CPF from ICP-Brasil certificate.
     *
     * ICP-Brasil certificates may have CPF in:
     * 1. CN field as "NAME:CPF" format
     * 2. OID 2.16.76.1.3.1 extension
     */
    private String extractCpf(X509Certificate cert) {
        String subject = cert.getSubjectX500Principal().getName();

        // Try CN format: "NAME:CPF"
        Pattern cnPattern = Pattern.compile("CN=([^:,]+):?(\\d{11})?");
        Matcher cnMatcher = cnPattern.matcher(subject);
        if (cnMatcher.find() && cnMatcher.group(2) != null) {
            return cnMatcher.group(2);
        }

        // Try to find 11-digit number in subject (common ICP-Brasil pattern)
        Pattern cpfPattern = Pattern.compile("(\\d{11})");
        Matcher cpfMatcher = cpfPattern.matcher(subject);
        if (cpfMatcher.find()) {
            return cpfMatcher.group(1);
        }

        // Try OID 2.16.76.1.3.1 extension
        try {
            byte[] extValue = cert.getExtensionValue("2.16.76.1.3.1");
            if (extValue != null && extValue.length > 0) {
                String value = new String(extValue, "ISO-8859-1");
                // CPF is usually at positions 8-18 in this OID
                if (value.length() >= 19) {
                    String possibleCpf = value.substring(8, 19).replaceAll("[^0-9]", "");
                    if (possibleCpf.length() == 11) {
                        return possibleCpf;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore extension parsing errors
        }

        return null;
    }
}
