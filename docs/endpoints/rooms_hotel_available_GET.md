# GET /api/v1/rooms/hotel/{hotelId}/available — Listar Quartos Disponíveis por Período

## Descrição
Este endpoint retorna todos os quartos de um hotel que estão disponíveis para reserva em um período específico (check-in e check-out). Valida se há conflitos com reservas existentes.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/rooms/hotel/{hotelId}/available` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Não (consulta dinâmica por datas) |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Consulta complexa verificando disponibilidade por período |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| hotelId | long | Sim | Identificador único do hotel |

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| checkIn | date | Sim | Data de entrada desejada (formato: yyyy-MM-dd) |
| checkOut | date | Sim | Data de saída desejada (formato: yyyy-MM-dd) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Available rooms retrieved successfully",
  "data": [
    {
      "id": 12,
      "roomNumber": "305",
      "roomType": "SUITE",
      "pricePerNight": 150.00,
      "maxOccupancy": 3,
      "description": "Suíte luxuosa com vista para o mar",
      "isAvailable": true,
      "floorNumber": 3,
      "hotelId": 1,
      "hotelName": "Hotel Grand Plaza"
    },
    {
      "id": 14,
      "roomNumber": "401",
      "roomType": "DELUXE",
      "pricePerNight": 200.00,
      "maxOccupancy": 4,
      "description": "Quarto deluxe com varanda privativa",
      "isAvailable": true,
      "floorNumber": 4,
      "hotelId": 1,
      "hotelName": "Hotel Grand Plaza"
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
| data | array | Lista de quartos disponíveis no período |
| data[].id | long | Identificador único do quarto |
| data[].roomNumber | string | Número do quarto |
| data[].roomType | enum | Tipo do quarto |
| data[].pricePerNight | decimal | Preço por diária |
| data[].maxOccupancy | int | Capacidade máxima de hóspedes |
| data[].description | string | Descrição do quarto |
| data[].isAvailable | boolean | Disponibilidade geral do quarto |
| data[].floorNumber | int | Andar do quarto |
| data[].hotelId | long | ID do hotel |
| data[].hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Parâmetros checkIn ou checkOut ausentes ou inválidos |
| 400 | INVALID_DATE_RANGE | checkOut menor ou igual a checkIn |
| 404 | HOTEL_NOT_FOUND | Hotel com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar quartos |

## Regras de Negócio
1. **Validação de Datas**: checkOut deve ser MAIOR que checkIn, caso contrário lança InvalidDateRangeException
2. **Validação de Hotel**: Verifica se o hotel existe antes de buscar quartos
3. **Disponibilidade Geral**: Retorna apenas quartos com isAvailable=true
4. **Verificação de Conflitos**: Exclui quartos que possuem reservas conflitantes no período:
   - Há conflito se: (checkIn < reservation.checkOut) AND (checkOut > reservation.checkIn)
   - Considera apenas reservas com status diferente de CANCELLED e EXPIRED
5. **Lista Vazia**: Retorna array vazio se nenhum quarto disponível (não é erro)
6. **Ordenação**: Resultados ordenados por pricePerNight ASC (mais baratos primeiro)
7. **Sem Cache**: Consulta sempre busca dados atualizados do banco (por ser dinâmica)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição, validar parâmetros e retornar resposta |
| Service | RoomService | Validar datas, validar hotel e delegar consulta complexa |
| Repository | HotelRepository | Validar existência do hotel |
| Repository | RoomRepository | Executar consulta com subquery para excluir quartos com conflitos |
| Repository | ReservationRepository | Auxiliar na verificação de conflitos |
| Mapper | RoomMapper | Converter lista de entidades em lista de DTOs |
| Util | DateUtils | Validar intervalo de datas |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/rooms/hotel/1/available?checkIn=2026-03-15&checkOut=2026-03-18]
    B --> C[RoomResource.findAvailableRooms]
    C --> D{Parâmetros presentes?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F{checkOut > checkIn?}
    F -->|Não| G[InvalidDateRangeException → 400]
    F -->|Sim| H[RoomService.findAvailableRooms]
    H --> I[HotelRepository.findById]
    I --> J{Hotel existe?}
    J -->|Não| K[NotFoundException → 404]
    J -->|Sim| L[RoomRepository.findAvailableByHotelAndPeriod]
    L --> M[SQL: SELECT rooms WHERE isAvailable=true AND NOT EXISTS reservas conflitantes]
    M --> N[Ordenar por pricePerNight ASC]
    N --> O[RoomMapper.toDtoList]
    O --> P[ApiResponse.success]
    P --> Q[Retornar 200 OK com lista]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant RoomService
    participant HotelRepository
    participant RoomRepository
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: GET /api/v1/rooms/hotel/1/available?checkIn=2026-03-15&checkOut=2026-03-18
    RoomResource->>RoomResource: Validar parâmetros presentes
    alt Parâmetros ausentes
        RoomResource-->>Cliente: 400 VALIDATION_ERROR
    else Parâmetros OK
        RoomResource->>RoomResource: Validar checkOut > checkIn
        alt Datas inválidas
            RoomResource-->>Cliente: 400 INVALID_DATE_RANGE
        else Datas válidas
            RoomResource->>RoomService: findAvailableRooms(1, checkIn, checkOut)
            RoomService->>HotelRepository: findById(1)
            HotelRepository->>Database: SELECT * FROM hotels WHERE id = 1
            alt Hotel não existe
                Database-->>HotelRepository: null
                HotelRepository-->>RoomService: Optional.empty
                RoomService-->>RoomResource: NotFoundException
                RoomResource-->>Cliente: 404 HOTEL_NOT_FOUND
            else Hotel existe
                Database-->>HotelRepository: Hotel
                RoomService->>RoomRepository: findAvailableByHotelAndPeriod(1, checkIn, checkOut)
                RoomRepository->>Database: SELECT r.* FROM rooms r WHERE r.hotel_id = 1 AND r.is_available = true AND NOT EXISTS (SELECT 1 FROM reservations res WHERE res.room_id = r.id AND res.status NOT IN (CANCELLED, EXPIRED) AND res.check_in_date < 2026-03-18 AND res.check_out_date > 2026-03-15) ORDER BY r.price_per_night
                Database-->>RoomRepository: List(Room)
                RoomRepository-->>RoomService: List(Room)
                RoomService->>RoomMapper: toDtoList(rooms)
                RoomMapper-->>RoomService: List(RoomDto)
                RoomService-->>RoomResource: List(RoomDto)
                RoomResource-->>Cliente: 200 OK + ApiResponse
            end
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Hotel ||--o( Room : "possui"
    Room ||--o( Reservation : "tem reservas"
    
    Room (
        Long id PK,
        String roomNumber,
        boolean isAvailable,
        BigDecimal pricePerNight,
        Long hotelId FK
    )
    
    Reservation (
        Long id PK,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status "NOT IN CANCELLED EXPIRED",
        Long roomId FK
    )
````
