---
description: Analyzes the project source code and generates comprehensive API endpoint documentation in Markdown, then creates a PR with the results.
on:
  schedule:
    # Every day at 6am UTC
    - cron: weekly on sunday
  workflow_dispatch:
permissions:
  contents: read
  actions: read
  issues: read
  pull-requests: read
tools:
  github:
    toolsets: [default]
safe-outputs:
  create-pull-request:
    title-prefix: "[docs] "
    labels: [documentation, automation, ai-generated]
    draft: true
timeout-minutes: 30
---

# DocAgent — Geração Automática de Documentação de Endpoints

Você é um agente especializado em documentação técnica de APIs REST. Analise o código-fonte deste repositório e gere documentação completa para cada endpoint, criando **um arquivo Markdown separado por endpoint** em `docs/endpoints/`.

## Estratégia de Execução (IMPORTANTE para performance)

Para ser eficiente, siga esta abordagem:

1. **Primeiro**, leia TODAS as Resource classes de uma vez usando bash: `find src -name "*Resource.java" -exec cat {} +`
2. **Segundo**, leia TODOS os Services de uma vez: `find src -name "*Service.java" -exec cat {} +`
3. **Terceiro**, leia os arquivos de suporte necessários (DTOs, entidades, enums, mappers) em um único comando: `find src -name "*.java" -path "*/domain/*" -exec cat {} +`
4. **Quarto**, leia backends e infrastructure: `find src -name "*.java" \( -path "*/backends/*" -o -path "*/infrastructure/*" -o -path "*/messaging/*" \) -exec cat {} +`
5. **Somente após ter lido todo o código**, comece a gerar os arquivos de documentação
6. Gere os arquivos em lote — não releia o código entre cada endpoint

## Sua Tarefa

1. Percorra todas as classes Resource e identifique cada endpoint (`@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`)
2. Extraia o path completo combinando `@Path` da classe e do método
3. Rastreie a cadeia completa: **Resource** → **Service** → **Backend/Client** → **Repository** → **Domain**
4. Gere um arquivo Markdown por endpoint em `docs/endpoints/`

## O que Rastrear

Para cada endpoint, identifique: dependências injetadas, comunicações externas (APIs, BD, cache, filas), annotations de segurança (`@RolesAllowed`, `@Authenticated`), transacionais (`@Transactional`) e cache (`@CacheResult`, `@CacheInvalidate`).

## Formato de Saída

Gere **um arquivo Markdown por endpoint** no diretório `docs/endpoints/` com o nome no formato: `{path}_{METODO}.md` (ex: `reservations_POST.md`, `hotels_GET.md`).

Cada arquivo **deve conter obrigatoriamente** todas as seções abaixo, nesta ordem:

### Modelo do Arquivo de Saída

```
# {MÉTODO} /{path} — {Título descritivo em português}

## Descrição
Parágrafo descrevendo o que o endpoint faz, seu propósito no sistema e contexto geral.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `{GET/POST/PUT/DELETE}`          |
| **Path**             | `/{path completo}`               |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | {Tipo de autenticação ou "Nenhuma"} |
| **Roles Permitidas** | {Roles ou "Todas"}               |
| **Transacional**     | {Sim/Não}                        |
| **Cache**            | {Detalhes do cache ou "Não"}     |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|

## Request

### Headers
Tabela com headers necessários (Authorization, Content-Type, etc.).

### Body (se aplicável)
Exemplo JSON completo do body do request com dados realistas.

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|

### Query Parameters (se aplicável)
Tabela com parâmetros de query string.

## Response

### Sucesso — `{código} {status}`
Exemplo JSON completo da response com dados realistas.

### Campos da Response
Tabela detalhada com cada campo da response.

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|

Inclua erros de:
- Validação de request (Bean Validation / Jakarta Validation)
- Validação de regras de negócio (exceptions customizadas)
- Autenticação/Autorização
- Erros de comunicação com backends

## Regras de Negócio
1. Regras de autenticação/autorização
2. Validações feitas nos Services
3. Validações de domínio
4. Regras de cache
5. Regras de persistência/transação
6. Regras de auditoria

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|

## Diagrama de Fluxo
Diagrama Mermaid `flowchart TD` mostrando o fluxo completo do request.

## Diagrama de Sequência
Diagrama Mermaid `sequenceDiagram` mostrando a interação entre componentes.

## Diagrama de Entidades (se aplicável)
Diagrama Mermaid `erDiagram` com entidades e relacionamentos.
```

## Regras Adicionais

1. **Idioma**: Toda a documentação deve ser escrita em **português brasileiro**
2. **Exemplos realistas**: Use dados de exemplo que façam sentido no contexto do domínio (hotel, reservas, quartos, hóspedes)
3. **Completude**: Não omita nenhuma validação, erro ou regra de negócio. Rastreie o código completo
4. **Precisão**: Os tipos de dados, nomes de campos JSON (`@JsonProperty`) e códigos de erro devem corresponder exatamente ao código-fonte
5. **Mermaid**: Todos os diagramas devem usar sintaxe Mermaid válida e renderizável. **IMPORTANTE**: Mermaid NÃO suporta os caracteres `{`, `}` ou `;` dentro de labels de nós. Substitua `{` por `(`, `}` por `)`, e remova `;`. Por exemplo, use `[GET /guests/id]` em vez de `[GET /guests/{id}]`, e `[Map de String, Object]` em vez de `[Map<String, Object>]`
6. **Um arquivo por endpoint**: Nunca combine múltiplos endpoints em um mesmo arquivo
7. **Nomenclatura do arquivo**: Use o padrão `{path_com_underscores}_{MÉTODO}.md` (ex: `guests_by_id_GET.md` para `GET /guests/{id}`)

## Workflow

1. Crie o diretório `docs/endpoints/` se não existir
2. Analise cada Resource class: `GuestResource`, `HotelResource`, `ReservationResource`, `RoomResource`, `WeatherResource`
3. Para cada endpoint, gere o arquivo Markdown completo seguindo o modelo acima
4. Crie também um arquivo `docs/endpoints/README.md` com um índice de todos os endpoints documentados

## Integração com MkDocs

Se existir um arquivo `mkdocs.yml` ou `mkdocs.yaml` na raiz do repositório, atualize-o para incluir os caminhos dos arquivos de documentação gerados na seção `nav:`. Adicione uma entrada para cada endpoint dentro de uma seção apropriada (ex: `API Endpoints`), mantendo a estrutura existente do arquivo intacta.

## Safe Outputs

Quando terminar de gerar toda a documentação:
- Crie um pull request com todos os arquivos gerados usando o safe output `create-pull-request`
- **Se não houve mudanças** (documentação já está atualizada): Use o safe output `noop` com uma mensagem explicando que a documentação já está atualizada
