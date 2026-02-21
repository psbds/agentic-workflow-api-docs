# PUT /api/v1/hotels/{id} — Atualizar Hotel

## Descrição
Este endpoint atualiza as informações de um hotel existente. Permite modificar dados como nome, endereço, classificação, descrição e informações de contato.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `PUT`                            |
| **Path**             | `/api/v1/hotels/{id}`            |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache do hotel específico (hotel:{id}) |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Atualização de hotel no banco de dados |
| Redis      | TCP    | Invalidação de cache do hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id        | long | Sim         | Identificador único do hotel a ser atualizado |

### Body
````json
{
  "name": "Hotel Sunset Beach Premium",
  "address": "Av. Beira Mar, 350 - Atualizado",
  "city": "Florianópolis",
  "country": "Brasil",
  "starRating": 5,
  "description": "Hotel premium à beira-mar com vista panorâmica e serviços exclusivos",
  "latitude": -27.595378,
  "longitude": -48.548050,
  "phoneNumber": "+55 48 3500-0001",
  "email": "premium@sunsetbeach.com.br"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| name | string | Sim | Nome do hotel |
| address | string | Sim | Endereço completo |
| city | string | Sim | Cidade |
| country | string | Sim | País |
| starRating | int | Não | Classificação em estrelas (1-5) |
| description | string | Não | Descrição detalhada |
| latitude | double | Não | Coordenada de latitude |
| longitude | double | Não | Coordenada de longitude |
| phoneNumber | string | Não | Telefone de contato |
| email | string | Não | E-mail de contato |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Hotel updated successfully",
  "data": {
    "id": 10,
    "name": "Hotel Sunset Beach Premium",
    "address": "Av. Beira Mar, 350 - Atualizado",
    "city": "Florianópolis",
    "country": "Brasil",
    "starRating": 5,
    "description": "Hotel premium à beira-mar com vista panorâmica e serviços exclusivos",
    "latitude": -27.595378,
    "longitude": -48.548050,
    "phoneNumber": "+55 48 3500-0001",
    "email": "premium@sunsetbeach.com.br",
    "totalRooms": 25
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem de sucesso |
| data | object | Dados do hotel atualizado |
| data.id | long | ID do hotel |
| data.name | string | Nome atualizado |
| data.address | string | Endereço atualizado |
| data.city | string | Cidade atualizada |
| data.country | string | País atualizado |
| data.starRating | int | Classificação atualizada |
| data.description | string | Descrição atualizada |
| data.latitude | double | Latitude atualizada |
| data.longitude | double | Longitude atualizada |
| data.phoneNumber | string | Telefone atualizado |
| data.email | string | E-mail atualizado |
| data.totalRooms | int | Total de quartos (não modificado neste endpoint) |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404         | NOT_FOUND      | Hotel não encontrado com o ID fornecido |
| 500         | INTERNAL_ERROR | Erro interno do servidor ao atualizar hotel |

## Regras de Negócio
1. Hotel deve existir no banco de dados, caso contrário retorna 404
2. Atualização é feita em transação (@Transactional)
3. Campo `updatedAt` é atualizado automaticamente via @PreUpdate
4. ID não pode ser modificado (gerado pelo banco e imutável)
5. Cache do hotel específico é invalidado após atualização
6. Relacionamentos com quartos não são afetados por esta operação

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição e extrair ID do path |
| Service | HotelService | Buscar hotel existente, atualizar campos e invalidar cache |
| Repository | HotelRepository | Executar UPDATE no banco de dados |
| Mapper | HotelMapper | Converter entidade atualizada em HotelDto |
| Config | CacheConfig | Invalidar cache do hotel |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[PUT /api/v1/hotels/10 + body]
    B --> C[HotelResource.updateHotel]
    C --> D[HotelService.update]
    D --> E[HotelRepository.findById]
    E --> F{Hotel Encontrado?}
    F -->|Não| G[Lançar NotFoundException]
    G --> H[Retornar 404 Not Found]
    F -->|Sim| I[@Transactional BEGIN]
    I --> J[Atualizar campos do hotel]
    J --> K[HotelRepository.persist]
    K --> L[@PreUpdate: set updatedAt]
    L --> M[UPDATE hotels SET...]
    M --> N[CacheConfig.delete cache:hotel:10]
    N --> O[@Transactional COMMIT]
    O --> P[HotelMapper.toDto]
    P --> Q[ApiResponse.success]
    Q --> R[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant HotelResource
    participant HotelService
    participant HotelRepository
    participant CacheConfig
    participant Database
    participant HotelMapper
    
    Cliente->>HotelResource: PUT /api/v1/hotels/10 + body
    HotelResource->>HotelService: update(10, hotel)
    HotelService->>HotelRepository: findById(10)
    HotelRepository->>Database: SELECT * FROM hotels WHERE id = 10
    alt Hotel Não Encontrado
        Database-->>HotelRepository: null
        HotelRepository-->>HotelService: null
        HotelService-->>HotelResource: throw NotFoundException
        HotelResource-->>Cliente: 404 Not Found
    else Hotel Encontrado
        Database-->>HotelRepository: Hotel
        HotelRepository-->>HotelService: Hotel
        HotelService->>HotelService: Atualizar campos
        HotelService->>HotelRepository: persist(hotel)
        HotelRepository->>Database: UPDATE hotels SET... WHERE id = 10
        Database-->>HotelRepository: Hotel atualizado
        HotelService->>CacheConfig: delete("hotel:10")
        HotelService->>HotelMapper: toDto(hotel)
        HotelMapper-->>HotelService: HotelDto
        HotelService-->>HotelResource: HotelDto
        HotelResource-->>Cliente: 200 OK + ApiResponse
    end
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
````
