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
| `POST` | `/api/v1/sign` | Assina PDF (retorna arquivo .p7s) |
| `POST` | `/api/v1/sign/json` | Assina PDF (retorna JSON com base64) |
| `POST` | `/api/v1/sign/batch` | Assina múltiplos PDFs |
| `POST` | `/api/v1/verify` | Verifica assinatura |

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

### Códigos de Erro

| Código | Erro | Descrição |
|--------|------|-----------|
| 400 | `INVALID_DOCUMENT` | Documento PDF inválido |
| 401 | `INVALID_PASSWORD` | Senha do certificado incorreta |
| 422 | `INVALID_CERTIFICATE` | Certificado inválido |
| 500 | `SIGNING_ERROR` | Erro ao assinar documento |

### Configuração

O arquivo `application.properties` permite configurar:

```properties
server.port=8080
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
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
├── Main.java                 # Entry point (GUI ou API)
├── DocumentSigner.java       # Core signing logic
├── PdfSigner.java            # PDF wrapper
├── DocumentSignerUI.java     # Swing GUI
├── api/
│   ├── ApiApplication.java   # Spring Boot config
│   ├── SignerController.java # REST endpoints
│   ├── SigningService.java   # Service layer
│   └── GlobalExceptionHandler.java
└── exception/
    ├── SigningException.java
    ├── InvalidCertificateException.java
    ├── InvalidPasswordException.java
    └── InvalidDocumentException.java
```

---

## Suporte

Se encontrar problemas, abra uma [issue no GitHub](https://github.com/brpl20/document-signer/issues).

## Changelog

### v1.1.0
- Adicionado modo API REST
- Suporte a drag-and-drop na interface
- Lembra último diretório e certificado
- Bruno collection para testes

### v1.0.0
- Versão inicial com interface gráfica
