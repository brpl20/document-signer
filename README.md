# Manual de Instalação - Assinador Projudi do ProcStudio

## Downloads
Você pode baixar a versão mais recente do Assinador Projudi do ProcStudio em nossa página de releases.

## Requisitos

- **Java**: É necessário ter o Java Runtime Environment (JRE) 8 ou superior instalado.
  - [Download do Java](https://www.java.com/pt-BR/download/)
  - Para verificar se você já tem o Java instalado, abra o terminal ou prompt de comando e digite: `java -version`

## Instalação

### Linux

1. Baixe o arquivo no [realeses](https://github.com/brpl20/document-signer/releases/tag/1.0.0).
2. Abra o terminal e navegue até a pasta onde o arquivo foi baixado.
3. Execute o comando: `java -jar ProcStudioSigner.jar`.
4. Opcionalmente, você pode tornar o arquivo executável: `chmod +x ProcStudioSigner.jar`.

### macOS

1. Baixe o arquivo no [realeses](https://github.com/brpl20/document-signer/releases/tag/1.0.0).
2. Abra o Terminal e navegue até a pasta onde o arquivo foi baixado.
3. Execute o comando: `java -jar ProcStudioSigner.jar`.
4. Alternativamente, você pode dar duplo clique no arquivo JAR se seu sistema estiver configurado para abrir arquivos JAR com o Java.

### Windows

1. Baixe o arquivo no [realeses](https://github.com/brpl20/document-signer/releases/tag/1.0.0).
2. Certifique-se de que o Java está instalado corretamente.
3. Opção 1: Dê um duplo clique no arquivo JAR (se o seu Windows estiver configurado para abrir arquivos JAR com o Java).
4. Opção 2: Abra o Prompt de Comando (cmd):
   - Navegue até a pasta onde o arquivo foi baixado usando o comando `cd`.
   - Execute o comando: `java -jar ProcStudioSigner.jar`.

## Uso Básico

1. Abra o aplicativo.
2. Selecione seu certificado digital tipo A1.
3. Escolha uma das opções:
   - Assinar um único arquivo.
   - Assinar múltiplos arquivos.
   - Assinar todos os arquivos em uma pasta.
4. Selecione o(s) arquivo(s) ou a pasta desejada.
5. Os arquivos assinados serão salvos no mesmo local dos originais com a extensão adicional `.p7s`.

## Suporte
Se encontrar problemas, abra uma issue no GitHub.
