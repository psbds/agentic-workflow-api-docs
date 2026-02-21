# GET /api/v1/hotels — Listar Hotéis

## Descrição
Este endpoint retorna uma lista paginada de todos os hotéis cadastrados no sistema. Permite consultar informações gerais sobre os hotéis, incluindo nome, localização, classificação por estrelas e dados de contato.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/hotels`                 |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `hotel:all:{page}:{size}`, TTL configurável via `hotel.cache-ttl-minutes` |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis   | TCP       | Armazenamento em cache para otimizar consultas repetidas |
| PostgreSQL | JDBC   | Consulta paginada de hotéis no banco de dados |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Valor Padrão | Descrição |
|-----------|------|-------------|--------------|-----------|
| page      | int  | Não         | 0            | Número da página (inicia em 0) |
| size      | int  | Não         | 20           | Quantidade de registros por página |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Hotels retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Hotel Grand Plaza",
      "address": "Av. Paulista, 1000",
      "city": "São Paulo",
      "country": "Brasil",
      "starRating": 5,
      "description": "Hotel de luxo no coração de São Paulo",
      "latitude": -23.561684,
      "longitude": -46.655981,
      "phoneNumber": "+55 11 3000-0000",
      "email": "contato@grandplaza.com.br",
      "totalRooms": 150
    },
    {
      "id": 2,
      "name": "Coastal Paradise Resort",
      "address": "Av. Atlântica, 500",
      "city": "Rio de Janeiro",
      "country": "Brasil",
      "starRating": 4,
      "description": "Resort à beira-mar com vista para o oceano",
      "latitude": -22.970722,
      "longitude": -43.182365,
      "phoneNumber": "+55 21 2500-0000",
      "email": "reservas@coastalparadise.com.br",
      "totalRooms": 200
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
| data | array | Lista de hotéis |
| data[].id | long | Identificador único do hotel |
| data[].name | string | Nome do hotel |
| data[].address | string | Endereço completo |
| data[].city | string | Cidade onde o hotel está localizado |
| data[].country | string | País onde o hotel está localizado |
| data[].starRating | int | Classificação em estrelas (1-5) |
| data[].description | string | Descrição detalhada do hotel |
| data[].latitude | double | Coordenada de latitude para geolocalização |
| data[].longitude | double | Coordenada de longitude para geolocalização |
| data[].phoneNumber | string | Telefone de contato |
| data[].email | string | E-mail de contato |
| data[].totalRooms | int | Número total de quartos do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 500         | INTERNAL_ERROR | Erro interno do servidor ao buscar hotéis |

## Regras de Negócio
1. A paginação inicia em 0 (zero-indexed)
2. O tamanho padrão da página é 20 registros
3. Resultados são armazenados em cache por período configurável (padrão: minutos definidos em `hotel.cache-ttl-minutes`)
4. A lista inclui todos os hotéis, independentemente de disponibilidade de quartos
5. O campo `totalRooms` é calculado dinamicamente com base nos quartos cadastrados para o hotel

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição HTTP, validar parâmetros de paginação e retornar resposta padronizada |
| Service | HotelService | Coordenar lógica de negócio, gerenciar cache e delegar consulta ao repositório |
| Repository | HotelRepository | Executar consulta paginada no banco de dados |
| Mapper | HotelMapper | Converter entidades Hotel em HotelDto para serialização |
| Config | CacheConfig | Gerenciar operações de cache (get/put) no Redis |
| Config | HotelConfig | Fornecer configurações como TTL do cache |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/hotels?page=0&size=20]
    B --> C[HotelResource.listHotels]
    C --> D[HotelService.findAll]
    D --> E{Cache Hit?}
    E -->|Sim| F[Retornar do Cache]
    E -->|Não| G[HotelRepository.findAllPaged]
    G --> H[Consulta SQL Paginada]
    H --> I[Armazenar no Cache]
    I --> J[HotelMapper.toDtoList]
    F --> J
    J --> K[ApiResponse.success]
    K --> L[Retornar 200 OK com lista de hotéis]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant HotelResource
    participant HotelService
    participant CacheConfig
    participant HotelRepository
    participant HotelMapper
    participant Database
    
    Cliente->>HotelResource: GET /api/v1/hotels?page=0&size=20
    HotelResource->>HotelService: findAll(0, 20)
    HotelService->>CacheConfig: getObject("hotel:all:0:20")
    alt Cache Hit
        CacheConfig-->>HotelService: List(Hotel)
    else Cache Miss
        HotelService->>HotelRepository: findAllPaged(0, 20)
        HotelRepository->>Database: SELECT * FROM hotels LIMIT 20 OFFSET 0
        Database-->>HotelRepository: List(Hotel)
        HotelRepository-->>HotelService: List(Hotel)
        HotelService->>CacheConfig: putObject("hotel:all:0:20", hotels, TTL)
    end
    HotelService->>HotelMapper: toDtoList(hotels)
    HotelMapper-->>HotelService: List(HotelDto)
    HotelService-->>HotelResource: List(HotelDto)
    HotelResource-->>Cliente: 200 OK + ApiResponse(success, data, message)
````

## Diagrama de Entidades
````mermaid
erDiagram
    Hotel ||--o( Room : "possui"
    
    Hotel (
        Long id PK,
        String name,
        String address,
        String city,
        String country,
        int starRating,
        String description,
        Double latitude,
        Double longitude,
        String phoneNumber,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
        Long hotelId FK,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
