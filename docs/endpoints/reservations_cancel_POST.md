# POST /api/v1/reservations/{id}/cancel — Cancelar Reserva

## Descrição
Este endpoint cancela uma reserva existente e calcula o valor de reembolso baseado na política de cancelamento configurada. O valor do reembolso varia de acordo com a antecedência do cancelamento em relação à data de check-in.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/reservations/{id}/cancel` |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache da reserva |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Atualização do status e cálculo de reembolso |
| Redis | TCP | Invalidação de cache da reserva |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único da reserva a ser cancelada |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Reservation cancelled successfully. Refund amount: R$ 225.00",
  "data": {
    "id": 42,
    "confirmationCode": "HTL-A1B2C3D4",
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-18",
    "numberOfGuests": 2,
    "totalPrice": 225.00,
    "status": "CANCELLED",
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
| message | string | Mensagem com informação do valor de reembolso |
| data | object | Dados da reserva cancelada |
| data.id | long | ID da reserva |
| data.confirmationCode | string | Código de confirmação |
| data.checkInDate | date | Data de entrada original |
| data.checkOutDate | date | Data de saída original |
| data.numberOfGuests | int | Quantidade de hóspedes |
| data.totalPrice | decimal | Valor do reembolso calculado (substitui o preço original) |
| data.status | enum | Status atualizado para CANCELLED |
| data.paymentStatus | enum | Status do pagamento (a ser atualizado manualmente para REFUNDED posteriormente) |
| data.specialRequests | string | Solicitações especiais originais |
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
| 500 | INTERNAL_ERROR | Erro interno do servidor ao cancelar reserva |

## Regras de Negócio
1. **Mudança de Status**: Altera status para CANCELLED
2. **Cálculo de Reembolso**: Utiliza PriceCalculator.calculateRefundAmount baseado em:
   - **Tempo até check-in**: Calcula horas desde agora até checkInDate
   - **Política de Cancelamento**: Obtém hoursBeforePolicy da configuração (ex: 48 horas)
   - **Regras de Reembolso**:
     - Se `horasAteCheckIn ≤ 0` (já passou check-in): reembolso = **0%** (R$ 0,00)
     - Se `horasAteCheckIn > hoursBeforePolicy`: reembolso = **100%** (valor total)
     - Se entre: reembolso = **50%** (metade do valor)
3. **Atualização de Preço**: O campo totalPrice é SUBSTITUÍDO pelo valor do reembolso calculado
4. **PaymentStatus**: NÃO é alterado automaticamente (deve ser atualizado para REFUNDED manualmente após processamento do reembolso)
5. **Validação de Existência**: Verifica se a reserva existe, caso contrário lança ReservationNotFoundException
6. **Sem Validação de Status**: Sistema permite cancelar independente do status atual (inclusive CANCELLED)
7. **Invalidação de Cache**: Remove cache da reserva (reservation:id) e do hóspede (reservation:guest:guestId)
8. **Evento de Cancelamento**: Publica evento RESERVATION_CANCELLED com valor de reembolso
9. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP e retornar 200 OK com mensagem de reembolso |
| Service | ReservationService | Calcular reembolso, atualizar status e invalidar cache |
| Repository | ReservationRepository | Persistir alteração de status e novo totalPrice |
| Util | PriceCalculator | Calcular valor de reembolso baseado em política de cancelamento |
| Util | DateUtils | Calcular horas entre agora e check-in |
| Mapper | ReservationMapper | Converter entidade em DTO |
| Config | CacheConfig | Invalidar cache da reserva |
| Config | HotelConfig | Fornecer hoursBeforePolicy (ex: 48 horas) |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/reservations/42/cancel]
    B --> C[ReservationResource.cancelReservation]
    C --> D[ReservationService.cancelReservation]
    D --> E[ReservationRepository.findById]
    E --> F{Reserva existe?}
    F -->|Não| G[ReservationNotFoundException → 404]
    F -->|Sim| H[DateUtils.hoursBetween - now to checkIn]
    H --> I[HotelConfig.getHoursBeforePolicy]
    I --> J[PriceCalculator.calculateRefundAmount]
    J --> K{Horas ate checkIn?}
    K -->|<= 0| L[Reembolso = 0%]
    K -->|> policy| M[Reembolso = 100%]
    K -->|entre| N[Reembolso = 50%]
    L --> O[reservation.setTotalPrice - refundAmount]
    M --> O
    N --> O
    O --> P[reservation.setStatus - CANCELLED]
    P --> Q[@Transactional: persist update]
    Q --> R[CacheConfig.delete - caches]
    R --> S[Publicar evento CANCELLED com refundAmount]
    S --> T[ReservationMapper.toDto]
    T --> U[ApiResponse com message incluindo reembolso]
    U --> V[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant ReservationService
    participant ReservationRepository
    participant DateUtils
    participant HotelConfig
    participant PriceCalculator
    participant CacheConfig
    participant Database
    participant ReservationMapper
    participant EventPublisher
    
    Cliente->>ReservationResource: POST /api/v1/reservations/42/cancel
    ReservationResource->>ReservationService: cancelReservation(42)
    ReservationService->>ReservationRepository: findById(42)
    ReservationRepository->>Database: SELECT * FROM reservations WHERE id = 42
    alt Reserva não existe
        Database-->>ReservationRepository: null
        ReservationRepository-->>ReservationService: Optional.empty
        ReservationService-->>ReservationResource: ReservationNotFoundException
        ReservationResource-->>Cliente: 404 RESERVATION_NOT_FOUND
    else Reserva existe
        Database-->>ReservationRepository: Reservation
        ReservationRepository-->>ReservationService: Reservation (totalPrice=450.00)
        ReservationService->>DateUtils: hoursBetween(now, checkInDate)
        DateUtils-->>ReservationService: 36 horas
        ReservationService->>HotelConfig: getHoursBeforePolicy()
        HotelConfig-->>ReservationService: 48 horas
        ReservationService->>PriceCalculator: calculateRefundAmount(450.00, checkInDate, 48)
        PriceCalculator->>PriceCalculator: 36h < 48h → 50% reembolso
        PriceCalculator-->>ReservationService: 225.00
        ReservationService->>ReservationService: reservation.setTotalPrice(225.00)
        ReservationService->>ReservationService: reservation.setStatus(CANCELLED)
        ReservationService->>ReservationRepository: persist(reservation)
        ReservationRepository->>Database: UPDATE reservations SET status=CANCELLED, total_price=225.00
        Database-->>ReservationRepository: Reservation atualizada
        ReservationService->>CacheConfig: delete("reservation:42")
        ReservationService->>CacheConfig: delete("reservation:guest:5")
        ReservationService->>EventPublisher: publish(RESERVATION_CANCELLED, refundAmount=225.00)
        ReservationService->>ReservationMapper: toDto(reservation)
        ReservationMapper-->>ReservationService: ReservationDto
        ReservationService-->>ReservationResource: ReservationDto + refundAmount
        ReservationResource-->>Cliente: 200 OK + message: "Refund amount: R$ 225.00"
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Reservation (
        Long id PK,
        String confirmationCode,
        LocalDate checkInDate,
        ReservationStatus status "ANY → CANCELLED",
        BigDecimal totalPrice "ORIGINAL → REFUND_AMOUNT",
        PaymentStatus paymentStatus "UNCHANGED_BY_CANCEL",
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
