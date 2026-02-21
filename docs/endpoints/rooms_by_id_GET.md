# GET /api/v1/rooms/{id} — Buscar Quarto por ID

## Descrição
Este endpoint retorna os detalhes completos de um quarto específico através do seu identificador único. Inclui informações do hotel associado.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/rooms/{id}`             |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `room:{id}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis | TCP | Consulta em cache para otimizar buscas repetidas |
| PostgreSQL | JDBC | Consulta do quarto com join para Hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único do quarto |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Room retrieved successfully",
  "data": {
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
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | object | Dados do quarto |
| data.id | long | Identificador único do quarto |
| data.roomNumber | string | Número do quarto |
| data.roomType | enum | Tipo: SINGLE, DOUBLE, SUITE, DELUXE, PENTHOUSE |
| data.pricePerNight | decimal | Preço por diária |
| data.maxOccupancy | int | Capacidade máxima de hóspedes |
| data.description | string | Descrição detalhada |
| data.isAvailable | boolean | Disponibilidade |
| data.floorNumber | int | Andar do quarto |
| data.hotelId | long | ID do hotel |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | ROOM_NOT_FOUND | Quarto com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar quarto |

## Regras de Negócio
1. Busca primeiro em cache com chave `room:{id}`
2. Se não encontrado em cache, consulta banco de dados com eager fetch de Hotel
3. Armazena resultado em cache por TTL configurável
4. Retorna 404 se quarto não existir
5. O campo hotelName é obtido através do relacionamento Room → Hotel

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição HTTP e retornar resposta padronizada |
| Service | RoomService | Buscar em cache, delegar ao repositório e cachear resultado |
| Repository | RoomRepository | Executar consulta no banco com join |
| Mapper | RoomMapper | Converter entidade Room em RoomDto |
| Config | CacheConfig | Gerenciar operações de cache no Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/rooms/12]
    B --> C[RoomResource.findById]
    C --> D[RoomService.findById]
    D --> E{Cache Hit?}
    E -->|Sim| F[Retornar do Cache]
    E -->|Não| G[RoomRepository.findById]
    G --> H{Quarto existe?}
    H -->|Não| I[NotFoundException → 404]
    H -->|Sim| J[Fetch Hotel]
    J --> K[Armazenar no Cache]
    K --> L[RoomMapper.toDto]
    F --> L
    L --> M[ApiResponse.success]
    M --> N[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant RoomService
    participant CacheConfig
    participant RoomRepository
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: GET /api/v1/rooms/12
    RoomResource->>RoomService: findById(12)
    RoomService->>CacheConfig: getObject("room:12")
    alt Cache Hit
        CacheConfig-->>RoomService: Room
    else Cache Miss
        RoomService->>RoomRepository: findById(12)
        RoomRepository->>Database: SELECT r.*, h.name FROM rooms r JOIN hotels h WHERE r.id = 12
        alt Quarto não existe
            Database-->>RoomRepository: null
            RoomRepository-->>RoomService: Optional.empty
            RoomService-->>RoomResource: NotFoundException
            RoomResource-->>Cliente: 404 ROOM_NOT_FOUND
        else Quarto existe
            Database-->>RoomRepository: Room com Hotel
            RoomRepository-->>RoomService: Room
            RoomService->>CacheConfig: putObject("room:12", room, TTL)
        end
    end
    RoomService->>RoomMapper: toDto(room)
    RoomMapper-->>RoomService: RoomDto
    RoomService-->>RoomResource: RoomDto
    RoomResource-->>Cliente: 200 OK + ApiResponse
````

## Diagrama de Entidades
````mermaid
erDiagram
    Room ||--|| Hotel : "pertence a"
    
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
    
    Hotel (
        Long id PK,
        String name
    )
````
