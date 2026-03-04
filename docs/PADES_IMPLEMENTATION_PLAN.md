# PAdES Implementation Plan

## Document Signer Evolution: From CMS/PKCS#7 (.p7s) to PAdES (Embedded PDF Signature)

**Version:** 1.0
**Branch:** `feature/pades-implementation`
**Created:** 2026-01-12
**Status:** Planning Phase

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [Target State: PAdES](#3-target-state-pades)
4. [Technical Architecture](#4-technical-architecture)
5. [Implementation Phases](#5-implementation-phases)
6. [New Components](#6-new-components)
7. [Modified Components](#7-modified-components)
8. [API Specification](#8-api-specification)
9. [GUI Specification](#9-gui-specification)
10. [Visual Signature Specification](#10-visual-signature-specification)
11. [Dependencies](#11-dependencies)
12. [Testing Strategy](#12-testing-strategy)
13. [Risk Analysis](#13-risk-analysis)
14. [Compatibility Matrix](#14-compatibility-matrix)
15. [Rollout Strategy](#15-rollout-strategy)
16. [References](#16-references)

---

## 1. Executive Summary

### Problem Statement

The current document-signer generates **detached CMS/PKCS#7 signatures (.p7s files)**, which:
- Require two files (PDF + .p7s) for validation
- Are not accepted by many systems (e.g., PJe - Processo Judicial Eletrônico)
- Don't display signature information when opening the PDF in Adobe Acrobat
- Require specialized knowledge to verify

### Solution

Implement **PAdES (PDF Advanced Electronic Signatures)** to generate:
- A **single PDF file** with the signature embedded
- Visual signature appearance showing signer information
- Native validation in Adobe Acrobat Reader
- Compliance with ICP-Brasil and international standards (ETSI EN 319 142)

### Key Benefits

| Aspect | Current (.p7s) | PAdES (PDF) |
|--------|---------------|-------------|
| Files generated | 2 (PDF + .p7s) | 1 (signed PDF) |
| Adobe Acrobat | Manual validation | Native signature panel |
| PJe compatibility | No | Yes |
| User experience | Technical | Intuitive |
| Visual signature | Not possible | Supported |

---

## 2. Current State Analysis

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CURRENT ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐   │
│   │    GUI      │     │     API     │     │   CLI (future)  │   │
│   │  (Swing)    │     │  (Spring)   │     │                 │   │
│   └──────┬──────┘     └──────┬──────┘     └────────┬────────┘   │
│          │                   │                      │            │
│          └───────────────────┼──────────────────────┘            │
│                              │                                   │
│                    ┌─────────▼─────────┐                         │
│                    │   SigningService  │                         │
│                    └─────────┬─────────┘                         │
│                              │                                   │
│          ┌───────────────────┼───────────────────┐               │
│          │                   │                   │               │
│   ┌──────▼──────┐    ┌───────▼───────┐   ┌──────▼──────┐        │
│   │  PdfSigner  │    │ CertValidator │   │ItiVerificador│       │
│   └──────┬──────┘    └───────────────┘   └─────────────┘        │
│          │                                                       │
│   ┌──────▼──────┐                                                │
│   │DocumentSigner│  ← CMS/PKCS#7 Generation                      │
│   │(BouncyCastle)│                                               │
│   └─────────────┘                                                │
│                                                                  │
│   OUTPUT: document.pdf + document.pdf.p7s (DETACHED)             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Current Signing Flow

```
1. Load PDF (PDFBox)
   └─► PDDocument.load(pdfBytes)

2. Normalize PDF
   └─► document.save(ByteArrayOutputStream)

3. Load Certificate (PKCS#12)
   └─► KeyStore.getInstance("PKCS12")
   └─► keystore.load(stream, password)
   └─► Extract PrivateKey + X509Certificate[]

4. Generate CMS Signature (BouncyCastle)
   └─► CMSSignedDataGenerator
   └─► JcaContentSignerBuilder("SHA256withRSA")
   └─► CMSProcessableByteArray(pdfBytes)
   └─► cmsGenerator.generate(cmsData, true)

5. Output
   └─► Write signature to .p7s file
   └─► Original PDF unchanged
```

### 2.3 Current File Inventory

| File | Lines | Purpose | PAdES Impact |
|------|-------|---------|--------------|
| `DocumentSigner.java` | 98 | CMS generation | Major changes |
| `PdfSigner.java` | 135 | PDF orchestration | Major changes |
| `CertificateValidator.java` | 252 | Certificate ops | Reuse as-is |
| `SignerController.java` | 305 | REST endpoints | Add new endpoints |
| `SigningService.java` | 149 | Business logic | Add PAdES methods |
| `DocumentSignerUI.java` | 557 | Swing GUI | Add format selection |
| `ItiVerificador.java` | 292 | ITI integration | Extend for PDF |

---

## 3. Target State: PAdES

### 3.1 What is PAdES?

**PAdES (PDF Advanced Electronic Signatures)** is defined by ETSI EN 319 142-1 and specifies how to embed CAdES (CMS Advanced Electronic Signatures) into PDF documents.

### 3.2 PAdES Profiles

| Profile | Description | Use Case |
|---------|-------------|----------|
| **PAdES-B** (Basic) | Basic signature with signing certificate | Standard signing |
| **PAdES-T** | Adds timestamp from TSA | Proof of time |
| **PAdES-LT** | Adds validation data (CRLs, OCSP) | Long-term archival |
| **PAdES-LTA** | Adds archive timestamp | Very long-term |

**Initial Implementation Target: PAdES-B with optional PAdES-T**

### 3.3 Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        TARGET ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐   │
│   │    GUI      │     │     API     │     │   CLI (future)  │   │
│   │  (Swing)    │     │  (Spring)   │     │                 │   │
│   └──────┬──────┘     └──────┬──────┘     └────────┬────────┘   │
│          │                   │                      │            │
│          └───────────────────┼──────────────────────┘            │
│                              │                                   │
│                    ┌─────────▼─────────┐                         │
│                    │   SigningService  │                         │
│                    └─────────┬─────────┘                         │
│                              │                                   │
│          ┌───────────────────┼───────────────────┐               │
│          │                   │                   │               │
│   ┌──────▼──────┐    ┌───────▼───────┐   ┌──────▼──────┐        │
│   │  PdfSigner  │    │ CertValidator │   │ItiVerificador│       │
│   └──────┬──────┘    └───────────────┘   └─────────────┘        │
│          │                                                       │
│   ┌──────┴─────────────────────┐                                 │
│   │                            │                                 │
│   ▼                            ▼                                 │
│ ┌─────────────┐     ┌───────────────────┐                        │
│ │DocumentSigner│     │  PadesSignerService │  ◄── NEW           │
│ │(CMS/.p7s)   │     │  (PDFBox + BC)       │                     │
│ └─────────────┘     └───────────────────┘                        │
│                               │                                  │
│                     ┌─────────┴─────────┐                        │
│                     │                   │                        │
│                     ▼                   ▼                        │
│           ┌─────────────┐     ┌─────────────────┐                │
│           │  Invisible  │     │VisibleSignature │  ◄── NEW      │
│           │  Signature  │     │    Renderer     │                │
│           └─────────────┘     └─────────────────┘                │
│                                                                  │
│   OUTPUT OPTIONS:                                                │
│   ├─► document.pdf.p7s (CMS detached - LEGACY)                   │
│   └─► document_signed.pdf (PAdES embedded - NEW)                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.4 PAdES Signing Flow (Target)

```
1. Load PDF (PDFBox)
   └─► PDDocument.load(pdfBytes)

2. Create Signature Dictionary
   └─► PDSignature object
   └─► Filter: Adobe.PPKLite
   └─► SubFilter: ETSI.CAdES.detached
   └─► Set Name, Location, Reason, ContactInfo, SignDate

3. (Optional) Create Visual Appearance
   └─► PDVisibleSignDesigner
   └─► Position: page, x, y, width, height
   └─► Content: Name, CPF, CA, DateTime, "Assinado Digitalmente"

4. Register Signature with Document
   └─► document.addSignature(signature, signatureInterface)

5. Calculate ByteRange
   └─► PDFBox handles this internally
   └─► Excludes /Contents placeholder from hash

6. Generate CMS (in SignatureInterface callback)
   └─► Receive InputStream of bytes to sign
   └─► Create CMSSignedData (detached mode)
   └─► Return CMS bytes

7. Save Incrementally
   └─► document.saveIncremental(outputStream)
   └─► CMS embedded in /Contents

8. Output
   └─► Single signed PDF file
```

---

## 4. Technical Architecture

### 4.1 PDF Signature Structure

```
PDF File Structure (Simplified)
├── %PDF-1.7
├── Objects (document content)
├── Catalog
│   └── AcroForm
│       └── Fields
│           └── Signature Field (Widget Annotation)
│               ├── /FT /Sig
│               ├── /V (Signature Dictionary)
│               │   ├── /Type /Sig
│               │   ├── /Filter /Adobe.PPKLite
│               │   ├── /SubFilter /ETSI.CAdES.detached
│               │   ├── /Name (Signer Name)
│               │   ├── /M (Signing Time)
│               │   ├── /Reason (Reason for signing)
│               │   ├── /Location (Signing location)
│               │   ├── /ContactInfo (Contact info)
│               │   ├── /ByteRange [0 offset1 offset2 length]
│               │   └── /Contents <CMS_SIGNATURE_HEX>
│               └── /AP (Appearance Dictionary - visual)
│                   └── /N (Normal appearance stream)
├── XRef Table
└── %%EOF
```

### 4.2 SubFilter Options

| SubFilter | Standard | Acrobat Support | Recommendation |
|-----------|----------|-----------------|----------------|
| `adbe.pkcs7.detached` | PDF 1.3+ | Full | Legacy systems |
| `adbe.pkcs7.sha1` | PDF 1.3+ | Full | Deprecated |
| `ETSI.CAdES.detached` | PAdES | Full | **Recommended** |
| `ETSI.RFC3161` | PAdES-T | Full | For timestamps |

**Decision: Use `ETSI.CAdES.detached` for PAdES compliance**

### 4.3 CMS Structure for PAdES

```
CMS SignedData (RFC 5652)
├── version: 1
├── digestAlgorithms: {SHA-256}
├── encapContentInfo
│   ├── eContentType: id-data
│   └── eContent: ABSENT (detached mode)
├── certificates: [signerCert, intermediateCerts...]
├── crls: OPTIONAL
└── signerInfos
    └── SignerInfo
        ├── version: 1
        ├── sid: IssuerAndSerialNumber
        ├── digestAlgorithm: SHA-256
        ├── signedAttrs
        │   ├── contentType: id-data
        │   ├── messageDigest: <hash of PDF ByteRange>
        │   ├── signingTime: UTCTime
        │   └── signingCertificateV2 (ESS)
        ├── signatureAlgorithm: SHA256withRSA
        └── signature: <encrypted hash>
```

---

## 5. Implementation Phases

### Phase 1: Core PAdES Signing (MVP)
**Goal:** Generate valid signed PDF with invisible signature
**Duration Estimate:** Development time (no calendar estimates)

#### Deliverables:
- [ ] `PadesSignerService.java` - Core PAdES signing logic
- [ ] `PadesSignatureInterface.java` - PDFBox SignatureInterface implementation
- [ ] Invisible signature generation
- [ ] Acrobat validation (signature panel visible)
- [ ] Unit tests for signing flow

#### Acceptance Criteria:
- [ ] Signed PDF opens in Adobe Acrobat Reader
- [ ] Signature appears in Acrobat signature panel
- [ ] Signature validates with ICP-Brasil certificate
- [ ] Original document content unchanged
- [ ] Certificate chain included in signature

---

### Phase 2: Visual Signature
**Goal:** Add visual signature appearance with signer information

#### Deliverables:
- [ ] `VisibleSignatureRenderer.java` - Appearance stream generator
- [ ] Visual appearance with required fields:
  - Nome (from CN)
  - CPF (from certificate subject)
  - Autoridade Certificadora (Issuer)
  - Data/Hora (Signing time)
  - Termo: "Assinado Digitalmente"
- [ ] Position configuration (page, coordinates)
- [ ] Font embedding for cross-platform compatibility

#### Visual Appearance Specification:
```
┌─────────────────────────────────────────┐
│  ╔═══════════════════════════════════╗  │
│  ║  ASSINADO DIGITALMENTE            ║  │
│  ║  ───────────────────────          ║  │
│  ║  Nome: João da Silva              ║  │
│  ║  CPF: ***.***.***-00              ║  │
│  ║  AC: AC SERASA RFB v5             ║  │
│  ║  Data: 12/01/2026 10:30:45        ║  │
│  ╚═══════════════════════════════════╝  │
└─────────────────────────────────────────┘
```

---

### Phase 3: API Integration
**Goal:** Expose PAdES signing via REST API

#### New Endpoints:
- [ ] `POST /api/v1/sign/pdf` - Return signed PDF binary
- [ ] `POST /api/v1/sign/pdf/json` - Return signed PDF as base64
- [ ] `POST /api/v1/sign/pdf/batch` - Batch signing (ZIP output)
- [ ] `POST /api/v1/sign/pdf/verified` - Sign and verify with ITI
- [ ] `POST /api/v1/verify/pdf` - Verify embedded PDF signature

#### Request Parameters:
```
Required:
  - document: MultipartFile (PDF)
  - certificate: MultipartFile (PFX/P12)
  - password: String

Optional:
  - format: String ("pades" | "p7s") [default: "pades"]
  - visible: Boolean [default: false]
  - reason: String
  - location: String
  - contact: String
  - page: Integer [default: 1]
  - position: String ("bottom-left" | "bottom-right" | "top-left" | "top-right" | "custom")
  - x: Integer (for custom position)
  - y: Integer (for custom position)
  - width: Integer [default: 200]
  - height: Integer [default: 80]
```

---

### Phase 4: GUI Integration
**Goal:** Add PAdES option to desktop application

#### GUI Changes:
- [ ] Output format selection (Radio buttons or ComboBox):
  - "PDF Assinado (PAdES) - Recomendado"
  - "Assinatura Destacada (.p7s)"
- [ ] Visual signature options panel:
  - Enable/disable checkbox
  - Page number input
  - Position preset dropdown
  - Preview button (optional)
- [ ] Progress indication during signing
- [ ] Success dialog with output path

#### Mockup:
```
┌──────────────────────────────────────────────────────────────┐
│  ProcStudio Signer                                    [─][□][×]│
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Certificado: [________________________] [Procurar]          │
│  Senha:       [________________________] [Verificar]         │
│                                                              │
│  ┌─ Formato de Saída ─────────────────────────────────────┐  │
│  │  ○ PDF Assinado (PAdES) - Recomendado                  │  │
│  │  ○ Assinatura Destacada (.p7s)                         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ Assinatura Visual (apenas PAdES) ─────────────────────┐  │
│  │  [✓] Incluir assinatura visual                         │  │
│  │      Página: [1 ▼]  Posição: [Rodapé direito ▼]        │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  [Assinar Arquivo] [Assinar Múltiplos] [Assinar Pasta]       │
│                                                              │
│  ┌─ Log ──────────────────────────────────────────────────┐  │
│  │ > Certificado carregado: João da Silva [Válido]        │  │
│  │ > Assinando: documento.pdf                              │  │
│  │ > Sucesso: documento_signed.pdf                         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

### Phase 5: Timestamp Support (PAdES-T)
**Goal:** Add trusted timestamp from TSA

#### Deliverables:
- [ ] TSA client implementation
- [ ] Timestamp token embedding in signature
- [ ] Configuration for TSA URL
- [ ] Fallback handling if TSA unavailable

#### TSA Options (ICP-Brasil):
- Serpro TSA
- Other accredited TSAs

---

### Phase 6: Validation & LTV (Future)
**Goal:** Add validation data for Long-Term Validation

#### Deliverables:
- [ ] CRL/OCSP retrieval
- [ ] Validation data embedding (DSS dictionary)
- [ ] PAdES-LT profile support

---

## 6. New Components

### 6.1 PadesSignerService.java

**Package:** `com.example.documentsigner.pades`

**Purpose:** Core PAdES signing logic using PDFBox

**Key Methods:**
```java
public class PadesSignerService {

    /**
     * Sign PDF with invisible signature (PAdES-B)
     */
    public byte[] signPdf(
        byte[] pdfBytes,
        byte[] certBytes,
        String password,
        SignatureMetadata metadata
    ) throws SigningException;

    /**
     * Sign PDF with visible signature
     */
    public byte[] signPdfVisible(
        byte[] pdfBytes,
        byte[] certBytes,
        String password,
        SignatureMetadata metadata,
        VisualSignatureConfig visualConfig
    ) throws SigningException;

    /**
     * Verify embedded PDF signature
     */
    public PdfVerificationResult verifyPdfSignature(
        byte[] signedPdfBytes
    ) throws SigningException;
}
```

### 6.2 PadesSignatureInterface.java

**Package:** `com.example.documentsigner.pades`

**Purpose:** PDFBox SignatureInterface implementation for CMS generation

**Implementation:**
```java
public class PadesSignatureInterface implements SignatureInterface {

    private final PrivateKey privateKey;
    private final Certificate[] certificateChain;

    @Override
    public byte[] sign(InputStream content) throws IOException {
        // 1. Read all bytes from content (ByteRange data)
        // 2. Calculate SHA-256 digest
        // 3. Create CMSSignedData with:
        //    - Digest algorithm: SHA-256
        //    - Signature algorithm: SHA256withRSA
        //    - Include signing certificate
        //    - Include certificate chain
        //    - Signed attributes (content-type, message-digest, signing-time, signing-cert-v2)
        // 4. Return DER-encoded CMS bytes
    }
}
```

### 6.3 VisibleSignatureRenderer.java

**Package:** `com.example.documentsigner.pades`

**Purpose:** Generate visual appearance for signature field

**Key Methods:**
```java
public class VisibleSignatureRenderer {

    /**
     * Create appearance stream with signer information
     */
    public PDRectangle createVisibleSignature(
        PDDocument document,
        PDSignature signature,
        VisualSignatureConfig config,
        CertificateInfo certInfo
    );

    /**
     * Extract display information from certificate
     */
    public SignerDisplayInfo extractSignerInfo(X509Certificate cert);
}

/**
 * Visual signature content
 */
public class SignerDisplayInfo {
    private String name;           // CN from subject
    private String cpf;            // Extracted from subject (e.g., OID 2.16.76.1.3.1)
    private String organization;   // O from subject
    private String issuerCA;       // CN from issuer
    private Date signingTime;
}
```

### 6.4 DTOs

**SignatureMetadata.java:**
```java
public class SignatureMetadata {
    private String reason;      // Reason for signing
    private String location;    // Signing location
    private String contactInfo; // Contact information
}
```

**VisualSignatureConfig.java:**
```java
public class VisualSignatureConfig {
    private boolean enabled;
    private int page;           // 1-indexed
    private SignaturePosition position;
    private Integer x;          // Custom X coordinate
    private Integer y;          // Custom Y coordinate
    private int width = 200;
    private int height = 80;
}

public enum SignaturePosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    CUSTOM
}
```

**PdfVerificationResult.java:**
```java
public class PdfVerificationResult {
    private boolean valid;
    private String signerName;
    private Date signingTime;
    private String reason;
    private boolean certificateValid;
    private boolean integrityValid;
    private String details;
}
```

---

## 7. Modified Components

### 7.1 PdfSigner.java

**Changes:**
- Add signature format parameter
- Delegate to PadesSignerService for PAdES
- Keep existing logic for .p7s

```java
public class PdfSigner {

    private final DocumentSigner cmsDocumentSigner;  // Existing
    private final PadesSignerService padesService;   // New

    public enum SignatureFormat {
        PADES,  // Embedded PDF signature
        CMS     // Detached .p7s
    }

    /**
     * Sign with format selection
     */
    public byte[] sign(
        byte[] pdfBytes,
        byte[] certBytes,
        String password,
        SignatureFormat format,
        SignatureMetadata metadata,
        VisualSignatureConfig visualConfig
    ) {
        if (format == SignatureFormat.PADES) {
            if (visualConfig != null && visualConfig.isEnabled()) {
                return padesService.signPdfVisible(...);
            } else {
                return padesService.signPdf(...);
            }
        } else {
            return cmsDocumentSigner.signDocument(...);
        }
    }
}
```

### 7.2 SigningService.java

**Changes:**
- Add PAdES signing methods
- Add format selection to existing methods

```java
@Service
public class SigningService {

    // Existing methods preserved...

    /**
     * Sign document and return signed PDF (PAdES)
     */
    public byte[] signDocumentPades(
        byte[] pdfBytes,
        byte[] certBytes,
        String password,
        SignatureMetadata metadata,
        VisualSignatureConfig visualConfig
    );

    /**
     * Sign and verify with ITI (PAdES)
     */
    public SignAndVerifyResult signPadesAndVerifyWithIti(
        byte[] pdfBytes,
        byte[] certBytes,
        String password,
        String documentFilename,
        boolean useStaging
    );
}
```

### 7.3 SignerController.java

**Changes:**
- Add new PAdES endpoints (see API Specification)
- Preserve existing endpoints for backward compatibility

### 7.4 DocumentSignerUI.java

**Changes:**
- Add format selection UI (Radio buttons)
- Add visual signature options panel
- Update signing logic to use selected format
- Update file naming for PAdES output (_signed.pdf vs .p7s)

---

## 8. API Specification

### 8.1 New Endpoints

#### POST /api/v1/sign/pdf

**Description:** Sign PDF and return signed PDF file (PAdES)

**Request:**
```
Content-Type: multipart/form-data

Required:
  document: File (PDF)
  certificate: File (PFX/P12)
  password: String

Optional:
  reason: String
  location: String
  contact: String
  visible: Boolean (default: false)
  page: Integer (default: 1)
  position: String (bottom-left|bottom-right|top-left|top-right|custom)
  x: Integer (required if position=custom)
  y: Integer (required if position=custom)
  width: Integer (default: 200)
  height: Integer (default: 80)
```

**Response (Success):**
```
HTTP 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="document_signed.pdf"

<binary PDF data>
```

**Response (Error):**
```json
HTTP 4xx/5xx
Content-Type: application/json

{
  "success": false,
  "error": "Error message",
  "code": "ERROR_CODE"
}
```

---

#### POST /api/v1/sign/pdf/json

**Description:** Sign PDF and return base64-encoded signed PDF

**Request:** Same as /api/v1/sign/pdf

**Response (Success):**
```json
HTTP 200 OK
Content-Type: application/json

{
  "success": true,
  "signed_pdf_base64": "JVBERi0xLj...",
  "filename": "document_signed.pdf",
  "original_filename": "document.pdf",
  "signature_info": {
    "signer_name": "João da Silva",
    "signing_time": "2026-01-12T10:30:45Z",
    "reason": "Acordo comercial",
    "visible_signature": true
  },
  "timestamp": "2026-01-12T10:30:45Z"
}
```

---

#### POST /api/v1/sign/pdf/batch

**Description:** Sign multiple PDFs and return ZIP archive

**Request:**
```
Content-Type: multipart/form-data

Required:
  documents: File[] (multiple PDFs)
  certificate: File (PFX/P12)
  password: String

Optional:
  (same as /api/v1/sign/pdf)
```

**Response (Success):**
```
HTTP 200 OK
Content-Type: application/zip
Content-Disposition: attachment; filename="signed_documents.zip"

<ZIP archive containing signed PDFs>
```

---

#### POST /api/v1/verify/pdf

**Description:** Verify embedded signature in PDF

**Request:**
```
Content-Type: multipart/form-data

Required:
  document: File (signed PDF)
```

**Response:**
```json
HTTP 200 OK
Content-Type: application/json

{
  "valid": true,
  "signatures": [
    {
      "signer_name": "João da Silva",
      "signing_time": "2026-01-12T10:30:45Z",
      "reason": "Acordo comercial",
      "certificate_valid": true,
      "integrity_valid": true,
      "covers_whole_document": true
    }
  ],
  "filename": "document_signed.pdf",
  "timestamp": "2026-01-12T10:30:50Z"
}
```

---

### 8.2 Updated Endpoints

#### POST /api/v1/sign (Modified)

**New Optional Parameter:**
```
format: String ("pades" | "p7s") [default: "p7s" for backward compatibility]
```

When `format=pades`:
- Returns signed PDF instead of .p7s
- Accepts visual signature parameters

---

## 9. GUI Specification

### 9.1 New UI Elements

#### Format Selection Panel
```java
// Radio button group for output format
JPanel formatPanel = new JPanel();
formatPanel.setBorder(BorderFactory.createTitledBorder("Formato de Saída"));

JRadioButton padesRadio = new JRadioButton("PDF Assinado (PAdES) - Recomendado");
JRadioButton p7sRadio = new JRadioButton("Assinatura Destacada (.p7s)");

ButtonGroup formatGroup = new ButtonGroup();
formatGroup.add(padesRadio);
formatGroup.add(p7sRadio);
padesRadio.setSelected(true);  // Default to PAdES
```

#### Visual Signature Options Panel
```java
JPanel visualPanel = new JPanel();
visualPanel.setBorder(BorderFactory.createTitledBorder("Assinatura Visual (apenas PAdES)"));

JCheckBox visualCheckbox = new JCheckBox("Incluir assinatura visual");
JLabel pageLabel = new JLabel("Página:");
JSpinner pageSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
JLabel positionLabel = new JLabel("Posição:");
JComboBox<String> positionCombo = new JComboBox<>(new String[]{
    "Rodapé esquerdo",
    "Rodapé direito",
    "Topo esquerdo",
    "Topo direito"
});

// Enable/disable based on format selection
padesRadio.addActionListener(e -> visualPanel.setEnabled(true));
p7sRadio.addActionListener(e -> visualPanel.setEnabled(false));
```

### 9.2 Preferences Persistence

Add new preferences keys:
```java
private static final String PREF_OUTPUT_FORMAT = "outputFormat";
private static final String PREF_VISUAL_SIGNATURE = "visualSignature";
private static final String PREF_VISUAL_PAGE = "visualPage";
private static final String PREF_VISUAL_POSITION = "visualPosition";
```

### 9.3 File Naming

| Format | Input | Output |
|--------|-------|--------|
| PAdES | document.pdf | document_signed.pdf |
| CMS | document.pdf | document.pdf.p7s |

---

## 10. Visual Signature Specification

### 10.1 Layout

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ASSINADO DIGITALMENTE                                │  │
│  │  ─────────────────────────────────────────────────    │  │
│  │                                                       │  │
│  │  Nome: [Nome completo do signatário]                  │  │
│  │  CPF: [***.***.***-XX]                                │  │
│  │  AC: [Nome da Autoridade Certificadora]               │  │
│  │  Data: [DD/MM/YYYY HH:MM:SS]                          │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Dimensions: 200 x 80 points (approximately 70 x 28 mm)
```

### 10.2 Content Extraction from Certificate

| Field | Source | OID/Method |
|-------|--------|------------|
| Nome | Subject CN | 2.5.4.3 |
| CPF | Subject (ICP-Brasil) | 2.16.76.1.3.1 or parse from CN |
| AC | Issuer CN | 2.5.4.3 |
| Data | Signing time | System time or TSA |

### 10.3 CPF Extraction Logic

ICP-Brasil certificates include CPF in subject. Common formats:
```
CN=JOAO DA SILVA:12345678900
or
OID 2.16.76.1.3.1 = 01011980123456789000000000000000
                     ^^^^^^^^           (birth date)
                             ^^^^^^^^^^^  (CPF)
```

**Extraction code:**
```java
public String extractCpf(X509Certificate cert) {
    String subject = cert.getSubjectX500Principal().getName();

    // Try CN format first: "NAME:CPF"
    Pattern cnPattern = Pattern.compile("CN=([^:]+):?(\\d{11})?");
    Matcher cnMatcher = cnPattern.matcher(subject);
    if (cnMatcher.find() && cnMatcher.group(2) != null) {
        return formatCpf(cnMatcher.group(2));
    }

    // Try OID 2.16.76.1.3.1
    try {
        byte[] extValue = cert.getExtensionValue("2.16.76.1.3.1");
        if (extValue != null) {
            // Parse and extract CPF from positions 8-18
            String value = new String(extValue);
            String cpf = value.substring(8, 19);
            return formatCpf(cpf);
        }
    } catch (Exception e) {
        // Ignore
    }

    return null;  // CPF not found
}

private String formatCpf(String cpf) {
    // Format: ***.***. ***-XX (masked for privacy)
    return "***.***.***-" + cpf.substring(9, 11);
}
```

### 10.4 Font Considerations

- Use PDF standard fonts (Helvetica) for maximum compatibility
- Or embed subset of Liberation Sans for better appearance
- Font size: 8-10pt for content, 10-12pt for header

---

## 11. Dependencies

### 11.1 Current Dependencies (pom.xml)

```xml
<!-- Already present -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.27</version>
</dependency>
```

### 11.2 Dependencies to Add

```xml
<!-- NONE REQUIRED for basic PAdES -->
<!-- PDFBox 2.0.27 + BouncyCastle 1.70 are sufficient -->

<!-- Optional: For advanced visual signature -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox-examples</artifactId>
    <version>2.0.27</version>
    <scope>provided</scope> <!-- Reference only -->
</dependency>

<!-- Optional: For timestamp (PAdES-T) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bctsp-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

### 11.3 Version Compatibility Matrix

| Component | Current | Compatible Range | Notes |
|-----------|---------|------------------|-------|
| Java | 1.8 | 1.8 - 17 | Keep 1.8 for compatibility |
| PDFBox | 2.0.27 | 2.0.x | Don't upgrade to 3.x yet |
| BouncyCastle | 1.70 | 1.68 - 1.77 | Stable API |
| Spring Boot | 2.7.18 | 2.7.x | Keep for Java 8 support |

---

## 12. Testing Strategy

### 12.1 Unit Tests

**PadesSignerServiceTest.java:**
```java
@Test
void signPdf_withValidInputs_returnsSignedPdf()

@Test
void signPdf_withInvalidPassword_throwsInvalidPasswordException()

@Test
void signPdf_withExpiredCertificate_throwsExpiredCertificateException()

@Test
void signPdfVisible_withValidConfig_includesVisualSignature()

@Test
void signPdf_signedPdf_canBeVerified()
```

**VisibleSignatureRendererTest.java:**
```java
@Test
void extractSignerInfo_fromIcpBrasilCert_extractsCpf()

@Test
void createVisibleSignature_onPage1_placesCorrectly()

@Test
void createVisibleSignature_atBottomRight_calculatesCoordinates()
```

### 12.2 Integration Tests

**SignerControllerIntegrationTest.java:**
```java
@Test
void signPdf_endpoint_returnsValidPdf()

@Test
void signPdfJson_endpoint_returnsBase64()

@Test
void verifyPdf_withValidSignature_returnsValid()

@Test
void signPdfBatch_withMultipleFiles_returnsZip()
```

### 12.3 Validation Tests

**External Validation Checklist:**
- [ ] Adobe Acrobat Reader DC - Signature visible in panel
- [ ] Adobe Acrobat Reader DC - "Signed and all signatures are valid" (with trusted root)
- [ ] ITI Verificador (Production) - Validation passes
- [ ] ITI Verificador (Staging) - Validation passes
- [ ] PJe - Document accepted for submission
- [ ] Foxit Reader - Signature recognized
- [ ] PDF.js (browser) - Signature recognized (limited)

### 12.4 Test Certificates

Create test certificates for automated testing:
```bash
# Generate test CA
openssl req -x509 -newkey rsa:2048 -keyout test-ca.key -out test-ca.crt -days 365 -nodes

# Generate test user certificate
openssl req -newkey rsa:2048 -keyout test-user.key -out test-user.csr -nodes
openssl x509 -req -in test-user.csr -CA test-ca.crt -CAkey test-ca.key -CAcreateserial -out test-user.crt -days 365

# Create PFX
openssl pkcs12 -export -out test-user.pfx -inkey test-user.key -in test-user.crt -certfile test-ca.crt
```

---

## 13. Risk Analysis

### 13.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| ByteRange calculation errors | Medium | High | Use PDFBox's built-in handling, extensive testing |
| CMS size exceeds placeholder | Medium | High | Reserve sufficient space (32KB default) |
| Font embedding issues | Low | Medium | Use standard PDF fonts |
| Certificate chain not included | Low | High | Explicitly include full chain |
| Visual signature position miscalculation | Medium | Low | Calculate from page dimensions |

### 13.2 Compatibility Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Acrobat doesn't validate | Low | Critical | Follow ETSI.CAdES.detached exactly |
| PJe rejects signature | Low | High | Test with PJe staging environment |
| ITI Verificador fails | Low | High | Test extensively before release |
| Older PDF viewers fail | Medium | Low | Document minimum viewer requirements |

### 13.3 Mitigation Strategies

1. **Incremental Development**: Start with invisible signature, validate, then add visual
2. **Reference Implementation**: Follow PDFBox examples exactly for signing structure
3. **External Testing**: Validate with Adobe, ITI before each phase release
4. **Fallback**: Keep .p7s as fallback option in case of issues

---

## 14. Compatibility Matrix

### 14.1 Target Systems

| System | Version | Expected Compatibility |
|--------|---------|----------------------|
| Adobe Acrobat Reader DC | Latest | Full |
| Adobe Acrobat Pro | Latest | Full |
| Foxit Reader | Latest | Full |
| PJe (Processo Judicial Eletrônico) | Current | Full |
| ITI Verificador | v2 | Full |
| e-SAJ | Current | To be verified |
| Projudi | Current | To be verified |

### 14.2 PDF Versions

| PDF Version | Support |
|-------------|---------|
| PDF 1.4 | Full |
| PDF 1.5 | Full |
| PDF 1.6 | Full |
| PDF 1.7 | Full |
| PDF 2.0 | Partial (needs testing) |

### 14.3 Certificate Types

| Type | Support |
|------|---------|
| ICP-Brasil A1 (software) | Full |
| ICP-Brasil A3 (token/smartcard) | Future enhancement |
| Non-ICP certificates | Basic (not validated with ITI) |

---

## 15. Rollout Strategy

### 15.1 Release Phases

```
Phase 1 (MVP)          Phase 2 (Visual)       Phase 3 (API)
──────────────         ─────────────────      ──────────────
   │                       │                      │
   ▼                       ▼                      ▼
┌─────────┐           ┌─────────┐           ┌─────────┐
│ v2.1.0  │───────────│ v2.2.0  │───────────│ v2.3.0  │
│ PAdES-B │           │ Visual  │           │ Full API │
│ Invisible│          │ Sig     │           │ + GUI   │
└─────────┘           └─────────┘           └─────────┘
     │                     │                     │
     ▼                     ▼                     ▼
  Alpha               Beta testing          Production
  Testing             with users            release
```

### 15.2 Version Naming

| Version | Content |
|---------|---------|
| v2.1.0-alpha | PAdES invisible signature (API only) |
| v2.1.0-beta | PAdES invisible + basic testing |
| v2.1.0 | PAdES invisible release |
| v2.2.0-alpha | Visual signature (API only) |
| v2.2.0-beta | Visual signature + GUI |
| v2.2.0 | Full visual signature release |
| v2.3.0 | Complete API + GUI integration |

### 15.3 Documentation Updates

- [ ] Update README.md with PAdES endpoints
- [ ] Add PADES_USAGE.md with examples
- [ ] Update API documentation
- [ ] Create migration guide from .p7s

---

## 16. References

### 16.1 Standards

- [ETSI EN 319 142-1](https://www.etsi.org/deliver/etsi_en/319100_319199/31914201/01.02.01_60/en_31914201v010201p.pdf) - PAdES Part 1: Building blocks and PAdES baseline signatures
- [ETSI EN 319 142-2](https://www.etsi.org/deliver/etsi_en/319100_319199/31914202/01.02.01_60/en_31914202v010201p.pdf) - PAdES Part 2: Extended signatures
- [PDF Reference 1.7](https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/PDF32000_2008.pdf) - PDF specification
- [RFC 5652](https://datatracker.ietf.org/doc/html/rfc5652) - CMS (Cryptographic Message Syntax)

### 16.2 Implementation References

- [PDFBox Signature Examples](https://pdfbox.apache.org/docs/2.0.27/javadocs/org/apache/pdfbox/examples/signature/package-summary.html)
- [PDFBox CreateVisibleSignature](https://pdfbox.apache.org/docs/2.0.5/javadocs/org/apache/pdfbox/examples/signature/CreateVisibleSignature.html)
- [BouncyCastle CMS Documentation](https://www.bouncycastle.org/docs/pkixdocs1.5on/org/bouncycastle/cms/package-summary.html)
- [Adobe Digital Signatures](https://www.adobe.com/devnet-docs/acrobatetk/tools/DigSigDC/standards.html)

### 16.3 Project Resources

- [GitHub Repository](https://github.com/brpl20/document-signer)
- [ITI Verificador](https://verificador.iti.gov.br)
- [ITI Verificador Developer Guide](https://validar.iti.gov.br/guia-desenvolvedor.html)

---

## Appendix A: PDFBox Signature Code Template

```java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

public class PadesSignatureTemplate implements SignatureInterface {

    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    public byte[] signPdf(byte[] pdfBytes, byte[] certBytes, String password) throws Exception {
        // Load certificate
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
        String alias = keystore.aliases().nextElement();
        privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());
        certificateChain = keystore.getCertificateChain(alias);

        // Load PDF
        PDDocument document = PDDocument.load(pdfBytes);

        // Create signature dictionary
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
        signature.setName(extractCN((X509Certificate) certificateChain[0]));
        signature.setSignDate(Calendar.getInstance());

        // Register signature
        document.addSignature(signature, this);

        // Save incrementally
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.saveIncremental(output);
        document.close();

        return output.toByteArray();
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            // Read content to sign
            byte[] contentBytes = content.readAllBytes();

            // Create CMS signature
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

            generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder()
                        .setProvider("BC")
                        .build())
                .build(signer, (X509Certificate) certificateChain[0]));

            generator.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));

            // Generate detached CMS
            CMSSignedData signedData = generator.generate(
                new CMSProcessableByteArray(contentBytes),
                false  // detached = false content not encapsulated
            );

            return signedData.getEncoded();

        } catch (Exception e) {
            throw new IOException("Signing failed", e);
        }
    }

    private String extractCN(X509Certificate cert) {
        String dn = cert.getSubjectX500Principal().getName();
        for (String part : dn.split(",")) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return dn;
    }
}
```

---

## Appendix B: Visual Signature Code Template

```java
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.awt.geom.Rectangle2D;
import java.io.*;

public class VisibleSignatureTemplate {

    public void addVisibleSignature(
        PDDocument document,
        PDSignature signature,
        int pageNumber,
        float x, float y,
        float width, float height,
        String signerName,
        String cpf,
        String issuerCA,
        String dateTime
    ) throws IOException {

        // Get or create AcroForm
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }

        // Create signature field
        PDSignatureField signatureField = new PDSignatureField(acroForm);
        signatureField.setPartialName("Signature1");

        // Get page
        PDPage page = document.getPage(pageNumber - 1);

        // Create widget annotation
        PDAnnotationWidget widget = signatureField.getWidgets().get(0);
        widget.setPage(page);

        // Set rectangle (position)
        PDRectangle rect = new PDRectangle(x, y, width, height);
        widget.setRectangle(rect);

        // Create appearance
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
        PDAppearanceStream appearanceStream = new PDAppearanceStream(document);
        appearanceStream.setResources(new PDResources());
        appearanceStream.setBBox(new PDRectangle(width, height));

        // Draw signature content
        PDPageContentStream cs = new PDPageContentStream(
            document, appearanceStream);

        // Border
        cs.setStrokingColor(0, 0, 0);
        cs.addRect(1, 1, width - 2, height - 2);
        cs.stroke();

        // Title
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(5, height - 15);
        cs.showText("ASSINADO DIGITALMENTE");
        cs.endText();

        // Line
        cs.moveTo(5, height - 20);
        cs.lineTo(width - 5, height - 20);
        cs.stroke();

        // Content
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8);
        cs.setLeading(12);
        cs.newLineAtOffset(5, height - 32);
        cs.showText("Nome: " + signerName);
        cs.newLine();
        cs.showText("CPF: " + cpf);
        cs.newLine();
        cs.showText("AC: " + issuerCA);
        cs.newLine();
        cs.showText("Data: " + dateTime);
        cs.endText();

        cs.close();

        appearanceDict.setNormalAppearance(appearanceStream);
        widget.setAppearance(appearanceDict);

        // Add to page and form
        page.getAnnotations().add(widget);
        acroForm.getFields().add(signatureField);

        // Set signature value
        signatureField.setValue(signature);
    }
}
```

---

## Appendix C: File Structure After Implementation

```
src/main/java/com/example/documentsigner/
├── Main.java
├── DocumentSigner.java                    (existing - CMS)
├── PdfSigner.java                         (modified - format selection)
├── CertificateValidator.java              (existing - reuse)
├── ItiVerificador.java                    (existing - extend)
├── DocumentSignerUI.java                  (modified - GUI updates)
│
├── pades/                                 (NEW PACKAGE)
│   ├── PadesSignerService.java            (core PAdES signing)
│   ├── PadesSignatureInterface.java       (PDFBox callback)
│   ├── VisibleSignatureRenderer.java      (visual appearance)
│   ├── SignerDisplayInfo.java             (extracted cert info)
│   └── dto/
│       ├── SignatureMetadata.java
│       ├── VisualSignatureConfig.java
│       ├── SignaturePosition.java
│       └── PdfVerificationResult.java
│
├── api/
│   ├── ApiApplication.java                (existing)
│   ├── SignerController.java              (modified - new endpoints)
│   ├── SigningService.java                (modified - PAdES methods)
│   ├── GlobalExceptionHandler.java        (existing)
│   └── dto/
│       ├── CertificateInfo.java           (existing)
│       ├── SignResponse.java              (existing)
│       ├── PdfSignResponse.java           (NEW)
│       ├── ErrorResponse.java             (existing)
│       └── VerifyResponse.java            (existing)
│
└── exception/
    ├── SigningException.java              (existing)
    ├── InvalidPasswordException.java      (existing)
    ├── InvalidCertificateException.java   (existing)
    ├── InvalidDocumentException.java      (existing)
    └── ExpiredCertificateException.java   (existing)
```

---

**End of Implementation Plan**

*Document maintained by: Development Team*
*Last updated: 2026-01-12*
