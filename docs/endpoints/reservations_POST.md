# POST /api/v1/reservations — Criar Nova Reserva

## Descrição
Este endpoint cria uma nova reserva de quarto no sistema. Executa validações completas de disponibilidade, consulta condições climáticas no destino e calcula o preço total baseado na quantidade de diárias. É o endpoint mais crítico do sistema de reservas.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `POST`                           |
| **Path**             | `/api/v1/reservations`           |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Sim                              |
| **Cache**            | Armazena cache da reserva criada com chave `reservation:{id}` |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| PostgreSQL | JDBC | Validação de Guest/Room, verificação de conflitos de datas, persistência da reserva |
| Redis | TCP | Armazenamento em cache da reserva criada |
| Open-Meteo Weather API | HTTPS | Consulta de previsão climática para a data de check-in no local do hotel |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Content-Type | application/json | Sim | Tipo do corpo da requisição |
| Accept | application/json | Não | Tipo de resposta aceita |

### Body
````json
{
  "guestId": 5,
  "roomId": 12,
  "checkInDate": "2026-03-15",
  "checkOutDate": "2026-03-18",
  "numberOfGuests": 2,
  "specialRequests": "Quarto com vista para o mar, cama king size"
}
````

### Campos do Request
| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| guestId | long | Sim | @NotNull - ID do hóspede deve existir no banco |
| roomId | long | Sim | @NotNull - ID do quarto deve existir no banco |
| checkInDate | date | Sim | @NotNull, @FutureOrPresent - Data de entrada (hoje ou futura, formato ISO: yyyy-MM-dd) |
| checkOutDate | date | Sim | @NotNull, @Future - Data de saída (deve ser futura e posterior ao checkIn) |
| numberOfGuests | int | Sim | @Min(1) - Quantidade de hóspedes (mínimo 1) |
| specialRequests | string | Não | Solicitações especiais do hóspede (opcional) |

## Response

