<script>
  import logo from './assets/logo.png';

  const API_BASE = '/api/v1';

  let certificateFiles = [];
  let certificatePassword = '';
  let outputFormat = 'pades';
  let includeVisualSignature = true;
  let page = 1;
  let signaturePosition = 'bottom-right';
  let documentsToSign = [];
  let statusMessage = '';
  let statusType = 'info'; // 'info', 'success', 'error'
  let isLoading = false;
  let certificateInfo = null;

  let certificateInput;
  let documentInput;

  function handleCertificateUpload(event) {
    certificateFiles = Array.from(event.target.files);
    certificateInfo = null;
  }

  function handleDocumentUpload(event) {
    documentsToSign = Array.from(event.target.files);
  }

  function setStatus(message, type = 'info') {
    statusMessage = message;
    statusType = type;
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  async function checkCertificate() {
    if (certificateFiles.length === 0) {
      setStatus('Por favor, selecione um arquivo de certificado primeiro.', 'error');
      return;
    }
    if (!certificatePassword) {
      setStatus('Por favor, insira a senha do certificado.', 'error');
      return;
    }

    isLoading = true;
    setStatus(`Verificando certificado: ${certificateFiles[0].name}...`);

    try {
      const formData = new FormData();
      formData.append('certificate', certificateFiles[0]);
      formData.append('password', certificatePassword);

      const res = await fetch(`${API_BASE}/certificate/info`, { method: 'POST', body: formData });
      const data = await res.json();

      if (!res.ok) {
        setStatus(data.message || 'Erro ao validar certificado.', 'error');
        certificateInfo = null;
      } else {
        certificateInfo = data;
        const expiry = data.expired
          ? ' (EXPIRADO!)'
          : ` (expira em ${data.daysUntilExpiry} dias)`;
        setStatus(`Certificado valido: ${data.commonName}${expiry}`, data.expired ? 'error' : 'success');
      }
    } catch (err) {
      setStatus('Erro de conexao com o servidor. Verifique se o backend esta rodando.', 'error');
      certificateInfo = null;
    } finally {
      isLoading = false;
    }
  }

  async function signDocuments() {
    if (certificateFiles.length === 0) {
      setStatus('Por favor, selecione um arquivo de certificado.', 'error');
      return;
    }
    if (!certificatePassword) {
      setStatus('Por favor, insira a senha do certificado.', 'error');
      return;
    }
    if (documentsToSign.length === 0) {
      setStatus('Por favor, selecione o(s) documento(s) para assinar.', 'error');
      return;
    }

    isLoading = true;
    const fileNames = documentsToSign.map(f => f.name).join(', ');
    setStatus(`Assinando ${documentsToSign.length} arquivo(s): ${fileNames}...`);

    try {
      if (isPades) {
        await signPades();
      } else {
        await signP7s();
      }
    } catch (err) {
      setStatus(`Erro ao assinar: ${err.message}`, 'error');
    } finally {
      isLoading = false;
    }
  }

  async function signPades() {
    if (documentsToSign.length === 1) {
      const formData = new FormData();
      formData.append('document', documentsToSign[0]);
      formData.append('certificate', certificateFiles[0]);
      formData.append('password', certificatePassword);
      formData.append('visible', includeVisualSignature);
      if (includeVisualSignature) {
        formData.append('page', page);
        formData.append('position', signaturePosition);
      }

      const res = await fetch(`${API_BASE}/sign/pdf`, { method: 'POST', body: formData });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Erro ao assinar PDF');
      }

      const blob = await res.blob();
      const filename = res.headers.get('Content-Disposition')?.match(/filename="?(.+?)"?$/)?.[1]
        || documentsToSign[0].name.replace('.pdf', '_signed.pdf');
      downloadBlob(blob, filename);
      setStatus(`Arquivo assinado com sucesso: ${filename}`, 'success');
    } else {
      const formData = new FormData();
      documentsToSign.forEach(f => formData.append('documents', f));
      formData.append('certificate', certificateFiles[0]);
      formData.append('password', certificatePassword);
      formData.append('visible', includeVisualSignature);
      if (includeVisualSignature) {
        formData.append('page', page);
        formData.append('position', signaturePosition);
      }

      const res = await fetch(`${API_BASE}/sign/pdf/batch`, { method: 'POST', body: formData });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Erro ao assinar PDFs');
      }

      const blob = await res.blob();
      const signed = res.headers.get('X-Signed-Count') || documentsToSign.length;
      const failed = res.headers.get('X-Failed-Count') || '0';
      downloadBlob(blob, 'documentos_assinados.zip');
      setStatus(`${signed} arquivo(s) assinado(s) com sucesso. ${failed > 0 ? `${failed} falha(s).` : ''}`, failed > 0 ? 'error' : 'success');
    }
  }

  async function signP7s() {
    if (documentsToSign.length === 1) {
      const formData = new FormData();
      formData.append('document', documentsToSign[0]);
      formData.append('certificate', certificateFiles[0]);
      formData.append('password', certificatePassword);

      const res = await fetch(`${API_BASE}/sign`, { method: 'POST', body: formData });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Erro ao assinar documento');
      }

      const blob = await res.blob();
      const filename = documentsToSign[0].name + '.p7s';
      downloadBlob(blob, filename);
      setStatus(`Assinatura gerada com sucesso: ${filename}`, 'success');
    } else {
      const formData = new FormData();
      documentsToSign.forEach(f => formData.append('documents', f));
      formData.append('certificate', certificateFiles[0]);
      formData.append('password', certificatePassword);

      const res = await fetch(`${API_BASE}/sign/batch`, { method: 'POST', body: formData });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Erro ao assinar documentos');
      }

      const data = await res.json();
      // Download each signature as individual .p7s files
      for (const doc of data.documents) {
        if (doc.success && doc.signature) {
          const bytes = Uint8Array.from(atob(doc.signature), c => c.charCodeAt(0));
          const blob = new Blob([bytes], { type: 'application/octet-stream' });
          downloadBlob(blob, doc.filename + '.p7s');
        }
      }
      setStatus(`${data.signed} de ${data.total} arquivo(s) assinado(s) com sucesso.`, data.signed === data.total ? 'success' : 'error');
    }
  }

  $: isMultipleFiles = documentsToSign.length > 1;
  $: isPades = outputFormat === 'pades';
