# ProcStudio Document Signer

Assinador de documentos PDF com certificados A1 (padrão brasileiro). Gera assinaturas `.p7s` (CMS/PKCS#7).

## Downloads

Você pode baixar a versão mais recente em nossa [página de releases](https://github.com/brpl20/document-signer/releases).

## Requisitos

- **Java**: JRE 8 ou superior
  - [Download do Java](https://www.java.com/pt-BR/download/)
  - Verificar instalação: `java -version`

## Modos de Execução

O aplicativo suporta dois modos:

### Modo GUI (Interface Gráfica)

```bash
java -jar document-signer-1.0-SNAPSHOT.jar
```

### Modo API (Servidor REST)

```bash
java -jar document-signer-1.0-SNAPSHOT.jar --api
```

O servidor inicia na porta 8080.

---

## Instalação

### Linux / macOS

```bash
# Baixar o JAR do releases
# Executar:
java -jar document-signer-1.0-SNAPSHOT.jar
```

### Windows

1. Baixe o arquivo JAR
2. Duplo clique no arquivo, ou
3. Abra o CMD e execute: `java -jar document-signer-1.0-SNAPSHOT.jar`

---

## Uso - Modo GUI

1. Abra o aplicativo
2. Selecione seu certificado digital A1 (.pfx)
3. Digite a senha do certificado
4. Escolha uma opção:
   - **Assinar arquivo**: Assina um único PDF
   - **Assinar múltiplos**: Seleciona vários PDFs
   - **Assinar pasta**: Assina todos PDFs de uma pasta
   - **Arrastar e soltar**: Arraste PDFs diretamente para a janela

Os arquivos assinados são salvos com extensão `.p7s`.

### Recursos da Interface

- Lembra o último diretório usado
- Lembra o último certificado selecionado
- Suporte a arrastar e soltar (drag-and-drop)

---

## Uso - Modo API

### Endpoints Disponíveis

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/health` | Health check |
| `POST` | `/api/v1/certificate/info` | Detalhes do certificado |
| `POST` | `/api/v1/certificate/validate` | Validar senha e validade |
| `POST` | `/api/v1/sign` | Assina PDF (retorna arquivo .p7s) |
| `POST` | `/api/v1/sign/json` | Assina PDF (retorna JSON com base64) |
| `POST` | `/api/v1/sign/batch` | Assina múltiplos PDFs |
| `POST` | `/api/v1/sign/verified` | Assina e valida no ITI |
| `POST` | `/api/v1/verify` | Verifica assinatura localmente |
| `POST` | `/api/v1/verify/iti` | Verifica no ITI Verificador |

### Exemplos com cURL

#### Health Check

```bash
curl http://localhost:8080/api/v1/health
```

Resposta:
```json
{
  "status": "ok",
  "service": "document-signer",
  "timestamp": "2026-01-07T19:14:38.098Z"
}
```

#### Assinar Documento (Download .p7s)

```bash
curl -X POST http://localhost:8080/api/v1/sign \
  -F "document=@documento.pdf" \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha" \
  -o documento.pdf.p7s
```

#### Assinar Documento (Resposta JSON)

```bash
curl -X POST http://localhost:8080/api/v1/sign/json \
  -F "document=@documento.pdf" \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha"
```

Resposta:
```json
{
  "success": true,
  "signature": "MIAGCSqGSIb3DQEHAq...",
  "filename": "documento.pdf",
  "timestamp": "2026-01-07T19:14:38.098Z"
}
```

#### Assinar Múltiplos Documentos

```bash
curl -X POST http://localhost:8080/api/v1/sign/batch \
  -F "documents=@doc1.pdf" \
  -F "documents=@doc2.pdf" \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha"
```

Resposta:
```json
{
  "success": true,
  "documents": [
    {"success": true, "signature": "...", "filename": "doc1.pdf", "timestamp": "..."},
    {"success": true, "signature": "...", "filename": "doc2.pdf", "timestamp": "..."}
  ],
  "total": 2,
  "signed": 2
}
```

#### Verificar Assinatura

```bash
curl -X POST http://localhost:8080/api/v1/verify \
  -F "document=@documento.pdf" \
  -F "signature=@documento.pdf.p7s"
```

Resposta:
```json
{
  "valid": true,
  "filename": "documento.pdf"
}
```

#### Obter Informações do Certificado

```bash
curl -X POST http://localhost:8080/api/v1/certificate/info \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha"
```

Resposta:
```json
{
  "valid": true,
  "subject": "CN=NOME DO TITULAR, ...",
  "commonName": "NOME DO TITULAR",
  "issuer": "CN=AC Certificadora, ...",
  "serialNumber": "ABC123...",
  "notBefore": "2024-01-01T00:00:00.000+00:00",
  "notAfter": "2025-01-01T00:00:00.000+00:00",
  "expired": false,
  "daysUntilExpiry": 365,
  "algorithm": "SHA256withRSA"
}
```

#### Validar Certificado (Senha e Validade)

```bash
curl -X POST http://localhost:8080/api/v1/certificate/validate \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha"
```

Resposta:
```json
{
  "valid": true,
  "message": "Certificate is valid and not expired",
  "timestamp": "2024-01-10T12:00:00.000Z"
}
```

#### Verificar no ITI (Governo Federal)

```bash
curl -X POST http://localhost:8080/api/v1/verify/iti \
  -F "document=@documento.pdf" \
  -F "signature=@documento.pdf.p7s" \
  -F "staging=false"
```

Resposta:
```json
{
  "success": true,
  "httpStatus": 200,
  "environment": "production",
  "itiResponse": "{ ... resposta do ITI ... }",
  "timestamp": "2024-01-10T12:00:00.000Z"
}
```

#### Assinar e Verificar no ITI

```bash
curl -X POST http://localhost:8080/api/v1/sign/verified \
  -F "document=@documento.pdf" \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha" \
  -F "staging=false"
```

Resposta:
```json
{
  "success": true,
  "signature": "MIAGCSqGSIb3DQEHAq...",
  "filename": "documento.pdf",
  "itiValidation": {
    "success": true,
    "httpStatus": 200,
    "environment": "production",
    "response": "{ ... resposta do ITI ... }"
  },
  "timestamp": "2024-01-10T12:00:00.000Z"
}
```

### Códigos de Erro

| Código | Erro | Descrição |
|--------|------|-----------|
| 400 | `INVALID_DOCUMENT` | Documento PDF inválido |
| 401 | `INVALID_PASSWORD` | Senha do certificado incorreta |
| 422 | `INVALID_CERTIFICATE` | Certificado inválido |
| 422 | `CERTIFICATE_EXPIRED` | Certificado expirado |
| 500 | `SIGNING_ERROR` | Erro ao assinar documento |
| 502 | `ITI_CONNECTION_ERROR` | Erro ao conectar com ITI |

### Configuração

O arquivo `application.properties` permite configurar:

```properties
server.port=8080
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
```

---

## ITI Verificador (Validação Oficial)

O sistema integra com o **ITI Verificador**, serviço oficial do Governo Federal para validação de assinaturas digitais ICP-Brasil.

### URLs Oficiais
- **Produção**: https://verificador.iti.gov.br
- **Homologação**: https://verificador.staging.iti.br
- **Portal**: https://validar.iti.gov.br
- **Documentação**: https://validar.iti.gov.br/guia-desenvolvedor.html

### Uso na API

```bash
# Verificar assinatura existente no ITI
curl -X POST http://localhost:8080/api/v1/verify/iti \
  -F "document=@documento.pdf" \
  -F "signature=@documento.pdf.p7s" \
  -F "staging=false"

# Assinar e verificar em uma única chamada
curl -X POST http://localhost:8080/api/v1/sign/verified \
  -F "document=@documento.pdf" \
  -F "certificate=@certificado.pfx" \
  -F "password=sua_senha"
```

### Uso Programático

```java
ItiVerificador verificador = new ItiVerificador(); // produção
// ou: new ItiVerificador(true); // homologação

ItiVerificationResult result = verificador.verifyDetachedSignature(
    signatureBytes,
    documentBytes,
    "documento.pdf.p7s",
    "documento.pdf"
);

if (result.isSuccess()) {
    System.out.println("Assinatura validada pelo ITI!");
}
```

---

## Bruno Collection

Uma coleção Bruno para testar a API está disponível em `/collection`.

1. Abra o Bruno
2. Clique em "Open Collection"
3. Selecione a pasta `/collection`
4. Execute os requests

---

## Docker

```bash
# Build
docker build -t document-signer .

# Run (API mode)
docker run -p 8080:8080 document-signer
```

---

## Desenvolvimento

### Build

```bash
mvn clean package
```

### Testes

```bash
# Testes unitários
mvn test

# Teste de assinatura (requer senha do certificado)
CERT_PASSWORD=sua_senha mvn test
```

### Estrutura do Projeto

```
src/main/java/com/example/documentsigner/
├── Main.java                    # Entry point (GUI ou API)
├── DocumentSigner.java          # Core signing logic
├── PdfSigner.java               # PDF wrapper
├── DocumentSignerUI.java        # Swing GUI
├── CertificateValidator.java    # Validação de certificados
├── ItiVerificador.java          # Cliente ITI Verificador
├── api/
│   ├── ApiApplication.java      # Spring Boot config
│   ├── SignerController.java    # REST endpoints
│   ├── SigningService.java      # Service layer
│   ├── GlobalExceptionHandler.java
│   └── dto/
│       ├── CertificateInfo.java # DTO info certificado
│       ├── SignResponse.java
│       ├── ErrorResponse.java
│       └── VerifyResponse.java
└── exception/
    ├── SigningException.java
    ├── InvalidCertificateException.java
    ├── InvalidPasswordException.java
    ├── InvalidDocumentException.java
    └── ExpiredCertificateException.java
```

---

## Suporte

Se encontrar problemas, abra uma [issue no GitHub](https://github.com/brpl20/document-signer/issues).

## Changelog

### v1.2.0
- Validação de certificado antes de assinar (senha e validade)
- Exibição de detalhes do certificado na GUI (botão "Check Certificate")
- Verificação de expiração do certificado com aviso ao usuário
- Integração com ITI Verificador (Governo Federal) - fonte oficial de validação
- Novos endpoints: `/certificate/info`, `/certificate/validate`, `/verify/iti`, `/sign/verified`
- Cliente `ItiVerificador` para validação externa programática
- Bruno collection expandida com novos endpoints
- Testes de integração com ITI

### v1.1.0
- Adicionado modo API REST
- Suporte a drag-and-drop na interface
- Lembra último diretório e certificado
- Bruno collection para testes

### v1.0.0
- Versão inicial com interface gráfica
