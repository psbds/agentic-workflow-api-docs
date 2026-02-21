# POST /api/v1/hotels — Criar Novo Hotel

## Descrição
Este endpoint cria um novo hotel no sistema. Recebe informações completas do hotel, incluindo localização geográfica, classificação e dados de contato, e persiste no banco de dados.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/hotels`                 |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache da listagem paginada |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Inserção de novo hotel no banco de dados |
| Redis      | TCP    | Invalidação de cache da listagem (hotel:all:0:20) |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Body
````json
{
  "name": "Hotel Sunset Beach",
  "address": "Av. Beira Mar, 350",
  "city": "Florianópolis",
  "country": "Brasil",
  "starRating": 4,
  "description": "Hotel à beira-mar com vista panorâmica do oceano",
  "latitude": -27.595378,
  "longitude": -48.548050,
  "phoneNumber": "+55 48 3500-0000",
  "email": "contato@sunsetbeach.com.br"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| name | string | Sim | @NotNull - Nome do hotel não pode ser vazio |
| address | string | Sim | @NotNull - Endereço completo obrigatório |
| city | string | Sim | @NotNull - Cidade obrigatória |
| country | string | Sim | @NotNull - País obrigatório |
| starRating | int | Não | Classificação em estrelas (1-5) |
| description | string | Não | Descrição detalhada do hotel |
| latitude | double | Não | Coordenada de latitude (usado para consulta de clima) |
| longitude | double | Não | Coordenada de longitude (usado para consulta de clima) |
| phoneNumber | string | Não | Telefone de contato |
| email | string | Não | E-mail de contato |

## Response

### Sucesso — `201 CREATED`
````json
{
  "success": true,
  "message": "Hotel created successfully",
  "data": {
    "id": 10,
    "name": "Hotel Sunset Beach",
    "address": "Av. Beira Mar, 350",
    "city": "Florianópolis",
    "country": "Brasil",
    "starRating": 4,
    "description": "Hotel à beira-mar com vista panorâmica do oceano",
    "latitude": -27.595378,
    "longitude": -48.548050,
    "phoneNumber": "+55 48 3500-0000",
    "email": "contato@sunsetbeach.com.br",
    "totalRooms": 0
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem de sucesso |
| data | object | Dados do hotel criado |
| data.id | long | ID gerado automaticamente pelo banco de dados |
| data.name | string | Nome do hotel |
| data.address | string | Endereço completo |
| data.city | string | Cidade |
| data.country | string | País |
| data.starRating | int | Classificação em estrelas |
| data.description | string | Descrição detalhada |
| data.latitude | double | Coordenada de latitude |
| data.longitude | double | Coordenada de longitude |
| data.phoneNumber | string | Telefone de contato |
| data.email | string | E-mail de contato |
| data.totalRooms | int | Total de quartos (sempre 0 para hotel recém-criado) |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400         | VALIDATION_ERROR | Erro de validação Bean Validation (campos obrigatórios ausentes ou inválidos) |
| 500         | INTERNAL_ERROR | Erro interno do servidor ao criar hotel |

## Regras de Negócio
1. Hotel é criado com transação (@Transactional) para garantir integridade
2. Campos `createdAt` e `updatedAt` são preenchidos automaticamente via @PrePersist
3. ID é gerado automaticamente pelo banco (IDENTITY)
4. Cache da listagem paginada é invalidado para garantir consistência (hotel:all:0:20)
5. totalRooms inicia em 0 e será incrementado conforme quartos são associados
6. Latitude e longitude são opcionais mas recomendados para funcionalidades de clima e geolocalização

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição, validar com Bean Validation e retornar 201 Created |
| Service | HotelService | Persistir hotel, invalidar cache e registrar log |
| Repository | HotelRepository | Executar INSERT no banco de dados via Panache |
| Mapper | HotelMapper | Converter entidade Hotel em HotelDto |
| Config | CacheConfig | Invalidar cache da listagem |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/hotels + body JSON]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[HotelResource.createHotel]
    F --> G[HotelService.create]
    G --> H[@Transactional BEGIN]
    H --> I[HotelRepository.persist]
    I --> J[INSERT INTO hotels]
    J --> K[@PrePersist: set createdAt, updatedAt]
    K --> L[CacheConfig.delete cache:all:0:20]
    L --> M[@Transactional COMMIT]
    M --> N[HotelMapper.toDto]
    N --> O[ApiResponse.success]
    O --> P[Retornar 201 Created]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant HotelResource
    participant Validator
    participant HotelService
    participant HotelRepository
    participant CacheConfig
    participant Database
    participant HotelMapper
    
    Cliente->>HotelResource: POST /api/v1/hotels + body
    HotelResource->>Validator: @Valid Hotel
    alt Validação Falha
        Validator-->>HotelResource: ValidationException
        HotelResource-->>Cliente: 400 Bad Request
    else Validação OK
        HotelResource->>HotelService: create(hotel)
        HotelService->>HotelRepository: persist(hotel)
        HotelRepository->>Database: INSERT INTO hotels
        Database-->>HotelRepository: Hotel com ID gerado
        HotelRepository-->>HotelService: Hotel
        HotelService->>CacheConfig: delete("hotel:all:0:20")
        HotelService->>HotelMapper: toDto(hotel)
        HotelMapper-->>HotelService: HotelDto
        HotelService-->>HotelResource: HotelDto
        HotelResource-->>Cliente: 201 Created + ApiResponse
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
