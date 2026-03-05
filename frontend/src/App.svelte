<script>
  import logo from './assets/logo.png';

  let certificateFiles = [];
  let certificatePassword = '';
  let outputFormat = 'pades';
  let includeVisualSignature = true;
  let page = 1;
  let signaturePosition = 'bottom-right';
  let documentsToSign = [];
  let statusMessage = '';

  let certificateInput;
  let documentInput;

  function handleCertificateUpload(event) {
    certificateFiles = Array.from(event.target.files);
  }

  function handleDocumentUpload(event) {
    documentsToSign = Array.from(event.target.files);
  }

  function checkCertificate() {
    if (certificateFiles.length === 0) {
      statusMessage = 'Por favor, selecione um arquivo de certificado primeiro.';
      return;
    }
    if (!certificatePassword) {
      statusMessage = 'Por favor, insira a senha do certificado.';
      return;
    }
    statusMessage = `Verificando certificado: ${certificateFiles[0].name}...`;
    // TODO: conectar com a API do backend
    setTimeout(() => {
      statusMessage = 'Certificado validado com sucesso (mock).';
    }, 1000);
  }

  function signDocuments() {
    if (certificateFiles.length === 0) {
      statusMessage = 'Por favor, selecione um arquivo de certificado.';
      return;
    }
    if (!certificatePassword) {
      statusMessage = 'Por favor, insira a senha do certificado.';
      return;
    }
    if (documentsToSign.length === 0) {
      statusMessage = 'Por favor, selecione o(s) documento(s) para assinar.';
      return;
    }

    const fileNames = documentsToSign.map(f => f.name).join(', ');
    statusMessage = `Assinando ${documentsToSign.length} arquivo(s): ${fileNames}...`;
    // TODO: conectar com a API do backend
    setTimeout(() => {
      statusMessage = `${documentsToSign.length} arquivo(s) assinado(s) com sucesso (mock).`;
    }, 1500);
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
          <button class="btn-secondary" on:click={checkCertificate}>Verificar Certificado</button>
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
          <button class="btn-primary" on:click={signDocuments}>
            Assinar Multiplos Arquivos ({documentsToSign.length})
          </button>
        {:else}
          <button class="btn-primary" on:click={signDocuments}>
            Assinar Arquivo
          </button>
        {/if}
      </div>
    </section>

    <!-- Status Bar -->
    {#if statusMessage}
      <div class="status-bar">
        {statusMessage}
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
    background: var(--blue-light);
    border: 1px solid var(--blue-border);
    border-radius: 6px;
    font-size: 0.875rem;
    color: var(--navy);
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
