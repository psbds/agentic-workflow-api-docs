# PATCH /api/v1/rooms/{id}/availability — Atualizar Disponibilidade do Quarto

## Descrição
Este endpoint atualiza apenas o campo de disponibilidade (isAvailable) de um quarto específico. Operação rápida para marcar quartos como disponíveis ou indisponíveis sem alterar outros dados.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `PATCH`                          |
| **Path**             | `/api/v1/rooms/{id}/availability` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove caches do quarto |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Atualização do campo isAvailable |
| Redis | TCP | Invalidação de caches relacionados |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único do quarto |

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| available | boolean | Sim | Nova disponibilidade (true ou false) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Room availability updated successfully",
  "data": {
    "id": 12,
    "roomNumber": "305",
    "roomType": "SUITE",
    "pricePerNight": 150.00,
    "maxOccupancy": 3,
    "description": "Suíte luxuosa com vista para o mar",
    "isAvailable": false,
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
| message | string | Mensagem de sucesso |
| data | object | Dados completos do quarto |
| data.id | long | ID do quarto |
| data.roomNumber | string | Número do quarto (inalterado) |
| data.roomType | enum | Tipo do quarto (inalterado) |
| data.pricePerNight | decimal | Preço (inalterado) |
| data.maxOccupancy | int | Capacidade (inalterada) |
| data.description | string | Descrição (inalterada) |
| data.isAvailable | boolean | Disponibilidade ATUALIZADA |
| data.floorNumber | int | Andar (inalterado) |
| data.hotelId | long | ID do hotel (inalterado) |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Parâmetro available ausente ou inválido |
| 404 | ROOM_NOT_FOUND | Quarto com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao atualizar disponibilidade |

## Regras de Negócio
1. **Atualização Parcial**: Apenas o campo isAvailable é modificado (PATCH semântico)
2. **Validação de Existência**: Verifica se o quarto existe, caso contrário lança NotFoundException
3. **Validação de Parâmetro**: Parâmetro available é obrigatório e deve ser boolean válido
4. **Idempotência**: Pode ser chamado múltiplas vezes com mesmo valor sem efeitos colaterais
5. **Campo updatedAt**: Atualizado automaticamente via @PreUpdate
6. **Invalidação de Cache**: Remove cache room:id e room:hotel:hotelId
7. **Uso Típico**: Chamado após check-out para liberar quarto (available=true) ou após check-in para ocupar (available=false)
8. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | RoomResource | Receber requisição, validar parâmetro e retornar 200 OK |
| Service | RoomService | Validar existência, atualizar isAvailable e invalidar cache |
| Repository | RoomRepository | Persistir atualização parcial |
| Mapper | RoomMapper | Converter entidade atualizada em DTO |
| Config | CacheConfig | Invalidar caches relacionados |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[PATCH /api/v1/rooms/12/availability?available=false]
    B --> C[RoomResource.updateAvailability]
    C --> D{Parâmetro available presente?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[RoomService.updateAvailability]
    F --> G[RoomRepository.findById]
    G --> H{Quarto existe?}
    H -->|Não| I[NotFoundException → 404]
    H -->|Sim| J[room.setIsAvailable - false]
    J --> K[@Transactional: persist update]
    K --> L[@PreUpdate: set updatedAt]
    L --> M[CacheConfig.delete - room:12]
    M --> N[CacheConfig.delete - room:hotel:1]
    N --> O[RoomMapper.toDto]
    O --> P[ApiResponse.success]
    P --> Q[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant RoomResource
    participant RoomService
    participant RoomRepository
    participant CacheConfig
    participant Database
    participant RoomMapper
    
    Cliente->>RoomResource: PATCH /api/v1/rooms/12/availability?available=false
    RoomResource->>RoomResource: Validar parâmetro available
    alt Parâmetro ausente ou inválido
        RoomResource-->>Cliente: 400 VALIDATION_ERROR
    else Parâmetro válido
        RoomResource->>RoomService: updateAvailability(12, false)
        RoomService->>RoomRepository: findById(12)
        RoomRepository->>Database: SELECT * FROM rooms WHERE id = 12
        alt Quarto não existe
            Database-->>RoomRepository: null
            RoomRepository-->>RoomService: Optional.empty
            RoomService-->>RoomResource: NotFoundException
            RoomResource-->>Cliente: 404 ROOM_NOT_FOUND
        else Quarto existe
            Database-->>RoomRepository: Room (isAvailable=true)
            RoomRepository-->>RoomService: Room
            RoomService->>RoomService: room.setIsAvailable(false)
            RoomService->>RoomRepository: persist(room)
            RoomRepository->>Database: UPDATE rooms SET is_available = false WHERE id = 12
            Database-->>RoomRepository: Room atualizado
            RoomService->>CacheConfig: delete("room:12")
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
    Room ||--o( Reservation : "estado reflete ocupação"
    
    Room (
        Long id PK,
        String roomNumber,
        boolean isAvailable "CAMPO_ATUALIZADO_POR_PATCH",
        Long hotelId FK,
        LocalDateTime updatedAt
    )
    
    Reservation (
        Long id PK,
        ReservationStatus status "CHECKED_IN → room.isAvailable=false",
        Long roomId FK "CHECKED_OUT → room.isAvailable=true"
    )
````
