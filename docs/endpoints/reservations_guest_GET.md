# GET /api/v1/reservations/guest/{guestId} — Listar Reservas por Hóspede

## Descrição
Este endpoint retorna todas as reservas associadas a um hóspede específico. Útil para consultar o histórico completo de reservas de um cliente, incluindo reservas passadas, ativas e futuras.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/reservations/guest/{guestId}` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `reservation:guest:{guestId}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache para histórico do hóspede |
| PostgreSQL | JDBC | Consulta de todas as reservas do hóspede ordenadas por data |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| guestId | long | Sim | Identificador único do hóspede |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Reservations retrieved successfully",
  "data": [
    {
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
    {
      "id": 38,
      "confirmationCode": "HTL-X9Y8Z7W6",
      "checkInDate": "2026-01-10",
      "checkOutDate": "2026-01-15",
      "numberOfGuests": 1,
      "totalPrice": 750.00,
      "status": "CHECKED_OUT",
      "paymentStatus": "PAID",
      "specialRequests": null,
      "weatherChecked": true,
      "weatherSummary": "Partly cloudy - Temp: 22.0°C - Wind: 8.5 km/h",
      "guestId": 5,
      "guestName": "Maria Silva Santos",
      "roomId": 8,
      "roomNumber": "201",
      "hotelName": "Coastal Paradise Resort"
    }
  ],
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | array | Lista de reservas do hóspede |
| data[].id | long | Identificador único da reserva |
| data[].confirmationCode | string | Código de confirmação HTL-XXXXXXXX |
| data[].checkInDate | date | Data de entrada |
| data[].checkOutDate | date | Data de saída |
| data[].numberOfGuests | int | Quantidade de hóspedes |
| data[].totalPrice | decimal | Preço total (ou valor de reembolso se cancelada) |
| data[].status | enum | Status: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, EXPIRED |
| data[].paymentStatus | enum | Status pagamento: PENDING, PAID, REFUNDED, PARTIALLY_REFUNDED |
| data[].specialRequests | string | Solicitações especiais (pode ser null) |
| data[].weatherChecked | boolean | Indica se clima foi verificado |
| data[].weatherSummary | string | Resumo do clima |
| data[].guestId | long | ID do hóspede (sempre igual ao parâmetro) |
| data[].guestName | string | Nome completo do hóspede |
| data[].roomId | long | ID do quarto reservado |
| data[].roomNumber | string | Número do quarto |
| data[].hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | GUEST_NOT_FOUND | Hóspede com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar reservas |

## Regras de Negócio
1. Valida se o hóspede existe antes de buscar reservas
2. Retorna lista vazia se hóspede não possui reservas
3. Resultados ordenados por checkInDate DESC (mais recentes primeiro)
4. Inclui reservas de TODOS os status (ativas, canceladas, expiradas, concluídas)
5. Busca primeiro em cache com chave `reservation:guest:{guestId}`
6. Armazena resultado em cache para futuras consultas
7. Cache é invalidado quando nova reserva do hóspede é criada ou atualizada

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | ReservationService | Validar existência do hóspede, buscar em cache e delegar ao repositório |
| Repository | GuestRepository | Validar existência do hóspede |
| Repository | ReservationRepository | Executar consulta de reservas por guestId com ordenação |
| Mapper | ReservationMapper | Converter lista de entidades em lista de DTOs |
| Config | CacheConfig | Gerenciar operações de cache no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/reservations/guest/5]
    B --> C[ReservationResource.findByGuestId]
    C --> D[ReservationService.findByGuestId]
    D --> E[GuestRepository.findById]
    E --> F{Guest existe?}
    F -->|Não| G[NotFoundException → 404]
    F -->|Sim| H{Cache Hit?}
    H -->|Sim| I[Retornar do Cache]
    H -->|Não| J[ReservationRepository.findByGuestIdOrderByCheckInDesc]
    J --> K[Consulta SQL com ORDER BY]
    K --> L[Armazenar no Cache]
    L --> M[ReservationMapper.toDtoList]
    I --> M
    M --> N[ApiResponse.success]
    N --> O[Retornar 200 OK com lista]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant ReservationService
    participant GuestRepository
    participant CacheConfig
    participant ReservationRepository
    participant Database
    participant ReservationMapper
    
    Cliente->>ReservationResource: GET /api/v1/reservations/guest/5
    ReservationResource->>ReservationService: findByGuestId(5)
    ReservationService->>GuestRepository: findById(5)
    GuestRepository->>Database: SELECT * FROM guests WHERE id = 5
    alt Guest não existe
        Database-->>GuestRepository: null
        GuestRepository-->>ReservationService: Optional.empty
        ReservationService-->>ReservationResource: NotFoundException
        ReservationResource-->>Cliente: 404 GUEST_NOT_FOUND
    else Guest existe
        Database-->>GuestRepository: Guest
        ReservationService->>CacheConfig: getObject("reservation:guest:5")
        alt Cache Hit
            CacheConfig-->>ReservationService: List(Reservation)
        else Cache Miss
            ReservationService->>ReservationRepository: findByGuestIdOrderByCheckInDesc(5)
            ReservationRepository->>Database: SELECT * FROM reservations WHERE guest_id = 5 ORDER BY check_in_date DESC
            Database-->>ReservationRepository: List(Reservation)
            ReservationRepository-->>ReservationService: List(Reservation)
            ReservationService->>CacheConfig: putObject("reservation:guest:5", reservations, TTL)
        end
        ReservationService->>ReservationMapper: toDtoList(reservations)
        ReservationMapper-->>ReservationService: List(ReservationDto)
        ReservationService-->>ReservationResource: List(ReservationDto)
        ReservationResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "possui"
    Reservation ||--|| Room : "reserva"
    Room ||--|| Hotel : "pertence a"
    
    Guest (
        Long id PK,
        String firstName,
        String lastName,
        String email
    )
    
    Reservation (
        Long id PK,
        String confirmationCode,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status,
        Long guestId FK
    )
````
