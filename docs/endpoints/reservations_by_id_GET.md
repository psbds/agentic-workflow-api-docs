# GET /api/v1/reservations/{id} — Buscar Reserva por ID

## Descrição
Este endpoint retorna os detalhes completos de uma reserva específica através do seu identificador único. Inclui informações do hóspede, quarto, hotel, status da reserva, pagamento e resumo climático.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/reservations/{id}`      |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `reservation:{id}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache para otimizar buscas repetidas |
| PostgreSQL | JDBC | Consulta da reserva com joins para Guest, Room e Hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único da reserva |

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
| data.confirmationCode | string | Código de confirmação formato HTL-XXXXXXXX |
| data.checkInDate | date | Data de entrada (formato: yyyy-MM-dd) |
| data.checkOutDate | date | Data de saída (formato: yyyy-MM-dd) |
| data.numberOfGuests | int | Quantidade de hóspedes |
| data.totalPrice | decimal | Preço total da reserva |
| data.status | enum | Status atual: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, EXPIRED |
| data.paymentStatus | enum | Status do pagamento: PENDING, PAID, REFUNDED, PARTIALLY_REFUNDED |
| data.specialRequests | string | Solicitações especiais do hóspede |
| data.weatherChecked | boolean | Indica se verificação climática foi executada |
| data.weatherSummary | string | Resumo do clima no check-in |
| data.guestId | long | ID do hóspede |
| data.guestName | string | Nome completo do hóspede |
| data.roomId | long | ID do quarto reservado |
| data.roomNumber | string | Número do quarto |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | RESERVATION_NOT_FOUND | Reserva com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar reserva |

## Regras de Negócio
1. Busca primeiro em cache com chave `reservation:{id}`
2. Se não encontrado em cache, consulta banco de dados com eager fetch de Guest, Room e Hotel
3. Armazena resultado em cache por TTL configurável para futuras consultas
4. Retorna 404 se reserva não existir
5. O campo `guestName` é concatenação de `guest.firstName` + `guest.lastName`
6. O campo `hotelName` é obtido através do relacionamento Room → Hotel

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | ReservationService | Buscar em cache, delegar ao repositório e cachear resultado |
| Repository | ReservationRepository | Executar consulta no banco com joins |
| Mapper | ReservationMapper | Converter entidade Reservation em ReservationDto |
| Config | CacheConfig | Gerenciar operações de cache (get/put) no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/reservations/42]
    B --> C[ReservationResource.findById]
    C --> D[ReservationService.findById]
    D --> E{Cache Hit?}
    E -->|Sim| F[Retornar do Cache]
    E -->|Não| G[ReservationRepository.findById]
    G --> H{Reserva existe?}
    H -->|Não| I[ReservationNotFoundException → 404]
    H -->|Sim| J[Fetch Guest, Room, Hotel]
    J --> K[Armazenar no Cache]
    K --> L[ReservationMapper.toDto]
    F --> L
    L --> M[ApiResponse.success]
    M --> N[Retornar 200 OK]
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
    
    Cliente->>ReservationResource: GET /api/v1/reservations/42
    ReservationResource->>ReservationService: findById(42)
    ReservationService->>CacheConfig: getObject("reservation:42")
    alt Cache Hit
        CacheConfig-->>ReservationService: Reservation
    else Cache Miss
        ReservationService->>ReservationRepository: findById(42)
        ReservationRepository->>Database: SELECT r.*, g.*, ro.*, h.* FROM reservations r JOIN...
        alt Reserva não existe
            Database-->>ReservationRepository: null
            ReservationRepository-->>ReservationService: Optional.empty
            ReservationService-->>ReservationResource: ReservationNotFoundException
            ReservationResource-->>Cliente: 404 RESERVATION_NOT_FOUND
        else Reserva existe
            Database-->>ReservationRepository: Reservation com Guest, Room, Hotel
            ReservationRepository-->>ReservationService: Reservation
            ReservationService->>CacheConfig: putObject("reservation:42", reservation, TTL)
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
    Guest ||--o( Reservation : "faz"
    Room ||--o( Reservation : "é reservado por"
    Hotel ||--o( Room : "possui"
    
    Reservation (
        Long id PK,
        String confirmationCode,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        String specialRequests,
        boolean weatherChecked,
        String weatherSummary,
        Long guestId FK,
        Long roomId FK
    )
    
    Guest (
        Long id PK,
        String firstName,
        String lastName,
        String email
    )
    
    Room (
        Long id PK,
        String roomNumber,
        Long hotelId FK
    )
    
    Hotel (
        Long id PK,
        String name
    )
````
