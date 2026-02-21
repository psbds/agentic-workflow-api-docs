# POST /api/v1/reservations/{id}/confirm — Confirmar Reserva

## Descrição
Este endpoint confirma uma reserva que está com status PENDING. A confirmação indica que o pagamento foi processado ou aprovado e a reserva está garantida para o hóspede.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/reservations/{id}/confirm` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache da reserva |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Atualização do status da reserva |
| Redis | TCP | Invalidação de cache da reserva |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único da reserva a ser confirmada |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Reservation confirmed successfully",
  "data": {
    "id": 42,
    "confirmationCode": "HTL-A1B2C3D4",
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-18",
    "numberOfGuests": 2,
    "totalPrice": 450.00,
    "status": "CONFIRMED",
    "paymentStatus": "PENDING",
    "specialRequests": "Quarto com vista para o mar",
    "weatherChecked": true,
    "weatherSummary": "Clear sky - Temp: 24.5°C - Wind: 12.3 km/h",
    "guestId": 5,
    "guestName": "Maria Silva Santos",
    "roomId": 12,
    "roomNumber": "305",
    "hotelName": "Hotel Grand Plaza"
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem de confirmação |
| data | object | Dados da reserva confirmada |
| data.id | long | ID da reserva |
| data.confirmationCode | string | Código de confirmação |
| data.checkInDate | date | Data de entrada |
| data.checkOutDate | date | Data de saída |
| data.numberOfGuests | int | Quantidade de hóspedes |
| data.totalPrice | decimal | Preço total |
| data.status | enum | Status atualizado para CONFIRMED |
| data.paymentStatus | enum | Status do pagamento (inalterado) |
| data.specialRequests | string | Solicitações especiais |
| data.weatherChecked | boolean | Verificação climática |
| data.weatherSummary | string | Resumo do clima |
| data.guestId | long | ID do hóspede |
| data.guestName | string | Nome do hóspede |
| data.roomId | long | ID do quarto |
| data.roomNumber | string | Número do quarto |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 404 | RESERVATION_NOT_FOUND | Reserva com o ID informado não existe |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao confirmar reserva |

## Regras de Negócio
1. **Mudança de Status**: Altera status de PENDING para CONFIRMED
2. **Validação de Existência**: Verifica se a reserva existe, caso contrário lança ReservationNotFoundException
3. **Sem Validação de Status Anterior**: Sistema permite confirmar independente do status atual (idempotente)
4. **PaymentStatus Independente**: A confirmação da reserva NÃO altera automaticamente o paymentStatus
5. **Invalidação de Cache**: Remove cache da reserva (reservation:id) e do hóspede (reservation:guest:guestId)
6. **Evento de Confirmação**: Publica evento RESERVATION_CONFIRMED para integração com outros sistemas
7. **Transação**: Operação executada em transação (@Transactional) garantindo atomicidade
8. **Campo updatedAt**: Atualizado automaticamente via @PreUpdate

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP e retornar 200 OK |
| Service | ReservationService | Validar existência, atualizar status e invalidar cache |
| Repository | ReservationRepository | Persistir alteração de status |
| Mapper | ReservationMapper | Converter entidade em DTO |
| Config | CacheConfig | Invalidar cache da reserva |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/reservations/42/confirm]
    B --> C[ReservationResource.confirmReservation]
    C --> D[ReservationService.confirmReservation]
    D --> E[ReservationRepository.findById]
    E --> F{Reserva existe?}
    F -->|Não| G[ReservationNotFoundException → 404]
    F -->|Sim| H[reservation.setStatus - CONFIRMED]
    H --> I[@Transactional: persist update]
    I --> J[@PreUpdate: set updatedAt]
    J --> K[CacheConfig.delete - reservation:42]
    K --> L[CacheConfig.delete - reservation:guest:5]
    L --> M[Publicar evento CONFIRMED]
    M --> N[ReservationMapper.toDto]
    N --> O[ApiResponse.success]
    O --> P[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant ReservationService
    participant ReservationRepository
    participant CacheConfig
    participant Database
    participant ReservationMapper
    participant EventPublisher
    
    Cliente->>ReservationResource: POST /api/v1/reservations/42/confirm
    ReservationResource->>ReservationService: confirmReservation(42)
    ReservationService->>ReservationRepository: findById(42)
    ReservationRepository->>Database: SELECT * FROM reservations WHERE id = 42
    alt Reserva não existe
        Database-->>ReservationRepository: null
        ReservationRepository-->>ReservationService: Optional.empty
        ReservationService-->>ReservationResource: ReservationNotFoundException
        ReservationResource-->>Cliente: 404 RESERVATION_NOT_FOUND
    else Reserva existe
        Database-->>ReservationRepository: Reservation
        ReservationRepository-->>ReservationService: Reservation
        ReservationService->>ReservationService: reservation.setStatus(CONFIRMED)
        ReservationService->>ReservationRepository: persist(reservation)
        ReservationRepository->>Database: UPDATE reservations SET status = CONFIRMED WHERE id = 42
        Database-->>ReservationRepository: Reservation atualizada
        ReservationService->>CacheConfig: delete("reservation:42")
        ReservationService->>CacheConfig: delete("reservation:guest:5")
        ReservationService->>EventPublisher: publish(RESERVATION_CONFIRMED)
        ReservationService->>ReservationMapper: toDto(reservation)
        ReservationMapper-->>ReservationService: ReservationDto
        ReservationService-->>ReservationResource: ReservationDto
        ReservationResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Reservation (
        Long id PK,
        String confirmationCode,
        ReservationStatus status "PENDING → CONFIRMED",
        PaymentStatus paymentStatus,
        Long guestId FK,
        Long roomId FK,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
