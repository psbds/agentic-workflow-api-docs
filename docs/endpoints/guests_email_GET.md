# GET /api/v1/guests/email/{email} — Buscar Hóspede por Email

## Descrição
Este endpoint busca um hóspede específico através do endereço de email. Útil para verificar se um cliente já está cadastrado no sistema antes de criar uma nova reserva.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/guests/email/{email}`   |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `guest:email:{email}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache para otimizar buscas repetidas |
| PostgreSQL | JDBC | Consulta do hóspede por email com índice otimizado |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| email | string | Sim | Endereço de email do hóspede |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Guest retrieved successfully",
  "data": {
    "id": 5,
    "firstName": "Maria",
    "lastName": "Silva Santos",
    "email": "maria.santos@email.com",
    "phoneNumber": "+55 11 98765-4321",
    "documentType": "CPF",
    "documentNumber": "123.456.789-00",
    "nationality": "Brasileira"
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | object | Dados do hóspede |
| data.id | long | Identificador único do hóspede |
| data.firstName | string | Primeiro nome do hóspede |
| data.lastName | string | Sobrenome do hóspede |
| data.email | string | Endereço de email (usado na busca) |
| data.phoneNumber | string | Telefone de contato |
| data.documentType | string | Tipo de documento (CPF, RG, Passaporte, etc.) |
| data.documentNumber | string | Número do documento |
| data.nationality | string | Nacionalidade do hóspede |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | GUEST_NOT_FOUND | Hóspede com o email informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar hóspede |

## Regras de Negócio
1. Email é case-insensitive (convertido para lowercase antes da busca)
2. Busca primeiro em cache com chave `guest:email:{email}`
3. Se não encontrado em cache, consulta banco de dados usando índice em email
4. Armazena resultado em cache para futuras consultas
5. Retorna 404 se email não existir no sistema
6. Email deve ser único no sistema (constraint UNIQUE no banco)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | GuestResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | GuestService | Buscar em cache, delegar ao repositório e cachear resultado |
| Repository | GuestRepository | Executar consulta por email com índice otimizado |
| Mapper | GuestMapper | Converter entidade Guest em GuestDto |
| Config | CacheConfig | Gerenciar operações de cache no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/guests/email/maria.santos@email.com]
    B --> C[GuestResource.findByEmail]
    C --> D[email.toLowerCase]
    D --> E[GuestService.findByEmail]
    E --> F{Cache Hit?}
    F -->|Sim| G[Retornar do Cache]
    F -->|Não| H[GuestRepository.findByEmail]
    H --> I{Hóspede existe?}
    I -->|Não| J[NotFoundException → 404]
    I -->|Sim| K[Armazenar no Cache]
    K --> L[GuestMapper.toDto]
    G --> L
    L --> M[ApiResponse.success]
    M --> N[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant GuestResource
    participant GuestService
    participant CacheConfig
    participant GuestRepository
    participant Database
    participant GuestMapper
    
    Cliente->>GuestResource: GET /api/v1/guests/email/maria.santos@email.com
    GuestResource->>GuestResource: email.toLowerCase()
    GuestResource->>GuestService: findByEmail("maria.santos@email.com")
    GuestService->>CacheConfig: getObject("guest:email:maria.santos@email.com")
    alt Cache Hit
        CacheConfig-->>GuestService: Guest
    else Cache Miss
        GuestService->>GuestRepository: findByEmail("maria.santos@email.com")
        GuestRepository->>Database: SELECT * FROM guests WHERE LOWER(email) = ?
        alt Email não existe
            Database-->>GuestRepository: null
            GuestRepository-->>GuestService: Optional.empty
            GuestService-->>GuestResource: NotFoundException
            GuestResource-->>Cliente: 404 GUEST_NOT_FOUND
        else Email existe
            Database-->>GuestRepository: Guest
            GuestRepository-->>GuestService: Guest
            GuestService->>CacheConfig: putObject("guest:email:...", guest, TTL)
        end
    end
    GuestService->>GuestMapper: toDto(guest)
    GuestMapper-->>GuestService: GuestDto
    GuestService-->>GuestResource: GuestDto
    GuestResource-->>Cliente: 200 OK + ApiResponse
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "faz"
    
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
