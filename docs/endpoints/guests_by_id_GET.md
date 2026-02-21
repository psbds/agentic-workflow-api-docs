# GET /api/v1/guests/{id} — Buscar Hóspede por ID

## Descrição
Este endpoint retorna informações detalhadas de um hóspede específico com base em seu identificador único. É utilizado para visualizar dados pessoais, documentação e informações de contato do hóspede.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/guests/{id}`            |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Não                              |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Consulta de hóspede específico no banco de dados |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id        | long | Sim         | Identificador único do hóspede |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Guest retrieved successfully",
  "data": {
    "id": 1,
    "firstName": "João",
    "lastName": "Silva",
    "email": "joao.silva@example.com",
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
| data.firstName | string | Primeiro nome |
| data.lastName | string | Sobrenome |
| data.email | string | E-mail único do hóspede |
| data.phoneNumber | string | Telefone de contato |
| data.documentType | string | Tipo de documento (CPF, RG, Passaporte, etc.) |
| data.documentNumber | string | Número do documento |
| data.nationality | string | Nacionalidade |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404         | NOT_FOUND      | Hóspede não encontrado com o ID fornecido |
| 500         | INTERNAL_ERROR | Erro interno do servidor ao buscar hóspede |

## Regras de Negócio
1. O ID do hóspede deve ser um número inteiro positivo
2. Se o hóspede não for encontrado, uma exceção `NotFoundException` é lançada
3. Dados sensíveis como documentos não são filtrados nesta versão da API
4. Não há cache implementado para este endpoint

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | GuestResource | Receber requisição HTTP e extrair parâmetro de path |
| Service | GuestService | Buscar hóspede e lançar exceção caso não encontrado |
| Repository | GuestRepository | Executar consulta por ID no banco de dados |
| Mapper | GuestMapper | Converter entidade Guest em GuestDto |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/guests/1]
    B --> C[GuestResource.getGuest]
    C --> D[GuestService.findById]
    D --> E[GuestRepository.findById]
    E --> F{Guest Encontrado?}
    F -->|Não| G[Lançar NotFoundException]
    G --> H[Retornar 404 Not Found]
    F -->|Sim| I[GuestMapper.toDto]
    I --> J[ApiResponse.success]
    J --> K[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant GuestResource
    participant GuestService
    participant GuestRepository
    participant GuestMapper
    participant Database
    
    Cliente->>GuestResource: GET /api/v1/guests/1
    GuestResource->>GuestService: findById(1)
    GuestService->>GuestRepository: findById(1)
    GuestRepository->>Database: SELECT * FROM guests WHERE id = 1
    alt Guest Não Encontrado
        Database-->>GuestRepository: null
        GuestRepository-->>GuestService: null
        GuestService-->>GuestResource: throw NotFoundException
        GuestResource-->>Cliente: 404 Not Found
    else Guest Encontrado
        Database-->>GuestRepository: Guest
        GuestRepository-->>GuestService: Guest
        GuestService->>GuestMapper: toDto(guest)
        GuestMapper-->>GuestService: GuestDto
        GuestService-->>GuestResource: GuestDto
        GuestResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "realiza"
    
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
    
    Reservation (
        Long id PK,
        String confirmationCode,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        Long guestId FK,
        Long roomId FK,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
