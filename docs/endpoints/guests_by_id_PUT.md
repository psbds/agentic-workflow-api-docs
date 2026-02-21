# PUT /api/v1/guests/{id} — Atualizar Hóspede

## Descrição
Este endpoint atualiza as informações de um hóspede existente no sistema. Permite modificar dados pessoais, contato e documentos do cliente.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `PUT`                            |
| **Path**             | `/api/v1/guests/{id}`            |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove caches do hóspede |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Atualização dos dados do hóspede |
| Redis | TCP | Invalidação de cache por ID e email |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único do hóspede a ser atualizado |

### Body
````json
{
  "firstName": "Carlos Alberto",
  "lastName": "Mendes Pereira Junior",
  "email": "carlos.pereira.jr@email.com",
  "phoneNumber": "+55 21 91234-9999",
  "documentType": "Passaporte",
  "documentNumber": "BR123456",
  "nationality": "Brasileira"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| firstName | string | Sim | @NotNull, @NotBlank - Primeiro nome não pode ser vazio |
| lastName | string | Sim | @NotNull, @NotBlank - Sobrenome não pode ser vazio |
| email | string | Sim | @NotNull, @Email - Email válido (deve ser único) |
| phoneNumber | string | Não | Telefone de contato atualizado |
| documentType | string | Não | Tipo de documento atualizado |
| documentNumber | string | Não | Número do documento atualizado |
| nationality | string | Não | Nacionalidade atualizada |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Guest updated successfully",
  "data": {
    "id": 25,
    "firstName": "Carlos Alberto",
    "lastName": "Mendes Pereira Junior",
    "email": "carlos.pereira.jr@email.com",
    "phoneNumber": "+55 21 91234-9999",
    "documentType": "Passaporte",
    "documentNumber": "BR123456",
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
| data | object | Dados do hóspede atualizado |
| data.id | long | ID do hóspede (não muda) |
| data.firstName | string | Primeiro nome atualizado |
| data.lastName | string | Sobrenome atualizado |
| data.email | string | Email atualizado (lowercase) |
| data.phoneNumber | string | Telefone atualizado |
| data.documentType | string | Tipo de documento atualizado |
| data.documentNumber | string | Número do documento atualizado |
| data.nationality | string | Nacionalidade atualizada |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos obrigatórios ausentes ou email inválido |
| 404 | GUEST_NOT_FOUND | Hóspede com o ID informado não existe |
| 409 | DUPLICATE_EMAIL | Novo email já pertence a outro hóspede |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao atualizar hóspede |

## Regras de Negócio
1. **Validação de Existência**: Verifica se o hóspede existe, caso contrário lança NotFoundException
2. **Email Único**: Se email for alterado, valida se novo email já existe em outro hóspede. Se sim, lança DuplicateEmailException (409)
3. **Email Normalizado**: Novo email é convertido para lowercase antes de salvar
4. **Atualização Completa**: PUT substitui TODOS os campos (não é PATCH)
5. **ID Imutável**: ID do hóspede não pode ser alterado
6. **Campo updatedAt**: Atualizado automaticamente via @PreUpdate
7. **Invalidação de Cache Dupla**: Remove cache do ID (guest:id) E dos emails (antigo e novo)
8. **Impacto em Reservas**: Reservas existentes mantêm referência ao hóspede atualizado (FK permanece válida)
9. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | GuestResource | Receber requisição, validar e retornar 200 OK |
| Service | GuestService | Validar existência, validar unicidade de email, atualizar e invalidar cache |
| Repository | GuestRepository | Persistir atualização no banco de dados |
| Mapper | GuestMapper | Converter entidade atualizada em DTO |
| Config | CacheConfig | Invalidar caches relacionados |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[PUT /api/v1/guests/25 + body]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[GuestResource.updateGuest]
    F --> G[GuestService.update]
    G --> H[GuestRepository.findById]
    H --> I{Hóspede existe?}
    I -->|Não| J[NotFoundException → 404]
    I -->|Sim| K[Salvar email antigo]
    K --> L[newEmail.toLowerCase]
    L --> M{Email mudou?}
    M -->|Sim| N[GuestRepository.findByEmail - novo]
    N --> O{Novo email já existe em outro?}
    O -->|Sim| P[DuplicateEmailException → 409]
    O -->|Não| Q[Atualizar todos os campos]
    M -->|Não| Q
    Q --> R[@Transactional: persist update]
    R --> S[@PreUpdate: set updatedAt]
    S --> T[CacheConfig.delete - guest:id:25]
    T --> U[CacheConfig.delete - guest:email:antigo]
    U --> V[CacheConfig.delete - guest:email:novo]
    V --> W[GuestMapper.toDto]
    W --> X[ApiResponse.success]
    X --> Y[Retornar 200 OK]
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
    
    Cliente->>GuestResource: PUT /api/v1/guests/25 + body
    GuestResource->>Validator: @Valid Guest
    alt Validação Falha
        Validator-->>GuestResource: ValidationException
        GuestResource-->>Cliente: 400 Bad Request
    else Validação OK
        GuestResource->>GuestService: update(25, guestData)
        GuestService->>GuestRepository: findById(25)
        GuestRepository->>Database: SELECT * FROM guests WHERE id = 25
        alt Hóspede não existe
            Database-->>GuestRepository: null
            GuestRepository-->>GuestService: Optional.empty
            GuestService-->>GuestResource: NotFoundException
            GuestResource-->>Cliente: 404 GUEST_NOT_FOUND
        else Hóspede existe
            Database-->>GuestRepository: Guest (email antigo)
            GuestRepository-->>GuestService: Guest
            GuestService->>GuestService: oldEmail = guest.email
            GuestService->>GuestService: newEmail.toLowerCase()
            alt Email mudou
                GuestService->>GuestRepository: findByEmail(newEmail)
                GuestRepository->>Database: SELECT * WHERE email = novo AND id != 25
                alt Novo email já existe
                    Database-->>GuestRepository: Guest diferente
                    GuestRepository-->>GuestService: Optional(Guest)
                    GuestService-->>GuestResource: DuplicateEmailException
                    GuestResource-->>Cliente: 409 DUPLICATE_EMAIL
                else Novo email disponível
                    Database-->>GuestRepository: null
                end
            end
            GuestService->>GuestService: Atualizar campos do guest
            GuestService->>GuestRepository: persist(guest)
            GuestRepository->>Database: UPDATE guests SET...
            Database-->>GuestRepository: Guest atualizado
            GuestService->>CacheConfig: delete("guest:id:25")
            GuestService->>CacheConfig: delete("guest:email:OLD")
            GuestService->>CacheConfig: delete("guest:email:NEW")
            GuestService->>GuestMapper: toDto(guest)
            GuestMapper-->>GuestService: GuestDto
            GuestService-->>GuestResource: GuestDto
            GuestResource-->>Cliente: 200 OK + ApiResponse
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "mantém referência"
    
    Guest (
        Long id PK IMMUTABLE,
        String firstName UPDATABLE,
        String lastName UPDATABLE,
        String email UPDATABLE_UNIQUE,
        String phoneNumber UPDATABLE,
        String documentType UPDATABLE,
        String documentNumber UPDATABLE,
        String nationality UPDATABLE,
        LocalDateTime createdAt IMMUTABLE,
        LocalDateTime updatedAt AUTO_UPDATED
    )
````
