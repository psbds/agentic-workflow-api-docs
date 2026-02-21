# Agentic Workflow - API Documentation

Workflow agentic que analisa automaticamente o código-fonte de uma API REST e gera documentação completa para cada endpoint, criando um Pull Request com os arquivos Markdown gerados.

## Como Funciona

- **Trigger**: Executa semanalmente (domingo) e permite execução manual via `workflow_dispatch`
- **Engine**: GitHub Copilot (default)
- **Output**: Cria um **Draft Pull Request** com arquivos de documentação em `docs/endpoints/`
- **Labels**: `documentation`, `automation`, `ai-generated`

---

## Guia de Setup — Configurando em Outro Repositório

### Pré-requisitos

- [GitHub CLI (`gh`)](https://cli.github.com/) instalado
- Extensão `gh-aw` instalada: `gh extension install github/gh-aw`
- Repositório com código-fonte de uma API REST (Java/Quarkus, Spring Boot, etc.)

---

### Passo 1: Instalar o GitHub CLI e a extensão gh-aw

```bash
# Instalar GitHub CLI (se ainda não tiver)
# macOS
brew install gh

# Windows
winget install GitHub.cli

# Linux
sudo apt install gh

# Autenticar no GitHub
gh auth login

# Instalar a extensão gh-aw
gh extension install github/gh-aw
```

---

### Passo 2: Inicializar o repositório para Agentic Workflows

No diretório raiz do repositório alvo:

```bash
cd /caminho/do/seu/repositorio
gh aw init
```

Isso cria a estrutura necessária em `.github/`.

---

### Passo 3: Copiar os arquivos do workflow

Copie os seguintes arquivos para o repositório alvo:

```
.github/
  workflows/
    weekly-api-docs.md          ← Workflow agentic (prompt + config)
```

> **Importante**: O arquivo `.lock.yml` será gerado automaticamente no próximo passo. Nunca edite o `.lock.yml` manualmente.

---

### Passo 4: Compilar o workflow

```bash
gh aw compile weekly-api-docs
```

Verifique que a compilação termina com **0 errors, 0 warnings**. Isso gera o arquivo `.github/workflows/weekly-api-docs.lock.yml`.

---

### Passo 5: Configurar o Personal Access Token (PAT) — `COPILOT_GITHUB_TOKEN`

O workflow usa o **Copilot** como AI engine, que requer um PAT dedicado para autenticação. Já o `create-pull-request` (safe-output) é gerenciado automaticamente pelo framework gh-aw usando o `GITHUB_TOKEN` padrão do Actions — **não precisa de PAT adicional**.

#### Criar um Fine-Grained Personal Access Token

1. Vá em [github.com/settings/personal-access-tokens/new](https://github.com/settings/personal-access-tokens/new)
2. Configure:

| Campo | Valor |
|---|---|
| **Token name** | `copilot-agentic-workflows` |
| **Expiration** | 90 dias (ou conforme política da organização) |
| **Resource owner** | Selecione **sua conta pessoal** (não uma organização) |
| **Repository access** | Selecione **Public repositories** |

> **Importante**: Você **deve** selecionar "Public repositories", mesmo que o repositório alvo seja privado. Caso contrário, a permissão "Copilot Requests" não aparecerá na lista.

3. Em **Account permissions**, habilite:

| Permission | Access Level | Motivo |
|---|---|---|
| **Copilot Requests** | Read | Autenticar o Copilot CLI como AI engine |

> **Nota**: Nenhuma **Repository permission** ou **Organization permission** é necessária para este token.

4. Clique em **Generate token** e copie o valor

---

### Passo 6: Adicionar o token como Secret no repositório

```bash
gh aw secrets set COPILOT_GITHUB_TOKEN --value "SEU_TOKEN_AQUI"
```

Ou pela UI:

1. Vá no repositório → **Settings** → **Secrets and variables** → **Actions**
2. Clique em **New repository secret**
3. Configure:

| Campo | Valor |
|---|---|
| **Name** | `COPILOT_GITHUB_TOKEN` |
| **Value** | Cole o PAT gerado no passo anterior |

> **Nota**: O safe-output `create-pull-request` usa o `GITHUB_TOKEN` padrão do GitHub Actions (configurado via `permissions:` no frontmatter do workflow). Não é necessário criar um PAT separado para criação de PRs.

---

### Passo 7: Habilitar GitHub Actions no repositório

1. Vá no repositório → **Settings** → **Actions** → **General**
2. Em **Actions permissions**, selecione **Allow all actions and reusable workflows**
3. Em **Workflow permissions**, selecione **Read and write permissions**
4. Marque **Allow GitHub Actions to create and approve pull requests**
5. Clique em **Save**

---

### Passo 8: Commit e Push

```bash
git add .github/workflows/weekly-api-docs.md .github/workflows/weekly-api-docs.lock.yml
git commit -m "feat: add weekly API documentation agentic workflow"
git push origin main
```

---

### Passo 9: Testar manualmente

Após o push, execute manualmente para validar:

1. Vá no repositório → **Actions**
2. Encontre o workflow **weekly-api-docs**
3. Clique em **Run workflow** → **Run workflow**
4. Acompanhe a execução e verifique se o Draft PR é criado corretamente

Ou via CLI:

```bash
gh workflow run weekly-api-docs.lock.yml
```

---

## Customização

### Alterar a frequência de execução

Edite o campo `on.schedule` no frontmatter:

```yaml
on:
  schedule: daily      # Diário (horário fuzzy automático)
  # ou
  schedule: weekly     # Semanal (horário fuzzy automático)
  # ou
  schedule:
    - cron: "0 9 * * 1"  # Toda segunda-feira às 9h UTC
```

> Após alterar o frontmatter, recompile com `gh aw compile weekly-api-docs`.

### Alterar o idioma da documentação

Edite a regra de idioma na seção "Regras Adicionais" do corpo markdown. Não precisa recompilar — mudanças no corpo do markdown são aplicadas automaticamente no próximo run.

### Adicionar integração com MkDocs

O workflow já suporta MkDocs. Basta ter um arquivo `mkdocs.yml` ou `mkdocs.yaml` na raiz do repositório e o agente atualizará a seção `nav:` automaticamente.

---

## Troubleshooting

| Problema | Solução |
|---|---|
| Workflow não aparece em Actions | Verifique se o `.lock.yml` foi commitado e está na branch default |
| PR não é criado | Verifique as permissões do Actions (Settings → Actions → Workflow permissions → "Allow GitHub Actions to create and approve pull requests") |
| Copilot falha na inferência | Verifique o secret `COPILOT_GITHUB_TOKEN` e se a conta tem licença Copilot ativa |
| Compilação falha | Execute `gh aw compile weekly-api-docs --verbose` para ver erros detalhados |
| Timeout (30 min) | Aumente `timeout-minutes` no frontmatter e recompile |
| Documentação incompleta | Ajuste a seção "Estrutura do Projeto" para refletir seus pacotes |

