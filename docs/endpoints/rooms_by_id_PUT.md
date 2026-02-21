# PUT /api/v1/rooms/{id} — Atualizar Quarto

## Descrição
Este endpoint atualiza as informações completas de um quarto existente. Permite modificar tipo, preço, capacidade e outras características do quarto.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `PUT`                            |
| **Path**             | `/api/v1/rooms/{id}`             |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove caches do quarto e hotel |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Atualização dos dados do quarto |
| Redis | TCP | Invalidação de caches por ID e hotelId |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único do quarto a ser atualizado |

### Body
````json
{
  "roomNumber": "505-A",
  "roomType": "PENTHOUSE",
  "pricePerNight": 600.00,
  "maxOccupancy": 8,
  "description": "Cobertura premium renovada com terraço ampliado",
  "isAvailable": true,
  "floorNumber": 5,
  "hotelId": 1
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| roomNumber | string | Sim | @NotNull, @NotBlank - Número do quarto |
| roomType | enum | Sim | @NotNull - Tipo do quarto |
| pricePerNight | decimal | Sim | @NotNull, @DecimalMin(0.01) - Preço mínimo R$ 0,01 |
| maxOccupancy | int | Sim | @NotNull, @Min(1) - Capacidade mínima 1 |
| description | string | Não | Descrição atualizada |
| isAvailable | boolean | Sim | Disponibilidade |
| floorNumber | int | Não | Andar do quarto |
| hotelId | long | Sim | @NotNull - ID do hotel (não pode ser alterado) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Room updated successfully",
  "data": {
    "id": 45,
    "roomNumber": "505-A",
    "roomType": "PENTHOUSE",
    "pricePerNight": 600.00,
    "maxOccupancy": 8,
    "description": "Cobertura premium renovada com terraço ampliado",
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
| data | object | Dados do quarto atualizado |
| data.id | long | ID do quarto (não muda) |
| data.roomNumber | string | Número do quarto atualizado |
| data.roomType | enum | Tipo do quarto atualizado |
| data.pricePerNight | decimal | Preço atualizado |
| data.maxOccupancy | int | Capacidade atualizada |
| data.description | string | Descrição atualizada |
| data.isAvailable | boolean | Disponibilidade atualizada |
| data.floorNumber | int | Andar atualizado |
| data.hotelId | long | ID do hotel (não muda) |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos obrigatórios ausentes ou valores inválidos |
| 404 | ROOM_NOT_FOUND | Quarto com o ID informado não existe |
| 404 | HOTEL_NOT_FOUND | Hotel com hotelId informado não existe |
| 409 | DUPLICATE_ROOM_NUMBER | Novo roomNumber já existe em outro quarto do mesmo hotel |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao atualizar quarto |

## Regras de Negócio
1. **Validação de Existência**: Verifica se o quarto existe, caso contrário lança NotFoundException
2. **Validação de Hotel**: Se hotelId for diferente do atual, valida se novo hotel existe
3. **Número Único por Hotel**: Se roomNumber mudou, valida se não existe em outro quarto do mesmo hotel
4. **Atualização Completa**: PUT substitui TODOS os campos (não é PATCH)
5. **ID Imutável**: ID do quarto não pode ser alterado
6. **Campo updatedAt**: Atualizado automaticamente via @PreUpdate
7. **Invalidação de Cache**: Remove cache room:id e room:hotel:hotelId (antigo e novo se mudou)
8. **Impacto em Reservas**: Reservas existentes mantêm referência ao quarto atualizado
9. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição, validar e retornar 200 OK |
| Service | RoomService | Validar existência, validar unicidade, atualizar e invalidar cache |
| Repository | RoomRepository | Persistir atualização no banco |
| Repository | HotelRepository | Validar hotel se mudou |
| Mapper | RoomMapper | Converter entidade atualizada em DTO |
| Config | CacheConfig | Invalidar caches relacionados |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[PUT /api/v1/rooms/45 + body]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[RoomResource.updateRoom]
    F --> G[RoomService.update]
    G --> H[RoomRepository.findById]
    H --> I{Quarto existe?}
    I -->|Não| J[NotFoundException → 404]
    I -->|Sim| K{HotelId mudou?}
    K -->|Sim| L[HotelRepository.findById - novo]
    L --> M{Novo hotel existe?}
    M -->|Não| N[NotFoundException → 404]
    M -->|Sim| O{RoomNumber mudou?}
    K -->|Não| O
    O -->|Sim| P[RoomRepository.findByHotelIdAndRoomNumber]
    P --> Q{Número existe em outro?}
    Q -->|Sim| R[DuplicateRoomNumberException → 409]
    Q -->|Não| S[Atualizar todos os campos]
    O -->|Não| S
    S --> T[@Transactional: persist update]
    T --> U[CacheConfig.delete - room:45]
    U --> V[CacheConfig.delete - room:hotel:OLD]
    V --> W[CacheConfig.delete - room:hotel:NEW]
    W --> X[RoomMapper.toDto]
    X --> Y[ApiResponse.success]
    Y --> Z[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant Validator
    participant RoomService
    participant RoomRepository
    participant HotelRepository
    participant CacheConfig
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: PUT /api/v1/rooms/45 + body
    RoomResource->>Validator: @Valid Room
    alt Validação Falha
        Validator-->>RoomResource: ValidationException
        RoomResource-->>Cliente: 400 Bad Request
    else Validação OK
        RoomResource->>RoomService: update(45, roomData)
        RoomService->>RoomRepository: findById(45)
        RoomRepository->>Database: SELECT * FROM rooms WHERE id = 45
        alt Quarto não existe
            Database-->>RoomRepository: null
            RoomRepository-->>RoomService: Optional.empty
            RoomService-->>RoomResource: NotFoundException
            RoomResource-->>Cliente: 404 ROOM_NOT_FOUND
        else Quarto existe
            Database-->>RoomRepository: Room (hotelId=1, roomNumber=505)
            RoomRepository-->>RoomService: Room
            alt hotelId mudou
                RoomService->>HotelRepository: findById(newHotelId)
                alt Hotel não existe
                    RoomService-->>RoomResource: NotFoundException
                    RoomResource-->>Cliente: 404 HOTEL_NOT_FOUND
                end
            end
            alt roomNumber mudou
                RoomService->>RoomRepository: findByHotelIdAndRoomNumber(hotelId, newNumber)
                alt Número já existe em outro
                    RoomService-->>RoomResource: DuplicateRoomNumberException
                    RoomResource-->>Cliente: 409 DUPLICATE_ROOM_NUMBER
                end
            end
            RoomService->>RoomService: Atualizar campos
            RoomService->>RoomRepository: persist(room)
            RoomRepository->>Database: UPDATE rooms SET...
            Database-->>RoomRepository: Room atualizado
            RoomService->>CacheConfig: delete("room:45")
            RoomService->>CacheConfig: delete("room:hotel:1")
            RoomService->>RoomMapper: toDto(room)
            RoomMapper-->>RoomService: RoomDto
            RoomService-->>RoomResource: RoomDto
            RoomResource-->>Cliente: 200 OK + ApiResponse
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Room ||--|| Hotel : "pertence a"
    Room ||--o( Reservation : "mantém reservas"
    
    Room (
        Long id PK IMMUTABLE,
        String roomNumber UPDATABLE_UNIQUE_PER_HOTEL,
        RoomType roomType UPDATABLE,
        BigDecimal pricePerNight UPDATABLE,
        int maxOccupancy UPDATABLE,
        boolean isAvailable UPDATABLE,
        Long hotelId FK UPDATABLE,
        LocalDateTime createdAt IMMUTABLE,
        LocalDateTime updatedAt AUTO_UPDATED
    )
````
