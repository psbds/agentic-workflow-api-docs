# GET /api/v1/reservations/confirmation/{code} — Buscar Reserva por Código de Confirmação

## Descrição
Este endpoint permite buscar uma reserva através do código de confirmação único gerado no momento da criação. É útil para hóspedes que desejam consultar sua reserva usando apenas o código recebido por email ou SMS.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/reservations/confirmation/{code}` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `reservation:code:{code}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache usando código de confirmação |
| PostgreSQL | JDBC | Consulta da reserva por confirmationCode com joins |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| code | string | Sim | Código de confirmação no formato HTL-XXXXXXXX |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Reservation retrieved successfully",
  "data": {
    "id": 42,
    "confirmationCode": "HTL-A1B2C3D4",
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-18",
    "numberOfGuests": 2,
    "totalPrice": 450.00,
    "status": "CONFIRMED",
    "paymentStatus": "PAID",
    "specialRequests": "Quarto com vista para o mar",
    "weatherChecked": true,
    "weatherSummary": "Clear sky - Temp: 24.5°C - Wind: 12.3 km/h",
    "guestId": 5,
    "guestName": "Maria Silva Santos",
    "roomId": 12,
    "roomNumber": "305",
    "hotelName": "Hotel Grand Plaza"
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | object | Dados da reserva |
| data.id | long | Identificador único da reserva |
| data.confirmationCode | string | Código de confirmação fornecido na busca |
| data.checkInDate | date | Data de entrada |
| data.checkOutDate | date | Data de saída |
| data.numberOfGuests | int | Quantidade de hóspedes |
| data.totalPrice | decimal | Preço total da reserva |
| data.status | enum | Status atual da reserva |
| data.paymentStatus | enum | Status do pagamento |
| data.specialRequests | string | Solicitações especiais |
| data.weatherChecked | boolean | Indica se clima foi verificado |
| data.weatherSummary | string | Resumo do clima |
| data.guestId | long | ID do hóspede |
| data.guestName | string | Nome completo do hóspede |
| data.roomId | long | ID do quarto |
| data.roomNumber | string | Número do quarto |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | RESERVATION_NOT_FOUND | Reserva com o código de confirmação informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar reserva |

## Regras de Negócio
1. Código de confirmação é case-insensitive (convertido para uppercase antes da busca)
2. Busca primeiro em cache com chave `reservation:code:{code}`
3. Se não encontrado em cache, consulta banco de dados usando índice em confirmationCode
4. Armazena resultado em cache para futuras consultas
5. Retorna 404 se código não existir no sistema
6. Códigos seguem formato HTL-XXXXXXXX onde X são caracteres hexadecimais

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | ReservationService | Buscar em cache, delegar ao repositório e cachear resultado |
| Repository | ReservationRepository | Executar consulta por confirmationCode com índice otimizado |
| Mapper | ReservationMapper | Converter entidade Reservation em ReservationDto |
| Config | CacheConfig | Gerenciar operações de cache no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/reservations/confirmation/HTL-A1B2C3D4]
    B --> C[ReservationResource.findByConfirmationCode]
    C --> D[code.toUpperCase]
    D --> E[ReservationService.findByConfirmationCode]
    E --> F{Cache Hit?}
    F -->|Sim| G[Retornar do Cache]
    F -->|Não| H[ReservationRepository.findByConfirmationCode]
    H --> I{Reserva existe?}
    I -->|Não| J[ReservationNotFoundException → 404]
    I -->|Sim| K[Fetch Guest, Room, Hotel]
    K --> L[Armazenar no Cache]
    L --> M[ReservationMapper.toDto]
    G --> M
    M --> N[ApiResponse.success]
    N --> O[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant ReservationService
    participant CacheConfig
    participant ReservationRepository
    participant Database
    participant ReservationMapper
    
    Cliente->>ReservationResource: GET /api/v1/reservations/confirmation/HTL-A1B2C3D4
    ReservationResource->>ReservationResource: code.toUpperCase()
    ReservationResource->>ReservationService: findByConfirmationCode("HTL-A1B2C3D4")
    ReservationService->>CacheConfig: getObject("reservation:code:HTL-A1B2C3D4")
    alt Cache Hit
        CacheConfig-->>ReservationService: Reservation
    else Cache Miss
        ReservationService->>ReservationRepository: findByConfirmationCode("HTL-A1B2C3D4")
        ReservationRepository->>Database: SELECT * FROM reservations WHERE confirmation_code = ?
        alt Código não existe
            Database-->>ReservationRepository: null
            ReservationRepository-->>ReservationService: Optional.empty
            ReservationService-->>ReservationResource: ReservationNotFoundException
            ReservationResource-->>Cliente: 404 RESERVATION_NOT_FOUND
        else Código existe
            Database-->>ReservationRepository: Reservation
            ReservationRepository-->>ReservationService: Reservation
            ReservationService->>CacheConfig: putObject("reservation:code:...", reservation, TTL)
        end
    end
    ReservationService->>ReservationMapper: toDto(reservation)
    ReservationMapper-->>ReservationService: ReservationDto
    ReservationService-->>ReservationResource: ReservationDto
    ReservationResource-->>Cliente: 200 OK + ApiResponse
````

## Diagrama de Entidades
````mermaid
erDiagram
    Reservation ||--|| Guest : "pertence a"
    Reservation ||--|| Room : "reserva"
    Room ||--|| Hotel : "pertence a"
    
    Reservation (
        Long id PK,
        String confirmationCode UNIQUE,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status,
        Long guestId FK,
        Long roomId FK
    )
````
