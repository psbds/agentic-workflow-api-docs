# GET /api/v1/rooms/hotel/{hotelId} — Listar Quartos por Hotel

## Descrição
Este endpoint retorna todos os quartos cadastrados em um hotel específico. Inclui informações de tipo, preço, capacidade e disponibilidade de cada quarto.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/rooms/hotel/{hotelId}`  |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `room:hotel:{hotelId}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache para otimizar buscas repetidas |
| PostgreSQL | JDBC | Consulta de todos os quartos do hotel com join para Hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| hotelId | long | Sim | Identificador único do hotel |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Rooms retrieved successfully",
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
      "id": 13,
      "roomNumber": "306",
      "roomType": "DOUBLE",
      "pricePerNight": 100.00,
      "maxOccupancy": 2,
      "description": "Quarto duplo padrão",
      "isAvailable": false,
      "floorNumber": 3,
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
| data | array | Lista de quartos do hotel |
| data[].id | long | Identificador único do quarto |
| data[].roomNumber | string | Número identificador do quarto |
| data[].roomType | enum | Tipo: SINGLE, DOUBLE, SUITE, DELUXE, PENTHOUSE |
| data[].pricePerNight | decimal | Preço por diária |
| data[].maxOccupancy | int | Capacidade máxima de hóspedes |
| data[].description | string | Descrição detalhada do quarto |
| data[].isAvailable | boolean | Indica se quarto está disponível |
| data[].floorNumber | int | Andar do quarto |
| data[].hotelId | long | ID do hotel (igual ao parâmetro) |
| data[].hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | HOTEL_NOT_FOUND | Hotel com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar quartos |

## Regras de Negócio
1. **Validação de Hotel**: Verifica se o hotel existe antes de buscar quartos, caso contrário lança NotFoundException
2. **Lista Vazia**: Retorna array vazio se hotel não possui quartos cadastrados (não é erro 404)
3. **Ordenação**: Resultados ordenados por floorNumber ASC, roomNumber ASC
4. **Busca em Cache**: Consulta cache com chave `room:hotel:{hotelId}` primeiro
5. **Armazenamento em Cache**: Cacheia resultado por TTL configurável
6. **Disponibilidade**: O campo isAvailable reflete apenas a disponibilidade do quarto (não considera reservas)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | RoomService | Validar hotel, buscar em cache e delegar ao repositório |
| Repository | HotelRepository | Validar existência do hotel |
| Repository | RoomRepository | Executar consulta de quartos por hotelId |
| Mapper | RoomMapper | Converter lista de entidades em lista de DTOs |
| Config | CacheConfig | Gerenciar operações de cache no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/rooms/hotel/1]
    B --> C[RoomResource.findByHotelId]
    C --> D[RoomService.findByHotelId]
    D --> E[HotelRepository.findById]
    E --> F{Hotel existe?}
    F -->|Não| G[NotFoundException → 404]
    F -->|Sim| H{Cache Hit?}
    H -->|Sim| I[Retornar do Cache]
    H -->|Não| J[RoomRepository.findByHotelIdOrderByFloorNumberAscRoomNumberAsc]
    J --> K[Consulta SQL com JOIN Hotel]
    K --> L[Armazenar no Cache]
    L --> M[RoomMapper.toDtoList]
    I --> M
    M --> N[ApiResponse.success]
    N --> O[Retornar 200 OK com lista]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant RoomService
    participant HotelRepository
    participant CacheConfig
    participant RoomRepository
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: GET /api/v1/rooms/hotel/1
    RoomResource->>RoomService: findByHotelId(1)
    RoomService->>HotelRepository: findById(1)
    HotelRepository->>Database: SELECT * FROM hotels WHERE id = 1
    alt Hotel não existe
        Database-->>HotelRepository: null
        HotelRepository-->>RoomService: Optional.empty
        RoomService-->>RoomResource: NotFoundException
        RoomResource-->>Cliente: 404 HOTEL_NOT_FOUND
    else Hotel existe
        Database-->>HotelRepository: Hotel
        RoomService->>CacheConfig: getObject("room:hotel:1")
        alt Cache Hit
            CacheConfig-->>RoomService: List(Room)
        else Cache Miss
            RoomService->>RoomRepository: findByHotelIdOrderBy(1)
            RoomRepository->>Database: SELECT r.*, h.name FROM rooms r JOIN hotels h WHERE r.hotel_id = 1 ORDER BY floor, room_number
            Database-->>RoomRepository: List(Room)
            RoomRepository-->>RoomService: List(Room)
            RoomService->>CacheConfig: putObject("room:hotel:1", rooms, TTL)
        end
        RoomService->>RoomMapper: toDtoList(rooms)
        RoomMapper-->>RoomService: List(RoomDto)
        RoomService-->>RoomResource: List(RoomDto)
        RoomResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Hotel ||--o( Room : "possui"
    
    Hotel (
        Long id PK,
        String name
    )
    
    Room (
        Long id PK,
        String roomNumber,
        RoomType roomType,
        BigDecimal pricePerNight,
        int maxOccupancy,
        String description,
        boolean isAvailable,
        int floorNumber,
        Long hotelId FK
    )
````
