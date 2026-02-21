# GET /api/v1/hotels/{id} — Buscar Hotel por ID

## Descrição
Este endpoint retorna informações detalhadas de um hotel específico com base em seu identificador único. É utilizado para visualizar todos os dados de um hotel, incluindo localização, classificação, contato e total de quartos disponíveis.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/hotels/{id}`            |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `hotel:{id}`, TTL configurável via `hotel.cache-ttl-minutes` |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Redis   | TCP       | Armazenamento em cache para otimizar buscas repetidas |
| PostgreSQL | JDBC   | Consulta de hotel específico no banco de dados |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id        | long | Sim         | Identificador único do hotel |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Hotel retrieved successfully",
  "data": {
    "id": 1,
    "name": "Hotel Grand Plaza",
    "address": "Av. Paulista, 1000",
    "city": "São Paulo",
    "country": "Brasil",
    "starRating": 5,
    "description": "Hotel de luxo no coração de São Paulo com quartos modernos e serviço de primeira classe",
    "latitude": -23.561684,
    "longitude": -46.655981,
    "phoneNumber": "+55 11 3000-0000",
    "email": "contato@grandplaza.com.br",
    "totalRooms": 150
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | object | Dados do hotel |
| data.id | long | Identificador único do hotel |
| data.name | string | Nome do hotel |
| data.address | string | Endereço completo |
| data.city | string | Cidade onde o hotel está localizado |
| data.country | string | País onde o hotel está localizado |
| data.starRating | int | Classificação em estrelas (1-5) |
| data.description | string | Descrição detalhada do hotel |
| data.latitude | double | Coordenada de latitude para geolocalização e consulta de clima |
| data.longitude | double | Coordenada de longitude para geolocalização e consulta de clima |
| data.phoneNumber | string | Telefone de contato |
| data.email | string | E-mail de contato |
| data.totalRooms | int | Número total de quartos do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404         | NOT_FOUND      | Hotel não encontrado com o ID fornecido |
| 500         | INTERNAL_ERROR | Erro interno do servidor ao buscar hotel |

## Regras de Negócio
1. O ID do hotel deve ser um número inteiro positivo
2. Se o hotel não for encontrado, uma exceção `NotFoundException` é lançada
3. Ao buscar com sucesso, o hotel é armazenado em cache para otimizar consultas futuras
4. O TTL do cache é configurável através da propriedade `hotel.cache-ttl-minutes`
5. O campo `totalRooms` é calculado dinamicamente com base nos quartos relacionados

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição HTTP, extrair parâmetro de path e retornar resposta |
| Service | HotelService | Gerenciar cache, buscar hotel e lançar exceção caso não encontrado |
| Repository | HotelRepository | Executar consulta por ID no banco de dados usando Panache |
| Mapper | HotelMapper | Converter entidade Hotel em HotelDto |
| Config | CacheConfig | Gerenciar operações de cache (get/put) |
| Config | HotelConfig | Fornecer TTL do cache |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/hotels/1]
    B --> C[HotelResource.getHotel]
    C --> D[HotelService.findById]
    D --> E{Cache Hit?}
    E -->|Sim| F[Retornar do Cache]
    E -->|Não| G[HotelRepository.findById]
    G --> H{Hotel Encontrado?}
    H -->|Não| I[Lançar NotFoundException]
    I --> J[Retornar 404 Not Found]
    H -->|Sim| K[Armazenar no Cache]
    K --> L[HotelMapper.toDto]
    F --> L
    L --> M[ApiResponse.success]
    M --> N[Retornar 200 OK]
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
    
    Cliente->>HotelResource: GET /api/v1/hotels/1
    HotelResource->>HotelService: findById(1)
    HotelService->>CacheConfig: getObject("hotel:1")
    alt Cache Hit
        CacheConfig-->>HotelService: Hotel
    else Cache Miss
        HotelService->>HotelRepository: findById(1)
        HotelRepository->>Database: SELECT * FROM hotels WHERE id = 1
        alt Hotel Não Encontrado
            Database-->>HotelRepository: null
            HotelRepository-->>HotelService: null
            HotelService-->>HotelResource: throw NotFoundException
            HotelResource-->>Cliente: 404 Not Found
        else Hotel Encontrado
            Database-->>HotelRepository: Hotel
            HotelRepository-->>HotelService: Hotel
            HotelService->>CacheConfig: putObject("hotel:1", hotel, TTL)
        end
    end
    HotelService->>HotelMapper: toDto(hotel)
    HotelMapper-->>HotelService: HotelDto
    HotelService-->>HotelResource: HotelDto
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
