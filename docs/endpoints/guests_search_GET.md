# GET /api/v1/guests/search?name=X — Buscar Hóspedes por Nome

## Descrição
Este endpoint permite buscar hóspedes através de uma busca parcial por nome (firstName ou lastName). Retorna todos os hóspedes cujos nomes contenham o termo pesquisado.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/guests/search`          |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Não (consulta dinâmica)          |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Consulta com LIKE em firstName e lastName |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| name | string | Sim | Termo de busca (case-insensitive, busca parcial) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Guests retrieved successfully",
  "data": [
    {
      "id": 5,
      "firstName": "Maria",
      "lastName": "Silva Santos",
      "email": "maria.santos@email.com",
      "phoneNumber": "+55 11 98765-4321",
      "documentType": "CPF",
      "documentNumber": "123.456.789-00",
      "nationality": "Brasileira"
    },
    {
      "id": 12,
      "firstName": "João",
      "lastName": "Maria Oliveira",
      "email": "joao.maria@email.com",
      "phoneNumber": "+55 21 99876-5432",
      "documentType": "RG",
      "documentNumber": "12.345.678-9",
      "nationality": "Brasileira"
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
| data | array | Lista de hóspedes encontrados |
| data[].id | long | Identificador único do hóspede |
| data[].firstName | string | Primeiro nome |
| data[].lastName | string | Sobrenome |
| data[].email | string | Endereço de email |
| data[].phoneNumber | string | Telefone de contato |
| data[].documentType | string | Tipo de documento |
| data[].documentNumber | string | Número do documento |
| data[].nationality | string | Nacionalidade |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Parâmetro name não fornecido ou vazio |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao buscar hóspedes |

## Regras de Negócio
1. **Busca Case-Insensitive**: Termo de busca é convertido para lowercase e comparado com LOWER(firstName) e LOWER(lastName)
2. **Busca Parcial**: Usa LIKE com padrão %termo% para encontrar correspondências em qualquer parte do nome
3. **Busca em Múltiplos Campos**: Procura em firstName OU lastName (OR logic)
4. **Lista Vazia**: Retorna array vazio se nenhum hóspede for encontrado (não é erro 404)
5. **Sem Paginação**: Retorna todos os resultados encontrados (pode ser limitado em produção)
6. **Ordenação**: Resultados ordenados por firstName ASC, lastName ASC

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | GuestResource | Receber requisição, validar parâmetro name e retornar resposta |
| Service | GuestService | Delegar consulta ao repositório |
| Repository | GuestRepository | Executar consulta com LIKE em firstName e lastName |
| Mapper | GuestMapper | Converter lista de entidades em lista de DTOs |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/guests/search?name=Maria]
    B --> C[GuestResource.searchByName]
    C --> D{Parâmetro name presente?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[name.toLowerCase]
    F --> G[GuestService.searchByName]
    G --> H[GuestRepository.findByNameContaining]
    H --> I[SQL: WHERE LOWER firstName LIKE %maria% OR LOWER lastName LIKE %maria%]
    I --> J[Ordenar por firstName, lastName ASC]
    J --> K[GuestMapper.toDtoList]
    K --> L[ApiResponse.success]
    L --> M[Retornar 200 OK com lista vazia ou com resultados]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant GuestResource
    participant GuestService
    participant GuestRepository
    participant Database
    participant GuestMapper
    
    Cliente->>GuestResource: GET /api/v1/guests/search?name=Maria
    GuestResource->>GuestResource: Validar parâmetro name
    alt name ausente ou vazio
        GuestResource-->>Cliente: 400 VALIDATION_ERROR
    else name válido
        GuestResource->>GuestResource: name.toLowerCase()
        GuestResource->>GuestService: searchByName("maria")
        GuestService->>GuestRepository: findByNameContaining("maria")
        GuestRepository->>Database: SELECT * FROM guests WHERE LOWER(first_name) LIKE %maria% OR LOWER(last_name) LIKE %maria% ORDER BY first_name, last_name
        Database-->>GuestRepository: List(Guest)
        GuestRepository-->>GuestService: List(Guest)
        GuestService->>GuestMapper: toDtoList(guests)
        GuestMapper-->>GuestService: List(GuestDto)
        GuestService-->>GuestResource: List(GuestDto)
        GuestResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest (
        Long id PK,
        String firstName "Indexed for search",
        String lastName "Indexed for search",
        String email,
        String phoneNumber,
        String documentType,
        String documentNumber,
        String nationality
    )
````
