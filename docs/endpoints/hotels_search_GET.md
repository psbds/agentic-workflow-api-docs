# GET /api/v1/hotels/search — Buscar Hotéis por Nome

## Descrição
Este endpoint permite buscar hotéis cujo nome contenha o termo especificado. Útil para implementar funcionalidades de busca e autocompletar na interface do usuário.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/hotels/search`          |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Não                              |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Busca parcial de hotéis por nome |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Query Parameters
| Parâmetro | Tipo   | Obrigatório | Descrição |
|-----------|--------|-------------|-----------|
| name      | string | Sim         | Termo de busca para filtrar hotéis por nome (busca parcial) |

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
| data | array | Lista de hotéis que correspondem ao termo de busca |
| data[].id | long | Identificador único do hotel |
| data[].name | string | Nome do hotel |
| data[].address | string | Endereço completo |
| data[].city | string | Cidade onde o hotel está localizado |
| data[].country | string | País onde o hotel está localizado |
| data[].starRating | int | Classificação em estrelas (1-5) |
| data[].description | string | Descrição detalhada do hotel |
| data[].latitude | double | Coordenada de latitude |
| data[].longitude | double | Coordenada de longitude |
| data[].phoneNumber | string | Telefone de contato |
| data[].email | string | E-mail de contato |
| data[].totalRooms | int | Número total de quartos do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 500         | INTERNAL_ERROR | Erro interno do servidor ao buscar hotéis |

## Regras de Negócio
1. A busca é parcial (LIKE) e não requer correspondência exata
2. Se nenhum hotel for encontrado, retorna lista vazia
3. Não há paginação nesta consulta
4. A busca não utiliza cache

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição e extrair query parameter |
| Service | HotelService | Delegar consulta ao repositório |
| Repository | HotelRepository | Executar busca com LIKE no banco de dados |
| Mapper | HotelMapper | Converter lista de entidades em DTOs |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/hotels/search?name=Grand]
    B --> C[HotelResource.searchByName]
    C --> D[HotelService.searchByName]
    D --> E[HotelRepository.findByNameContaining]
    E --> F[Consulta SQL: WHERE name LIKE %Grand%]
    F --> G[HotelMapper.toDtoList]
    G --> H[ApiResponse.success]
    H --> I[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant HotelResource
    participant HotelService
    participant HotelRepository
    participant HotelMapper
    participant Database
    
    Cliente->>HotelResource: GET /api/v1/hotels/search?name=Grand
    HotelResource->>HotelService: searchByName("Grand")
    HotelService->>HotelRepository: findByNameContaining("Grand")
    HotelRepository->>Database: SELECT * FROM hotels WHERE name LIKE '%Grand%'
    Database-->>HotelRepository: List(Hotel)
    HotelRepository-->>HotelService: List(Hotel)
    HotelService->>HotelMapper: toDtoList(hotels)
    HotelMapper-->>HotelService: List(HotelDto)
    HotelService-->>HotelResource: List(HotelDto)
    HotelResource-->>Cliente: 200 OK + ApiResponse
````

## Diagrama de Entidades
````mermaid
erDiagram
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