</script>

<main>
  <div class="container">
    <!-- Header with Logo -->
    <header class="header">
      <img src={logo} alt="ProcStudio" class="logo" />
      <h1>Assinador de Documentos</h1>
    </header>

    <!-- Certificate Section -->
    <section class="section">
      <h2>Certificado</h2>
      <div class="form-row">
        <label for="cert-file">Arquivo do Certificado (.pfx):</label>
        <div class="input-group">
          <input
            type="text"
            readonly
            value={certificateFiles.length > 0 ? certificateFiles.map(f => f.name).join(', ') : ''}
            placeholder="Selecione o arquivo do certificado..."
          />
          <button class="btn-secondary" on:click={() => certificateInput.click()}>Procurar</button>
          <input
            bind:this={certificateInput}
            type="file"
            accept=".pfx,.p12"
            on:change={handleCertificateUpload}
            hidden
          />
        </div>
      </div>

      <div class="form-row">
        <label for="cert-password">Senha do Certificado:</label>
        <div class="input-group">
          <input
            id="cert-password"
            type="password"
            bind:value={certificatePassword}
            placeholder="Insira a senha do certificado"
          />
          <button class="btn-secondary" on:click={checkCertificate} disabled={isLoading}>
            {isLoading ? 'Verificando...' : 'Verificar Certificado'}
          </button>
        </div>
      </div>
    </section>

    <!-- Output Format Section -->
    <section class="section">
      <h2>Formato de Saida</h2>
      <div class="radio-group">
        <label class="radio-label">
          <input type="radio" bind:group={outputFormat} value="pades" />
          PDF Assinado (PAdES) - Recomendado
        </label>
        <label class="radio-label">
          <input type="radio" bind:group={outputFormat} value="p7s" />
          Assinatura Destacada (.p7s)
        </label>
      </div>

      {#if isPades}
        <div class="visual-signature-options">
          <label class="checkbox-label">
            <input type="checkbox" bind:checked={includeVisualSignature} />
            Incluir assinatura visual
          </label>

          {#if includeVisualSignature}
            <div class="signature-details">
              <div class="inline-field">
                <label for="page">Pagina:</label>
                <input
                  id="page"
                  type="number"
                  min="1"
                  bind:value={page}
                  class="small-input"
                />
              </div>
              <div class="inline-field">
                <label for="position">Posicao:</label>
                <select id="position" bind:value={signaturePosition}>
                  <option value="top-left">Superior Esquerdo</option>
                  <option value="top-right">Superior Direito</option>
                  <option value="bottom-left">Inferior Esquerdo</option>
                  <option value="bottom-right">Inferior Direito</option>
                  <option value="center">Centro</option>
                </select>
              </div>
            </div>
          {/if}
        </div>
      {/if}
    </section>

    <!-- Document Upload & Sign Section -->
    <section class="section">
      <h2>Documentos</h2>
      <div class="form-row">
        <label for="doc-files">Arquivos para assinar:</label>
        <div class="input-group">
          <input
            id="doc-files"
            type="text"
            readonly
            value={documentsToSign.length > 0 ? documentsToSign.map(f => f.name).join(', ') : ''}
            placeholder="Selecione o(s) documento(s) para assinar..."
          />
          <button class="btn-secondary" on:click={() => documentInput.click()}>Procurar</button>
          <input
            bind:this={documentInput}
            type="file"
            accept={isPades ? '.pdf' : '*'}
            multiple
            on:change={handleDocumentUpload}
            hidden
          />
        </div>
      </div>

      <div class="actions">
        {#if isMultipleFiles}
          <button class="btn-primary" on:click={signDocuments} disabled={isLoading}>
            {isLoading ? 'Assinando...' : `Assinar Multiplos Arquivos (${documentsToSign.length})`}
          </button>
        {:else}
          <button class="btn-primary" on:click={signDocuments} disabled={isLoading}>
            {isLoading ? 'Assinando...' : 'Assinar Arquivo'}
          </button>
        {/if}
      </div>
    </section>

    <!-- Status Bar -->
    {#if statusMessage}
      <div class="status-bar status-{statusType}">
        {statusMessage}
      </div>
    {/if}

    {#if certificateInfo && !certificateInfo.expired}
      <div class="cert-info">
        <strong>Titular:</strong> {certificateInfo.commonName}<br/>
        <strong>Emissor:</strong> {certificateInfo.issuer}<br/>
        <strong>Valido ate:</strong> {new Date(certificateInfo.notAfter).toLocaleDateString('pt-BR')}
      </div>
    {/if}

    <footer class="footer">
      <span>ProcStudio Assinador de Documentos</span>
    </footer>
  </div>
</main>

<style>
  main {
    display: flex;
    justify-content: center;
    padding: 2rem 1rem;
  }

  .container {
    background: var(--white);
    border-radius: 10px;
    box-shadow: 0 4px 16px rgba(15, 26, 62, 0.1);
    padding: 2rem;
    max-width: 720px;
    width: 100%;
    border-top: 4px solid var(--blue);
  }

  .header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid var(--gray-200);
  }

  .logo {
    height: 36px;
    width: auto;
  }

  h1 {
    margin: 0;
    font-size: 1.4rem;
    color: var(--navy);
    font-weight: 600;
  }

  h2 {
    margin: 0 0 1rem;
    font-size: 1rem;
    color: var(--blue);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .section {
    margin-bottom: 1.5rem;
    padding: 1.25rem;
    border: 1px solid var(--gray-200);
    border-radius: 8px;
    background: var(--gray-50);
  }

  .form-row {
    margin-bottom: 0.75rem;
  }

  .form-row label {
    display: block;
    margin-bottom: 0.35rem;
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--gray-700);
  }

  .input-group {
    display: flex;
    gap: 0.5rem;
  }

  .input-group input[type="text"],
  .input-group input[type="password"] {
    flex: 1;
    padding: 0.5rem 0.75rem;
    border: 1px solid var(--gray-300);
    border-radius: 6px;
    font-size: 0.875rem;
    background: var(--white);
    color: var(--navy);
    transition: border-color 0.2s;
  }

  .input-group input[type="text"]:focus,
  .input-group input[type="password"]:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px rgba(0, 136, 255, 0.12);
  }

  .btn-secondary {
    padding: 0.5rem 1rem;
    background: var(--navy);
    color: var(--white);
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.825rem;
    font-weight: 500;
    white-space: nowrap;
    transition: background-color 0.2s;
  }

  .btn-secondary:hover {
    background: var(--navy-dark);
  }

  .radio-group {
    display: flex;
    gap: 1.5rem;
    margin-bottom: 1rem;
  }

  .radio-label,
  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.875rem;
    cursor: pointer;
    color: var(--navy);
  }

  .radio-label input[type="radio"],
  .checkbox-label input[type="checkbox"] {
    accent-color: var(--blue);
  }

  .visual-signature-options {
    padding: 0.75rem 1rem;
    border: 1px solid var(--blue-border);
    border-radius: 6px;
    background: var(--blue-light);
  }

  .signature-details {
    display: flex;
    gap: 1.5rem;
    margin-top: 0.75rem;
    padding-left: 1.5rem;
  }

  .inline-field {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.875rem;
    color: var(--navy);
  }

  .small-input {
    width: 60px;
    padding: 0.35rem 0.5rem;
    border: 1px solid var(--gray-300);
    border-radius: 6px;
    font-size: 0.875rem;
    background: var(--white);
  }

  .small-input:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px rgba(0, 136, 255, 0.12);
  }

  select {
    padding: 0.35rem 0.5rem;
    border: 1px solid var(--gray-300);
    border-radius: 6px;
    font-size: 0.875rem;
    background: var(--white);
    color: var(--navy);
  }

  select:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px rgba(0, 136, 255, 0.12);
  }

  .actions {
    margin-top: 0.75rem;
  }

  .btn-primary {
    padding: 0.65rem 1.75rem;
    background: var(--blue);
    color: var(--white);
    border: none;
    border-radius: 6px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .btn-primary:hover {
    background: var(--blue-hover);
  }

  .status-bar {
    margin-top: 1rem;
    padding: 0.75rem 1rem;
    border-radius: 6px;
    font-size: 0.875rem;
  }

  .status-info {
    background: var(--blue-light);
    border: 1px solid var(--blue-border);
    color: var(--navy);
  }

  .status-success {
    background: #e6f9e6;
    border: 1px solid #82d982;
    color: #1a5c1a;
  }

  .status-error {
    background: #fde8e8;
    border: 1px solid #f5a3a3;
    color: #8b1a1a;
  }

  .cert-info {
    margin-top: 0.75rem;
    padding: 0.75rem 1rem;
    background: #e6f9e6;
    border: 1px solid #82d982;
    border-radius: 6px;
    font-size: 0.825rem;
    color: var(--navy);
    line-height: 1.6;
  }

  .btn-primary:disabled,
  .btn-secondary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .footer {
    margin-top: 1.5rem;
    padding-top: 0.75rem;
    border-top: 1px solid var(--gray-200);
    text-align: center;
    font-size: 0.75rem;
    color: var(--gray-500);
  }
</style>
