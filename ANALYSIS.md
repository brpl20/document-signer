# ProcStudio Signer - Technical Analysis

**Date:** 2026-01-06
**Purpose:** Analysis for future improvements and architectural decisions

---

## Project Overview

**Application:** ProcStudio Signer - A Java Swing desktop PDF signer for A1 certificates (Brazilian standard)

**Output Format:** `.p7s` detached signatures (CMS/PKCS#7)

**Core Stack:**
- Java 8 (Swing UI)
- BouncyCastle 1.70 (cryptography, CMS/PKCS#7 signatures)
- Apache PDFBox 2.0.27 (PDF processing)
- Maven (build system)

**Current Distribution:** Single JAR file (~10MB) requiring `java -jar` command

---

## Current Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Entry Point                          │
│                      Main.java                          │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Swing)                     │
│                  DocumentSignerUI.java                  │
│  - Certificate file selection                           │
│  - Password input                                       │
│  - Sign File / Sign Multiple / Sign Folder buttons     │
│  - Log output area                                      │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   PDF Layer                             │
│                   PdfSigner.java                        │
│  - Load PDF with PDFBox                                 │
│  - Convert to byte array                                │
│  - Save .p7s output                                     │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Signing Layer                          │
│                 DocumentSigner.java                     │
│  - Load PKCS#12 keystore (.pfx)                        │
│  - Extract private key and certificate chain           │
│  - Generate CMS SignedData (SHA256withRSA)             │
│  - Verify signatures                                    │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Crypto Layer                           │
│              BouncyCastle + JDK KeyStore               │
│  - RSA/SHA256 algorithms                                │
│  - X.509 certificate handling                           │
│  - CMS structure generation                             │
└─────────────────────────────────────────────────────────┘
```

---

## Issue 1: File Navigation Improvements

### Current State

File navigation uses basic `JFileChooser` in `DocumentSignerUI.java:89-108`.

### Identified Problems

| Problem | Impact |
|---------|--------|
| No recent files history | User must navigate from scratch each time |
| Certificate path not remembered | Must re-select certificate every session |
| No drag-and-drop support | Less intuitive UX |
| Basic file chooser styling | Doesn't feel native |
| No file preview/metadata | No visibility into selected files |

### Proposed Improvements

| Feature | Complexity | Priority | Implementation Notes |
|---------|------------|----------|---------------------|
| Remember last directory | Low | High | Use `java.util.prefs.Preferences` |
| Remember last certificate path | Low | High | Store in Preferences, auto-populate field |
| Drag-and-drop files | Medium | High | Implement `TransferHandler` on main panel |
| Recent files list (last 5-10) | Medium | Medium | Preferences + dropdown or menu |
| Native file dialogs (AWT FileDialog) | Low | Medium | Replace JFileChooser with FileDialog |
| File preview panel | High | Low | Show PDF metadata before signing |

### Implementation Reference

```java
// Preferences example
Preferences prefs = Preferences.userNodeForPackage(DocumentSignerUI.class);
String lastDir = prefs.get("lastDirectory", System.getProperty("user.home"));
String lastCert = prefs.get("lastCertificate", "");

// Save on selection
prefs.put("lastDirectory", selectedFile.getParent());
prefs.put("lastCertificate", certFile.getAbsolutePath());
```

---

## Issue 2: Standalone Without JAR Command

### Current State

Application requires: `java -jar ProcStudioSigner.jar`

### Options Analysis

#### Option A: jpackage (Recommended)

**What:** Official Oracle tool (JDK 14+) that creates native installers with bundled JRE.

**Output:**
- macOS: `.dmg` or `.app` bundle
- Windows: `.exe` or `.msi` installer
- Linux: `.deb` or `.rpm` package

**Pros:**
- Official, maintained by Oracle
- Creates true double-clickable applications
- Bundles JRE so users don't need Java installed
- Can target Java 8 apps (just need JDK 14+ to run jpackage)

**Cons:**
- Larger file size (~40-100MB with JRE)
- Need to build separately for each OS

**Implementation:**

```bash
# macOS
jpackage \
  --input target/ \
  --main-jar document-signer-1.0-SNAPSHOT.jar \
  --name "ProcStudio Signer" \
  --app-version "1.0.0" \
  --vendor "ProcStudio" \
  --icon src/main/resources/icon.icns \
  --type dmg

# Windows
jpackage \
  --input target/ \
  --main-jar document-signer-1.0-SNAPSHOT.jar \
  --name "ProcStudio Signer" \
  --app-version "1.0.0" \
  --vendor "ProcStudio" \
  --icon src/main/resources/icon.ico \
  --type exe \
  --win-shortcut \
  --win-menu

# Linux
jpackage \
  --input target/ \
  --main-jar document-signer-1.0-SNAPSHOT.jar \
  --name "procstudio-signer" \
  --app-version "1.0.0" \
  --vendor "ProcStudio" \
  --icon src/main/resources/icon.png \
  --type deb
```

#### Option B: GraalVM Native Image

**What:** Compiles Java to true native binary (no JVM).

**Pros:**
- Small binary size
- Instant startup
- No JRE required

**Cons:**
- BouncyCastle uses heavy reflection - requires extensive configuration
- Some crypto providers may not work
- Complex build process
- May have runtime issues with dynamic features

**Verdict:** Not recommended for this project due to BouncyCastle complexity.

#### Option C: Launch4j (Windows Only)

**What:** Wraps JAR in .exe with embedded or external JRE.

**Pros:**
- Simple setup
- Windows-native experience

**Cons:**
- Windows only
- Still needs JRE (bundled or installed)

#### Option D: Electron + Java Backend

**What:** Electron frontend communicating with Java signing service.

**Pros:**
- Modern UI possibilities
- Cross-platform

**Cons:**
- Massive overhead
- Complex architecture
- Overkill for this use case

### Recommendation

**Use jpackage** for creating native installers. It's the simplest, most reliable approach.

**Maven Plugin Integration:**

```xml
<!-- Add to pom.xml for automated builds -->
<plugin>
  <groupId>org.panteleyev</groupId>
  <artifactId>jpackage-maven-plugin</artifactId>
  <version>1.6.0</version>
  <configuration>
    <name>ProcStudio Signer</name>
    <appVersion>1.0.0</appVersion>
    <vendor>ProcStudio</vendor>
    <mainJar>document-signer-1.0-SNAPSHOT.jar</mainJar>
    <mainClass>com.example.documentsigner.Main</mainClass>
  </configuration>
</plugin>
```

---

## Issue 3: Headless/API Mode

### Current State

No API exists. Pure desktop GUI application.

### Feasibility: HIGH

The core signing logic (`DocumentSigner.java` and `PdfSigner.java`) has **zero UI dependencies**. The architecture already cleanly separates concerns.

### Proposed API Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   API Layer (New)                       │
│               Spring Boot / Spark Java                  │
│  - REST endpoints                                       │
│  - Request validation                                   │
│  - Authentication                                       │
│  - Response formatting                                  │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Signing Service (New)                      │
│  - Certificate management                               │
│  - Batch processing                                     │
│  - Error handling                                       │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│           Existing Core (No changes needed)             │
│         DocumentSigner.java + PdfSigner.java           │
└─────────────────────────────────────────────────────────┘
```

### API Endpoint Design

#### Sign Single Document

```
POST /api/v1/sign
Content-Type: application/json

Request:
{
  "document": "<base64-encoded-pdf>",
  "certificate": "<base64-encoded-pfx>",  // OR certificateId for server-stored
  "password": "<certificate-password>",
  "filename": "document.pdf"  // optional, for metadata
}

Response (200 OK):
{
  "success": true,
  "signature": "<base64-encoded-p7s>",
  "signedAt": "2026-01-06T12:00:00Z",
  "certificateSubject": "CN=User Name, ..."
}

Response (400 Bad Request):
{
  "success": false,
  "error": "Invalid certificate password",
  "code": "INVALID_PASSWORD"
}
```

#### Sign Multiple Documents

```
POST /api/v1/sign/batch
Content-Type: application/json

Request:
{
  "documents": [
    {"document": "<base64>", "filename": "doc1.pdf"},
    {"document": "<base64>", "filename": "doc2.pdf"}
  ],
  "certificate": "<base64-encoded-pfx>",
  "password": "<certificate-password>"
}

Response:
{
  "success": true,
  "results": [
    {"filename": "doc1.pdf", "signature": "<base64>", "success": true},
    {"filename": "doc2.pdf", "signature": "<base64>", "success": true}
  ]
}
```

#### Verify Signature

```
POST /api/v1/verify
Content-Type: application/json

Request:
{
  "document": "<base64-encoded-pdf>",
  "signature": "<base64-encoded-p7s>"
}

Response:
{
  "valid": true,
  "signer": {
    "subject": "CN=User Name, ...",
    "issuer": "CN=AC Certisign, ...",
    "validFrom": "2025-01-01",
    "validTo": "2026-01-01"
  }
}
```

### Security Considerations

| Concern | Mitigation |
|---------|-----------|
| Password in transit | HTTPS mandatory, consider encrypted payload |
| Certificate storage | If server-stored, use HSM or encrypted vault |
| API authentication | API keys, JWT, or OAuth2 |
| Rate limiting | Prevent abuse and DoS |
| Audit logging | Log all signing operations |
| Input validation | Validate PDF format, certificate format |

### Implementation Options

#### Option A: Spring Boot (Recommended for Java)

```xml
<!-- Add to pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <version>2.7.18</version>
</dependency>
```

#### Option B: Spark Java (Lightweight)

```xml
<dependency>
  <groupId>com.sparkjava</groupId>
  <artifactId>spark-core</artifactId>
  <version>2.9.4</version>
</dependency>
```

#### Option C: Separate Python API

Create a thin Python API that calls the Java JAR as subprocess or rewrite core in Python (see Issue 4).

---

## Issue 4: Rewriting in Another Language

### Library Complexity Analysis

| Library | Purpose | Difficulty to Replace |
|---------|---------|----------------------|
| BouncyCastle (bcprov) | Crypto algorithms, RSA, SHA256 | Medium - most languages have equivalents |
| BouncyCastle (bcpkix) | CMS/PKCS#7 generation, X.509 | HIGH - specialized, few equivalents |
| PDFBox | PDF loading | Low - many alternatives |
| JDK KeyStore | PKCS#12 loading | Low - OS-level support |

**The critical challenge is CMS/PKCS#7 signature generation with proper certificate chain inclusion.** This is what BouncyCastle excels at.

### Language Comparison

#### Python

| Aspect | Details |
|--------|---------|
| **Crypto Libraries** | `cryptography`, `pyOpenSSL`, `asn1crypto` |
| **PDF Signing** | `endesive` - specifically designed for PDF digital signatures! |
| **PDF Processing** | `PyPDF2`, `pikepdf`, `PyMuPDF` |
| **API Frameworks** | FastAPI, Flask (excellent, modern) |
| **Desktop GUI** | PyQt (GPL/commercial), Tkinter (basic), PySide6 |
| **Packaging** | PyInstaller, cx_Freeze |
| **Effort** | Medium - `endesive` handles most complexity |

**Key Library: endesive**
```python
# Example with endesive
from endesive.pdf import cms

def sign_pdf(pdf_path, pfx_path, password):
    with open(pfx_path, 'rb') as fp:
        p12 = pkcs12.load_key_and_certificates(fp.read(), password.encode())

    with open(pdf_path, 'rb') as fp:
        data = fp.read()

    signature = cms.sign(data, p12[0], p12[1], p12[2], 'sha256')
    return signature
```

**Verdict:** Best alternative to Java. `endesive` library is purpose-built for this.

#### C# / .NET

| Aspect | Details |
|--------|---------|
| **Crypto Libraries** | BouncyCastle.NET (near-identical API!) |
| **PDF Signing** | iText.NET, PDFSharp |
| **API Frameworks** | ASP.NET Core (excellent) |
| **Desktop GUI** | WPF, MAUI, Avalonia |
| **Packaging** | .NET 8 Native AOT, single-file publish |
| **Effort** | Medium - code translates almost directly |

**Verdict:** Excellent alternative. BouncyCastle.NET is essentially identical API.

#### Go

| Aspect | Details |
|--------|---------|
| **Crypto Libraries** | `crypto/x509`, limited CMS support |
| **PDF Signing** | No mature library |
| **API Frameworks** | Gin, Echo (excellent) |
| **Desktop GUI** | Fyne, Wails (with web frontend) |
| **Packaging** | Single binary (native) |
| **Effort** | HIGH - would need custom CMS implementation |

**Verdict:** Not recommended. CMS/PKCS#7 support is too limited.

#### Rust

| Aspect | Details |
|--------|---------|
| **Crypto Libraries** | `ring`, `rustls`, `p7` crate |
| **PDF Signing** | Limited ecosystem |
| **API Frameworks** | Actix, Axum (excellent) |
| **Desktop GUI** | egui, Tauri |
| **Packaging** | Single binary (native) |
| **Effort** | HIGH - immature ecosystem for this use case |

**Verdict:** Possible but significant investment. Not mature for PDF signing.

#### Node.js / TypeScript

| Aspect | Details |
|--------|---------|
| **Crypto Libraries** | `node-forge`, `pkcs7` |
| **PDF Signing** | `pdf-lib` (no signing), `node-signpdf` |
| **API Frameworks** | Express, Fastify, NestJS |
| **Desktop GUI** | Electron |
| **Packaging** | pkg, Electron Builder |
| **Effort** | HIGH - CMS support is weak |

**Verdict:** Not recommended for cryptographic work.

### Recommendation Matrix

| Goal | Recommended Language | Reason |
|------|---------------------|--------|
| Stay with current | Java | Already working, use jpackage for native |
| API-only service | Python + endesive | Purpose-built, modern API frameworks |
| Desktop + API | Java (refactor) | Add Spring Boot module |
| Maximum portability | C# / .NET 8 | BouncyCastle.NET + Native AOT |
| Escape Java entirely | Python | Best library support after Java |

### If Rewriting in Python

**Required Libraries:**

```
endesive>=2.4.0      # PDF/CMS signing
cryptography>=41.0   # Core crypto
pyOpenSSL>=23.0      # OpenSSL bindings
PyPDF2>=3.0          # PDF processing
fastapi>=0.100       # API framework (if needed)
uvicorn>=0.23        # ASGI server (if needed)
PyQt6>=6.5           # Desktop GUI (if needed)
pyinstaller>=5.13    # Packaging
```

**Project Structure:**

```
procstudio-signer-py/
├── src/
│   ├── core/
│   │   ├── signer.py        # Core signing logic
│   │   └── verifier.py      # Signature verification
│   ├── api/
│   │   ├── main.py          # FastAPI app
│   │   └── routes.py        # API endpoints
│   └── gui/
│       └── app.py           # PyQt6 interface
├── requirements.txt
├── pyproject.toml
└── README.md
```

---

## Summary: Decision Matrix

| Improvement | Effort | Impact | Recommendation |
|-------------|--------|--------|----------------|
| File navigation | Low | High | Do first - quick win |
| jpackage native build | Medium | High | Do second - major UX improvement |
| Add API layer | Medium | High | Do third if API needed |
| Full Python rewrite | High | Medium | Only if escaping Java is priority |

## Suggested Implementation Order

1. **Phase 1: Quick Wins (1-2 days)**
   - Remember last directory
   - Remember last certificate
   - Add drag-and-drop

2. **Phase 2: Native Packaging (1 day)**
   - Set up jpackage build
   - Create installers for macOS, Windows, Linux
   - Add Maven plugin for automated builds

3. **Phase 3: API Layer (3-5 days)**
   - Add Spring Boot dependency
   - Create REST endpoints
   - Add authentication
   - Create API documentation

4. **Phase 4: Rewrite (optional, 2-3 weeks)**
   - Only if Python/other language is strongly preferred
   - Start with core signing, then add API, then GUI

---

## Appendix: Key File Locations

| File | Purpose |
|------|---------|
| `src/main/java/com/example/documentsigner/Main.java` | Entry point |
| `src/main/java/com/example/documentsigner/DocumentSignerUI.java` | Swing UI |
| `src/main/java/com/example/documentsigner/DocumentSigner.java` | Core signing logic |
| `src/main/java/com/example/documentsigner/PdfSigner.java` | PDF wrapper |
| `pom.xml` | Maven build configuration |
| `src/dist/ProcStudioSigner.jar` | Current compiled JAR |

---

## Appendix: Projudi Integration Notes

**Current Status:** No Projudi-specific integration.

The application supports A1 certificates which are compatible with Projudi (Tribunal de Justica do Parana), but there is no:
- Projudi API integration
- Specific certificate validation for Projudi requirements
- Automated document submission

**Future Consideration:** If Projudi integration is needed, research their API documentation for automated submission after signing.
