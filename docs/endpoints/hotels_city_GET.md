# GET /api/v1/hotels/city/{city} — Buscar Hotéis por Cidade

## Descrição
Este endpoint retorna uma lista de hotéis localizados em uma cidade específica. É utilizado para filtrar hotéis por localização geográfica, facilitando a busca por acomodações em destinos específicos.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/hotels/city/{city}`     |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Não                              |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC   | Consulta de hotéis por cidade no banco de dados |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo   | Obrigatório | Descrição |
|-----------|--------|-------------|-----------|
| city      | string | Sim         | Nome da cidade para filtrar hotéis |

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
      "id": 3,
      "name": "Business Center Hotel",
      "address": "Av. Faria Lima, 2500",
      "city": "São Paulo",
      "country": "Brasil",
      "starRating": 4,
      "description": "Hotel corporativo próximo ao centro financeiro",
      "latitude": -23.574321,
      "longitude": -46.688765,
      "phoneNumber": "+55 11 4000-0000",
      "email": "reservas@businesscenter.com.br",
      "totalRooms": 80
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
| data | array | Lista de hotéis na cidade especificada |
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
1. A busca por cidade é case-sensitive (diferencia maiúsculas de minúsculas)
2. Se nenhum hotel for encontrado na cidade, retorna uma lista vazia (não gera erro)
3. Não há paginação nesta consulta
4. A busca não utiliza cache (consulta sempre atualizada do banco)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | HotelResource | Receber requisição HTTP e extrair parâmetro de path |
| Service | HotelService | Delegar consulta ao repositório |
| Repository | HotelRepository | Executar consulta por cidade no banco de dados |
| Mapper | HotelMapper | Converter lista de entidades Hotel em HotelDto |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/hotels/city/São Paulo]
    B --> C[HotelResource.getByCity]
    C --> D[HotelService.findByCity]
    D --> E[HotelRepository.findByCity]
    E --> F[Consulta SQL: WHERE city = ?]
    F --> G[HotelMapper.toDtoList]
    G --> H[ApiResponse.success]
    H --> I[Retornar 200 OK com lista]
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
    
    Cliente->>HotelResource: GET /api/v1/hotels/city/São Paulo
    HotelResource->>HotelService: findByCity("São Paulo")
    HotelService->>HotelRepository: findByCity("São Paulo")
    HotelRepository->>Database: SELECT * FROM hotels WHERE city = 'São Paulo'
    Database-->>HotelRepository: List(Hotel)
    HotelRepository-->>HotelService: List(Hotel)
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
````
