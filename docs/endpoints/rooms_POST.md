# POST /api/v1/rooms — Criar Novo Quarto

## Descrição
Este endpoint cria um novo quarto associado a um hotel existente. Registra informações de tipo, capacidade, preço e características do quarto.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/rooms`                  |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache de listagem do hotel |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Inserção de novo quarto no banco de dados |
| Redis | TCP | Invalidação de cache room:hotel:hotelId |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Body
````json
{
  "roomNumber": "505",
  "roomType": "PENTHOUSE",
  "pricePerNight": 500.00,
  "maxOccupancy": 6,
  "description": "Cobertura de luxo com terraço privativo e jacuzzi",
  "isAvailable": true,
  "floorNumber": 5,
  "hotelId": 1
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| roomNumber | string | Sim | @NotNull, @NotBlank - Número do quarto não pode ser vazio |
| roomType | enum | Sim | @NotNull - Valores: SINGLE, DOUBLE, SUITE, DELUXE, PENTHOUSE |
| pricePerNight | decimal | Sim | @NotNull, @DecimalMin(0.01) - Preço mínimo de R$ 0,01 |
| maxOccupancy | int | Sim | @NotNull, @Min(1) - Capacidade mínima de 1 hóspede |
| description | string | Não | Descrição detalhada do quarto |
| isAvailable | boolean | Não | Disponibilidade (padrão: true se não fornecido) |
| floorNumber | int | Não | Andar do quarto |
| hotelId | long | Sim | @NotNull - ID do hotel (deve existir no banco) |

## Response

### Sucesso — `201 CREATED`
````json
{
  "success": true,
  "message": "Room created successfully",
  "data": {
    "id": 45,
    "roomNumber": "505",
    "roomType": "PENTHOUSE",
    "pricePerNight": 500.00,
    "maxOccupancy": 6,
    "description": "Cobertura de luxo com terraço privativo e jacuzzi",
    "isAvailable": true,
    "floorNumber": 5,
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
| message | string | Mensagem de sucesso |
| data | object | Dados do quarto criado |
| data.id | long | ID gerado automaticamente pelo banco de dados |
| data.roomNumber | string | Número do quarto |
| data.roomType | enum | Tipo do quarto |
| data.pricePerNight | decimal | Preço por diária |
| data.maxOccupancy | int | Capacidade máxima |
| data.description | string | Descrição |
| data.isAvailable | boolean | Disponibilidade |
| data.floorNumber | int | Andar |
| data.hotelId | long | ID do hotel |
| data.hotelName | string | Nome do hotel (obtido via join) |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos obrigatórios ausentes ou valores inválidos |
| 404 | HOTEL_NOT_FOUND | Hotel com hotelId informado não existe |
| 409 | DUPLICATE_ROOM_NUMBER | Número do quarto já existe neste hotel |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao criar quarto |

## Regras de Negócio
1. **Validação de Hotel**: Verifica se hotel existe, caso contrário lança NotFoundException
2. **Número Único por Hotel**: Valida se roomNumber já existe no hotel. Se sim, lança DuplicateRoomNumberException (409)
3. **Disponibilidade Padrão**: Se isAvailable não fornecido, assume true
4. **Campos Timestamp**: createdAt e updatedAt preenchidos automaticamente via @PrePersist
5. **ID Gerado**: ID gerado automaticamente pelo banco (IDENTITY)
6. **Invalidação de Cache**: Remove cache `room:hotel:{hotelId}` para garantir consistência
7. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição, validar e retornar 201 Created |
| Service | RoomService | Validar hotel, validar unicidade do roomNumber, persistir e invalidar cache |
| Repository | HotelRepository | Validar existência do hotel |
| Repository | RoomRepository | Executar INSERT no banco via Panache |
| Mapper | RoomMapper | Converter entidade Room em RoomDto |
| Config | CacheConfig | Invalidar cache da listagem do hotel |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/rooms + body JSON]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[RoomResource.createRoom]
    F --> G[RoomService.create]
    G --> H[HotelRepository.findById]
    H --> I{Hotel existe?}
    I -->|Não| J[NotFoundException → 404]
    I -->|Sim| K[RoomRepository.findByHotelIdAndRoomNumber]
    K --> L{RoomNumber já existe?}
    L -->|Sim| M[DuplicateRoomNumberException → 409]
    L -->|Não| N[@Transactional BEGIN]
    N --> O[RoomRepository.persist]
    O --> P[INSERT INTO rooms]
    P --> Q[@PrePersist: set createdAt, updatedAt]
    Q --> R[CacheConfig.delete - room:hotel:1]
    R --> S[@Transactional COMMIT]
    S --> T[RoomMapper.toDto]
    T --> U[ApiResponse.success]
    U --> V[Retornar 201 Created]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant Validator
    participant RoomService
    participant HotelRepository
    participant RoomRepository
    participant CacheConfig
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: POST /api/v1/rooms + body
    RoomResource->>Validator: @Valid Room
    alt Validação Falha
        Validator-->>RoomResource: ValidationException
        RoomResource-->>Cliente: 400 Bad Request
    else Validação OK
        RoomResource->>RoomService: create(room)
        RoomService->>HotelRepository: findById(hotelId)
        HotelRepository->>Database: SELECT * FROM hotels WHERE id = ?
        alt Hotel não existe
            Database-->>HotelRepository: null
            HotelRepository-->>RoomService: Optional.empty
            RoomService-->>RoomResource: NotFoundException
            RoomResource-->>Cliente: 404 HOTEL_NOT_FOUND
        else Hotel existe
            Database-->>HotelRepository: Hotel
            RoomService->>RoomRepository: findByHotelIdAndRoomNumber(hotelId, roomNumber)
            RoomRepository->>Database: SELECT * WHERE hotel_id = ? AND room_number = ?
            alt RoomNumber já existe
                Database-->>RoomRepository: Room
                RoomRepository-->>RoomService: Optional(Room)
                RoomService-->>RoomResource: DuplicateRoomNumberException
                RoomResource-->>Cliente: 409 DUPLICATE_ROOM_NUMBER
            else RoomNumber disponível
                Database-->>RoomRepository: null
                RoomService->>RoomRepository: persist(room)
                RoomRepository->>Database: INSERT INTO rooms
                Database-->>RoomRepository: Room com ID gerado
                RoomRepository-->>RoomService: Room
                RoomService->>CacheConfig: delete("room:hotel:1")
                RoomService->>RoomMapper: toDto(room)
                RoomMapper-->>RoomService: RoomDto
                RoomService-->>RoomResource: RoomDto
                RoomResource-->>Cliente: 201 Created + ApiResponse
            end
        end
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
        String roomNumber UNIQUE_PER_HOTEL,
        RoomType roomType,
        BigDecimal pricePerNight,
        int maxOccupancy,
        boolean isAvailable,
        int floorNumber,
        Long hotelId FK,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
