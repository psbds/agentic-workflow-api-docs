# POST /api/v1/guests — Criar Novo Hóspede

## Descrição
Este endpoint cria um novo hóspede no sistema. Registra informações completas do cliente, incluindo dados pessoais, documentos e contato, necessários para realizar reservas.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/guests`                 |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - limpa cache de buscas por email se existir |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Inserção de novo hóspede no banco de dados |
| Redis | TCP | Invalidação de cache se email já foi consultado anteriormente |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Body
````json
{
  "firstName": "Carlos",
  "lastName": "Mendes Pereira",
  "email": "carlos.pereira@email.com",
  "phoneNumber": "+55 21 91234-5678",
  "documentType": "CPF",
  "documentNumber": "987.654.321-00",
  "nationality": "Brasileira"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| firstName | string | Sim | @NotNull, @NotBlank - Primeiro nome não pode ser vazio |
| lastName | string | Sim | @NotNull, @NotBlank - Sobrenome não pode ser vazio |
| email | string | Sim | @NotNull, @Email - Email válido e único no sistema |
| phoneNumber | string | Não | Telefone de contato (formato livre) |
| documentType | string | Não | Tipo de documento (CPF, RG, Passaporte, CNH, etc.) |
| documentNumber | string | Não | Número do documento |
| nationality | string | Não | Nacionalidade do hóspede |

## Response

### Sucesso — `201 CREATED`
````json
{
  "success": true,
  "message": "Guest created successfully",
  "data": {
    "id": 25,
    "firstName": "Carlos",
    "lastName": "Mendes Pereira",
    "email": "carlos.pereira@email.com",
    "phoneNumber": "+55 21 91234-5678",
    "documentType": "CPF",
    "documentNumber": "987.654.321-00",
    "nationality": "Brasileira"
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem de sucesso |
| data | object | Dados do hóspede criado |
| data.id | long | ID gerado automaticamente pelo banco de dados |
| data.firstName | string | Primeiro nome |
| data.lastName | string | Sobrenome |
| data.email | string | Email (convertido para lowercase antes de salvar) |
| data.phoneNumber | string | Telefone de contato |
| data.documentType | string | Tipo de documento |
| data.documentNumber | string | Número do documento |
| data.nationality | string | Nacionalidade |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos obrigatórios ausentes ou email inválido (Bean Validation) |
| 409 | DUPLICATE_EMAIL | Email já cadastrado no sistema (constraint UNIQUE violada) |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao criar hóspede |

## Regras de Negócio
1. **Email Único**: Sistema valida se email já existe antes de inserir. Se duplicado, lança exceção com código 409
2. **Email Normalizado**: Email é convertido para lowercase antes de salvar para garantir busca case-insensitive
3. **Campos Opcionais**: phoneNumber, documentType, documentNumber e nationality são opcionais
4. **Campos Timestamp**: createdAt e updatedAt são preenchidos automaticamente via @PrePersist
5. **ID Gerado**: ID é gerado automaticamente pelo banco (IDENTITY strategy)
6. **Invalidação de Cache**: Remove cache `guest:email:{email}` se existir
7. **Transação**: Operação executada em transação (@Transactional) garantindo atomicidade

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | GuestResource | Receber requisição, validar com Bean Validation e retornar 201 Created |
| Service | GuestService | Validar unicidade do email, persistir hóspede e invalidar cache |
| Repository | GuestRepository | Executar INSERT no banco de dados via Panache |
| Mapper | GuestMapper | Converter entidade Guest em GuestDto |
| Config | CacheConfig | Invalidar cache do email se existir |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/guests + body JSON]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[GuestResource.createGuest]
    F --> G[GuestService.create]
    G --> H[email.toLowerCase]
    H --> I[GuestRepository.findByEmail]
    I --> J{Email já existe?}
    J -->|Sim| K[DuplicateEmailException → 409]
    J -->|Não| L[@Transactional BEGIN]
    L --> M[GuestRepository.persist]
    M --> N[INSERT INTO guests]
    N --> O[@PrePersist: set createdAt, updatedAt]
    O --> P[CacheConfig.delete - guest:email:...]
    P --> Q[@Transactional COMMIT]
    Q --> R[GuestMapper.toDto]
    R --> S[ApiResponse.success]
    S --> T[Retornar 201 Created]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant GuestResource
    participant Validator
    participant GuestService
    participant GuestRepository
    participant CacheConfig
    participant Database
    participant GuestMapper
    
    Cliente->>GuestResource: POST /api/v1/guests + body
    GuestResource->>Validator: @Valid Guest
    alt Validação Falha
        Validator-->>GuestResource: ValidationException
        GuestResource-->>Cliente: 400 Bad Request
    else Validação OK
        GuestResource->>GuestService: create(guest)
        GuestService->>GuestService: email.toLowerCase()
        GuestService->>GuestRepository: findByEmail(email)
        GuestRepository->>Database: SELECT * FROM guests WHERE email = ?
        alt Email já existe
            Database-->>GuestRepository: Guest existente
            GuestRepository-->>GuestService: Optional(Guest)
            GuestService-->>GuestResource: DuplicateEmailException
            GuestResource-->>Cliente: 409 DUPLICATE_EMAIL
        else Email não existe
            Database-->>GuestRepository: null
            GuestService->>GuestRepository: persist(guest)
            GuestRepository->>Database: INSERT INTO guests
            Database-->>GuestRepository: Guest com ID gerado
            GuestRepository-->>GuestService: Guest
            GuestService->>CacheConfig: delete("guest:email:carlos.pereira@email.com")
            GuestService->>GuestMapper: toDto(guest)
            GuestMapper-->>GuestService: GuestDto
            GuestService-->>GuestResource: GuestDto
            GuestResource-->>Cliente: 201 Created + ApiResponse
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "pode fazer"
    
    Guest (
        Long id PK,
        String firstName,
        String lastName,
        String email UNIQUE,
        String phoneNumber,
        String documentType,
        String documentNumber,
        String nationality,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