### Sucesso — `201 CREATED`
````json
{
  "success": true,
  "message": "Reservation created successfully",
  "data": {
    "id": 42,
    "confirmationCode": "HTL-A1B2C3D4",
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-18",
    "numberOfGuests": 2,
    "totalPrice": 450.00,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "specialRequests": "Quarto com vista para o mar, cama king size",
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
| data | object | Dados da reserva criada |
| data.id | long | ID único da reserva gerado automaticamente |
| data.confirmationCode | string | Código de confirmação no formato HTL-XXXXXXXX (8 caracteres hexadecimais) |
| data.checkInDate | date | Data de entrada (formato: yyyy-MM-dd) |
| data.checkOutDate | date | Data de saída (formato: yyyy-MM-dd) |
| data.numberOfGuests | int | Quantidade de hóspedes |
| data.totalPrice | decimal | Preço total calculado (pricePerNight × dias) |
| data.status | enum | Status da reserva (sempre PENDING na criação) |
| data.paymentStatus | enum | Status do pagamento (sempre PENDING na criação) |
| data.specialRequests | string | Solicitações especiais fornecidas |
| data.weatherChecked | boolean | Indica se verificação climática foi executada (sempre true) |
| data.weatherSummary | string | Resumo do clima no formato: descrição - Temp: X°C - Wind: Y km/h |
| data.guestId | long | ID do hóspede |
| data.guestName | string | Nome completo do hóspede (firstName + lastName) |
| data.roomId | long | ID do quarto reservado |
| data.roomNumber | string | Número do quarto |
| data.hotelName | string | Nome do hotel |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Campos obrigatórios ausentes ou inválidos (Bean Validation) |
| 400 | INVALID_DATE_RANGE | checkOutDate menor ou igual a checkInDate |
| 400 | INVALID_DATE_RANGE | Duração da reserva excede o máximo configurado (maxReservationDays) |
| 404 | GUEST_NOT_FOUND | Hóspede com guestId não existe no sistema |
| 404 | ROOM_NOT_FOUND | Quarto com roomId não existe no sistema |
| 409 | ROOM_NOT_AVAILABLE | Quarto possui reservas conflitantes no período solicitado |
| 503 | WEATHER_CHECK_FAILED | Condições climáticas inadequadas para viagem ou erro na API de clima |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao criar reserva |

## Regras de Negócio
1. **Validação de Datas**: checkOutDate deve ser MAIOR que checkInDate, caso contrário lança InvalidDateRangeException
2. **Duração Máxima**: A quantidade de dias entre check-in e check-out não pode exceder o valor configurado em maxReservationDays (configurável via application.properties)
3. **Validação de Existência**: Guest e Room devem existir no banco de dados, caso contrário lança NotFoundException
4. **Verificação de Disponibilidade**: Sistema consulta todas as reservas do quarto e verifica se há sobreposição de datas. Há conflito se: (checkIn < outraCheckOut) AND (checkOut > outraCheckIn). Se encontrado conflito, lança RoomNotAvailableException
5. **Verificação Climática**: Sistema consulta a API Open-Meteo com latitude/longitude do hotel e data de check-in. Avalia se clima é adequado baseado em: temperatura dentro da faixa configurada (minTemperature a maxTemperature), velocidade do vento abaixo do limite (maxWindSpeed), ausência de condições severas (tempestades, neve intensa). Se inadequado ou erro na API, lança WeatherCheckFailedException
6. **Cálculo de Preço**: Usa PriceCalculator.calculateTotalPrice que multiplica pricePerNight do quarto pela quantidade de dias (usando DateUtils.daysBetween)
7. **Geração de Código**: ConfirmationCodeGenerator gera código único no formato HTL-XXXXXXXX usando UUID (8 caracteres hexadecimais em uppercase)
8. **Status Inicial**: Toda reserva é criada com status=PENDING e paymentStatus=PENDING
9. **Resumo de Clima**: Armazena em weatherSummary uma string formatada com: descrição do clima + temperatura média + velocidade do vento
10. **Evento de Criação**: Publica evento RESERVATION_CREATED após persistência bem-sucedida
11. **Cache**: Armazena a reserva criada em cache com chave reservation:id e TTL configurável
12. **Transação**: Toda operação é executada em transação (@Transactional) garantindo atomicidade

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | ReservationResource | Receber requisição HTTP, validar com Bean Validation e retornar 201 Created |
| Service | ReservationService | Orquestrar validações, consultar clima, calcular preço, gerar código e persistir |
| Service | WeatherService | Executar verificação climática via API externa e avaliar adequação para viagem |
| Backend | WeatherApiClient | Integração REST com Open-Meteo Weather API |
| Repository | GuestRepository | Validar existência do hóspede |
| Repository | RoomRepository | Validar existência do quarto e obter dados para cálculo |
| Repository | ReservationRepository | Verificar conflitos de datas e persistir reserva |
| Util | PriceCalculator | Calcular preço total baseado em diárias |
| Util | ConfirmationCodeGenerator | Gerar código de confirmação único HTL-XXXXXXXX |
| Util | DateUtils | Calcular quantidade de dias e validar intervalos |
| Mapper | ReservationMapper | Converter entidade Reservation em ReservationDto |
| Config | CacheConfig | Armazenar reserva em cache Redis |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[POST /api/v1/reservations + body JSON]
    B --> C[Bean Validation]
    C --> D{Validação OK?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F[ReservationResource.createReservation]
    F --> G[ReservationService.create]
    G --> H{checkOutDate > checkInDate?}
    H -->|Não| I[InvalidDateRangeException → 400]
    H -->|Sim| J{Duração <= maxReservationDays?}
    J -->|Não| K[InvalidDateRangeException → 400]
    J -->|Sim| L[GuestRepository.findById]
    L --> M{Guest existe?}
    M -->|Não| N[NotFoundException → 404]
    M -->|Sim| O[RoomRepository.findById]
    O --> P{Room existe?}
    P -->|Não| Q[NotFoundException → 404]
    P -->|Sim| R[ReservationRepository.checkConflicts]
    R --> S{Conflito de datas?}
    S -->|Sim| T[RoomNotAvailableException → 409]
    S -->|Não| U[WeatherService.checkWeather]
    U --> V{Clima adequado?}
    V -->|Não| W[WeatherCheckFailedException → 503]
    V -->|Sim| X[PriceCalculator.calculateTotalPrice]
    X --> Y[ConfirmationCodeGenerator.generate]
    Y --> Z[Criar Reservation: status=PENDING, paymentStatus=PENDING]
    Z --> AA[ReservationRepository.persist]
    AA --> AB[INSERT INTO reservations]
    AB --> AC[CacheConfig.put - reservation:id]
    AC --> AD[Publicar evento CREATED]
    AD --> AE[ReservationMapper.toDto]
    AE --> AF[ApiResponse.success]
    AF --> AG[Retornar 201 Created]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant ReservationResource
    participant Validator
    participant ReservationService
    participant GuestRepository
    participant RoomRepository
    participant ReservationRepository
    participant WeatherService
    participant WeatherApiClient
    participant PriceCalculator
    participant ConfirmationCodeGenerator
    participant CacheConfig
    participant Database
    participant ReservationMapper
    
    Cliente->>ReservationResource: POST /api/v1/reservations + body
    ReservationResource->>Validator: @Valid CreateReservationRequest
    alt Validação Falha
        Validator-->>ReservationResource: ValidationException
        ReservationResource-->>Cliente: 400 Bad Request
    else Validação OK
        ReservationResource->>ReservationService: create(request)
        ReservationService->>ReservationService: validateDateRange
        alt checkOutDate <= checkInDate
            ReservationService-->>ReservationResource: InvalidDateRangeException
            ReservationResource-->>Cliente: 400 INVALID_DATE_RANGE
        else Datas válidas
            ReservationService->>GuestRepository: findById(guestId)
            GuestRepository->>Database: SELECT * FROM guests WHERE id = ?
            alt Guest não existe
                Database-->>GuestRepository: null
                GuestRepository-->>ReservationService: Optional.empty
                ReservationService-->>ReservationResource: NotFoundException
                ReservationResource-->>Cliente: 404 GUEST_NOT_FOUND
            else Guest existe
                Database-->>GuestRepository: Guest
                ReservationService->>RoomRepository: findById(roomId)
                RoomRepository->>Database: SELECT * FROM rooms WHERE id = ?
                alt Room não existe
                    Database-->>RoomRepository: null
                    RoomRepository-->>ReservationService: Optional.empty
                    ReservationService-->>ReservationResource: NotFoundException
                    ReservationResource-->>Cliente: 404 ROOM_NOT_FOUND
                else Room existe
                    Database-->>RoomRepository: Room
                    ReservationService->>ReservationRepository: findConflictingReservations(roomId, checkIn, checkOut)
                    ReservationRepository->>Database: SELECT * FROM reservations WHERE room_id = ? AND overlap
                    alt Conflito encontrado
                        Database-->>ReservationRepository: List(Reservation)
                        ReservationRepository-->>ReservationService: List não vazia
                        ReservationService-->>ReservationResource: RoomNotAvailableException
                        ReservationResource-->>Cliente: 409 ROOM_NOT_AVAILABLE
                    else Sem conflitos
                        Database-->>ReservationRepository: Lista vazia
                        ReservationService->>WeatherService: checkWeather(hotel.latitude, hotel.longitude, checkInDate)
                        WeatherService->>WeatherApiClient: getForecast(lat, lon, checkInDate)
                        WeatherApiClient->>WeatherApiClient: HTTP GET Open-Meteo API
                        alt Clima inadequado ou erro API
                            WeatherApiClient-->>WeatherService: WeatherCheckFailedException
                            WeatherService-->>ReservationService: WeatherCheckFailedException
                            ReservationService-->>ReservationResource: WeatherCheckFailedException
                            ReservationResource-->>Cliente: 503 WEATHER_CHECK_FAILED
                        else Clima adequado
                            WeatherApiClient-->>WeatherService: WeatherDto
                            ReservationService->>PriceCalculator: calculateTotalPrice(pricePerNight, checkIn, checkOut)
                            PriceCalculator-->>ReservationService: BigDecimal totalPrice
                            ReservationService->>ConfirmationCodeGenerator: generate()
                            ConfirmationCodeGenerator-->>ReservationService: HTL-XXXXXXXX
                            ReservationService->>ReservationRepository: persist(reservation)
                            ReservationRepository->>Database: INSERT INTO reservations
                            Database-->>ReservationRepository: Reservation com ID
                            ReservationRepository-->>ReservationService: Reservation
                            ReservationService->>CacheConfig: putObject(reservation:id, reservation, TTL)
                            ReservationService->>ReservationService: publishEvent(CREATED)
                            ReservationService->>ReservationMapper: toDto(reservation)
                            ReservationMapper-->>ReservationService: ReservationDto
                            ReservationService-->>ReservationResource: ReservationDto
                            ReservationResource-->>Cliente: 201 Created + ApiResponse
                        end
                    end
                end
            end
        end
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Guest ||--o( Reservation : "faz"
    Room ||--o( Reservation : "é reservado por"
    Hotel ||--o( Room : "possui"
    
    Guest (
        Long id PK,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String documentType,
        String documentNumber,
        String nationality,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
    
    Hotel (
        Long id PK,
        String name,
        String address,
        String city,
        String country,
        Double latitude,
        Double longitude,
        String phoneNumber,
        String email
    )
    
    Room (
        Long id PK,
        String roomNumber,
        RoomType roomType,
        BigDecimal pricePerNight,
        int maxOccupancy,
        boolean isAvailable,
        int floorNumber,
        Long hotelId FK
    )
    
    Reservation (
        Long id PK,
        String confirmationCode,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        String specialRequests,
        boolean weatherChecked,
        String weatherSummary,
        Long guestId FK,
        Long roomId FK,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    )
````
