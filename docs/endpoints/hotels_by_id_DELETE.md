# DELETE /api/v1/hotels/{id} — Deletar Hotel

## Descrição
Este endpoint remove permanentemente um hotel do sistema. A operação é irreversível e também remove todos os quartos associados ao hotel devido ao cascade configurado na entidade.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `DELETE`                         |
| **Path**             | `/api/v1/hotels/{id}`            |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache do hotel (hotel:{id}) |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Remoção de hotel e quartos relacionados (cascade) |
| Redis      | TCP    | Invalidação de cache do hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id        | long | Sim         | Identificador único do hotel a ser removido |

## Response

### Sucesso — `204 NO CONTENT`
Sem corpo na resposta. Status HTTP 204 indica que a operação foi bem-sucedida e o recurso foi removido.

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404         | NOT_FOUND      | Hotel não encontrado com o ID fornecido |
| 500         | INTERNAL_ERROR | Erro interno do servidor ao deletar hotel |

## Regras de Negócio
1. Hotel deve existir no banco de dados, caso contrário retorna 404
2. Operação é transacional (@Transactional) para garantir integridade
3. Todos os quartos associados ao hotel são removidos automaticamente (CascadeType.ALL, orphanRemoval = true)
4. **ATENÇÃO**: Reservas ativas associadas aos quartos deste hotel podem causar erro de constraint de integridade referencial se não forem tratadas previamente
5. Cache do hotel é invalidado após remoção
6. Operação não pode ser desfeita

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição, extrair ID e retornar 204 No Content |
| Service | HotelService | Verificar existência do hotel, deletar e invalidar cache |
| Repository | HotelRepository | Executar DELETE no banco de dados |
| Config | CacheConfig | Invalidar cache do hotel |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[DELETE /api/v1/hotels/10]
    B --> C[HotelResource.deleteHotel]
    C --> D[HotelService.delete]
    D --> E[HotelRepository.findById]
    E --> F{Hotel Encontrado?}
    F -->|Não| G[Lançar NotFoundException]
    G --> H[Retornar 404 Not Found]
    F -->|Sim| I[@Transactional BEGIN]
    I --> J[HotelRepository.delete]
    J --> K[DELETE FROM hotels CASCADE]
    K --> L[Remover quartos relacionados]
    L --> M[CacheConfig.delete cache:hotel:10]
    M --> N[@Transactional COMMIT]
    N --> O[Retornar 204 No Content]
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
    
    Cliente->>HotelResource: DELETE /api/v1/hotels/10
    HotelResource->>HotelService: delete(10)
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
        HotelService->>HotelRepository: delete(hotel)
        HotelRepository->>Database: DELETE FROM hotels WHERE id = 10
        Database->>Database: CASCADE DELETE rooms
        Database-->>HotelRepository: Success
        HotelService->>CacheConfig: delete("hotel:10")
        HotelService-->>HotelResource: void
        HotelResource-->>Cliente: 204 No Content
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Hotel ||--o( Room : "possui - CASCADE DELETE"
    
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
