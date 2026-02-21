# PUT /api/v1/reservations/{id} — Atualizar Reserva

## Descrição
Este endpoint permite atualizar informações de uma reserva existente, como datas de check-in/check-out, número de hóspedes e solicitações especiais. Recalcula o preço automaticamente se as datas forem alteradas e valida conflitos de disponibilidade.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `PUT`                            |
| **Path**             | `/api/v1/reservations/{id}`      |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Invalidação - remove cache da reserva atualizada |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Validação de conflitos e atualização da reserva |
| Redis | TCP | Invalidação de cache da reserva |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Path Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| id | long | Sim | Identificador único da reserva a ser atualizada |

### Body
````json
{
  "checkInDate": "2026-03-16",
  "checkOutDate": "2026-03-20",
  "numberOfGuests": 3,
  "specialRequests": "Berço para bebê e café da manhã às 7h"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| checkInDate | date | Não | @FutureOrPresent - Nova data de entrada (formato: yyyy-MM-dd) |
| checkOutDate | date | Não | @Future - Nova data de saída (deve ser posterior ao checkIn) |
| numberOfGuests | int | Não | @Min(1) - Nova quantidade de hóspedes |
| specialRequests | string | Não | Novas solicitações especiais (substitui anteriores) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Reservation updated successfully",
  "data": {
    "id": 42,
    "confirmationCode": "HTL-A1B2C3D4",
    "checkInDate": "2026-03-16",
    "checkOutDate": "2026-03-20",
    "numberOfGuests": 3,
    "totalPrice": 600.00,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "specialRequests": "Berço para bebê e café da manhã às 7h",
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
| message | string | Mensagem de sucesso |
| data | object | Dados da reserva atualizada |
| data.id | long | ID da reserva (não muda) |
| data.confirmationCode | string | Código de confirmação (não muda) |
| data.checkInDate | date | Data de entrada atualizada |
| data.checkOutDate | date | Data de saída atualizada |
| data.numberOfGuests | int | Quantidade de hóspedes atualizada |
| data.totalPrice | decimal | Preço recalculado se datas mudaram |
| data.status | enum | Status da reserva (não muda na atualização) |
| data.paymentStatus | enum | Status do pagamento (não muda) |
| data.specialRequests | string | Solicitações especiais atualizadas |
| data.weatherChecked | boolean | Mantém valor original |
| data.weatherSummary | string | Mantém resumo original do clima |
| data.guestId | long | ID do hóspede (não muda) |
| data.guestName | string | Nome do hóspede |
| data.roomId | long | ID do quarto (não muda) |
| data.roomNumber | string | Número do quarto |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos inválidos (Bean Validation) |
| 400 | INVALID_DATE_RANGE | checkOutDate menor ou igual a checkInDate |
| 400 | INVALID_DATE_RANGE | Duração excede maxReservationDays configurado |
| 404 | RESERVATION_NOT_FOUND | Reserva com o ID informado não existe |
| 409 | ROOM_NOT_AVAILABLE | Novas datas conflitam com outras reservas do mesmo quarto |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao atualizar reserva |

## Regras de Negócio
1. **Campos Opcionais**: Apenas campos fornecidos no body são atualizados (patch parcial)
2. **Validação de Datas**: Se checkInDate OU checkOutDate fornecidos, valida que checkOut > checkIn
3. **Duração Máxima**: Se datas alteradas, valida que duração ≤ maxReservationDays
4. **Conflito de Disponibilidade**: Verifica se novas datas conflitam com outras reservas do mesmo quarto (excluindo a reserva atual da verificação)
5. **Recálculo de Preço**: Se checkInDate ou checkOutDate mudarem, recalcula totalPrice usando PriceCalculator
6. **Campos Imutáveis**: guestId, roomId, confirmationCode, status, paymentStatus, weatherChecked e weatherSummary NÃO podem ser alterados via PUT
7. **Invalidação de Cache**: Remove cache da reserva (reservation:id e reservation:guest:guestId)
8. **Evento de Atualização**: Publica evento RESERVATION_UPDATED após sucesso
9. **Transação**: Operação executada em transação (@Transactional)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição, validar e retornar 200 OK |
| Service | ReservationService | Validar datas, verificar conflitos, recalcular preço e atualizar |
| Repository | ReservationRepository | Verificar conflitos de datas e persistir atualização |
| Util | PriceCalculator | Recalcular preço se datas mudaram |
| Util | DateUtils | Validar intervalo de datas e calcular dias |
| Mapper | ReservationMapper | Converter entidade atualizada em DTO |
| Config | CacheConfig | Invalidar cache da reserva |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[PUT /api/v1/reservations/42 + body]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[ReservationResource.updateReservation]
    F --> G[ReservationService.update]
    G --> H[ReservationRepository.findById]
    H --> I{Reserva existe?}
    I -->|Não| J[ReservationNotFoundException → 404]
    I -->|Sim| K{Datas fornecidas?}
    K -->|Sim| L{checkOut > checkIn?}
    L -->|Não| M[InvalidDateRangeException → 400]
    L -->|Sim| N{Duração <= max?}
    N -->|Não| O[InvalidDateRangeException → 400]
    N -->|Sim| P[ReservationRepository.checkConflicts excluindo ID atual]
    P --> Q{Conflito?}
    Q -->|Sim| R[RoomNotAvailableException → 409]
    Q -->|Não| S[PriceCalculator.calculateTotalPrice]
    S --> T[Atualizar campos fornecidos]
    K -->|Não| T
    T --> U[@Transactional: persist update]
    U --> V[CacheConfig.delete - reservation:42, reservation:guest:5]
    V --> W[Publicar evento UPDATED]
    W --> X[ReservationMapper.toDto]
    X --> Y[ApiResponse.success]
    Y --> Z[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant Validator
    participant ReservationService
    participant ReservationRepository
    participant PriceCalculator
    participant CacheConfig
    participant Database
    participant ReservationMapper
    
    Cliente->>ReservationResource: PUT /api/v1/reservations/42 + body
    ReservationResource->>Validator: @Valid UpdateReservationRequest
    alt Validação Falha
        Validator-->>ReservationResource: ValidationException
        ReservationResource-->>Cliente: 400 Bad Request
    else Validação OK
        ReservationResource->>ReservationService: update(42, request)
        ReservationService->>ReservationRepository: findById(42)
        ReservationRepository->>Database: SELECT * FROM reservations WHERE id = 42
        alt Reserva não existe
            Database-->>ReservationRepository: null
            ReservationRepository-->>ReservationService: Optional.empty
            ReservationService-->>ReservationResource: ReservationNotFoundException
            ReservationResource-->>Cliente: 404 RESERVATION_NOT_FOUND
        else Reserva existe
            Database-->>ReservationRepository: Reservation
            ReservationService->>ReservationService: validateDateRange se datas fornecidas
            alt Datas inválidas
                ReservationService-->>ReservationResource: InvalidDateRangeException
                ReservationResource-->>Cliente: 400 INVALID_DATE_RANGE
            else Datas válidas
                ReservationService->>ReservationRepository: findConflictingReservations excluindo ID 42
                ReservationRepository->>Database: SELECT * WHERE room_id = ? AND overlap AND id != 42
                alt Conflito encontrado
                    Database-->>ReservationRepository: List(Reservation)
                    ReservationRepository-->>ReservationService: Conflitos
                    ReservationService-->>ReservationResource: RoomNotAvailableException
                    ReservationResource-->>Cliente: 409 ROOM_NOT_AVAILABLE
                else Sem conflitos
                    Database-->>ReservationRepository: Lista vazia
                    alt Datas mudaram
                        ReservationService->>PriceCalculator: calculateTotalPrice(pricePerNight, newCheckIn, newCheckOut)
                        PriceCalculator-->>ReservationService: novo totalPrice
                    end
                    ReservationService->>ReservationService: Atualizar campos fornecidos
                    ReservationService->>ReservationRepository: persist(reservation)
                    ReservationRepository->>Database: UPDATE reservations SET...
                    Database-->>ReservationRepository: Reservation atualizada
                    ReservationService->>CacheConfig: delete("reservation:42")
                    ReservationService->>CacheConfig: delete("reservation:guest:5")
                    ReservationService->>ReservationService: publishEvent(UPDATED)
                    ReservationService->>ReservationMapper: toDto(reservation)
                    ReservationMapper-->>ReservationService: ReservationDto
                    ReservationService-->>ReservationResource: ReservationDto
                    ReservationResource-->>Cliente: 200 OK + ApiResponse
                end
            end
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Reservation ||--|| Guest : "pertence a"
    Reservation ||--|| Room : "reserva"
    
    Reservation (
        Long id PK,
        String confirmationCode IMMUTABLE,
        LocalDate checkInDate UPDATABLE,
        LocalDate checkOutDate UPDATABLE,
        int numberOfGuests UPDATABLE,
        BigDecimal totalPrice RECALCULATED,
        ReservationStatus status IMMUTABLE_VIA_PUT,
        PaymentStatus paymentStatus IMMUTABLE_VIA_PUT,
        String specialRequests UPDATABLE,
        Long guestId FK IMMUTABLE,
        Long roomId FK IMMUTABLE
    )
````
