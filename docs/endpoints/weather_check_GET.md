# GET /api/v1/weather/check — Consultar Previsão do Tempo

## Descrição
Este endpoint consulta a previsão do tempo para uma localização específica (coordenadas geográficas) em uma data futura. Integra-se com a API Open-Meteo para obter dados meteorológicos e avalia se as condições são adequadas para viagem.

## Informações Gerais
| Propriedade         | Valor                            |
|----------------------|----------------------------------|
| **Método HTTP**      | `GET`                            |
| **Path**             | `/api/v1/weather/check`          |
| **Content-Type**     | `application/json`               |
| **Autenticação**     | Nenhuma                          |
| **Roles Permitidas** | Todas                            |
| **Transacional**     | Não                              |
| **Cache**            | Sim - chave: `weather:{latitude}:{longitude}:{date}`, TTL configurável |

## Comunicações Externas
| Serviço | Protocolo | Descrição |
|---------|-----------|-----------|
| Open-Meteo Weather API | HTTPS | Consulta de previsão meteorológica (api.open-meteo.com) |
| Redis | TCP | Armazenamento em cache das previsões |
| PostgreSQL | JDBC | Não utilizado (endpoint read-only externo) |

## Request

### Headers
| Header | Valor | Obrigatório | Descrição |
|--------|-------|-------------|-----------|
| Accept | application/json | Não | Tipo de resposta aceita |

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| latitude | double | Sim | Coordenada de latitude (-90 a 90) |
| longitude | double | Sim | Coordenada de longitude (-180 a 180) |
| date | date | Sim | Data da previsão (formato: yyyy-MM-dd, até 16 dias no futuro) |

## Response

### Sucesso — `200 OK`
````json
{
  "success": true,
  "message": "Weather forecast retrieved successfully",
  "data": {
    "temperature": 24.5,
    "windSpeed": 12.3,
    "weatherDescription": "Clear sky",
    "locationName": "São Paulo, Brasil",
    "forecastDate": "2026-03-15",
    "isSuitableForTravel": true
  },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Campos da Response
| Campo | Tipo | Descrição |
|-------|------|-----------|
| success | boolean | Indica se a operação foi bem-sucedida |
| message | string | Mensagem descritiva do resultado |
| data | object | Dados da previsão meteorológica |
| data.temperature | double | Temperatura média do dia em graus Celsius (média de temp_max e temp_min) |
| data.windSpeed | double | Velocidade máxima do vento em km/h |
| data.weatherDescription | string | Descrição textual do clima (ex: Clear sky, Rain, Thunderstorm) |
| data.locationName | string | Nome da localização (aproximado pelas coordenadas) |
| data.forecastDate | date | Data da previsão consultada |
| data.isSuitableForTravel | boolean | Indica se o clima é adequado para viagem baseado em critérios configuráveis |
| timestamp | datetime | Data e hora da resposta |

### Erros
| Código HTTP | Código de Erro | Descrição |
|-------------|----------------|-----------|
| 400 | VALIDATION_ERROR | Parâmetros ausentes ou inválidos (latitude, longitude ou date) |
| 400 | INVALID_DATE_RANGE | Data fornecida está no passado ou além do limite de 16 dias |
| 503 | WEATHER_CHECK_FAILED | Erro ao consultar API Open-Meteo ou timeout na requisição |
| 500 | INTERNAL_ERROR | Erro interno do servidor ao processar previsão |

## Regras de Negócio
1. **Validação de Coordenadas**: latitude deve estar entre -90 e 90, longitude entre -180 e 180
2. **Validação de Data**: date deve ser futura (não pode ser passado) e dentro do limite de previsão da API (geralmente 16 dias)
3. **Consulta em Cache**: Busca primeiro em cache com chave `weather:{lat}:{lon}:{date}`
4. **Chamada à API Externa**: Se não encontrado em cache, chama Open-Meteo Weather API com parâmetros:
   - `latitude`, `longitude`
   - `daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max,weather_code`
   - `start_date={date}`, `end_date={date}`
5. **Cálculo de Temperatura Média**: `avgTemp = (tempMax + tempMin) / 2.0`
6. **Mapeamento de Weather Code**: Converte código numérico em descrição textual:
   - `0` → Clear sky
   - `1-3` → Partly cloudy
   - `45, 48` → Fog
   - `51-57` → Drizzle
   - `61-67` → Rain
   - `71-77` → Snow
   - `80-82` → Rain showers
   - `85-86` → Snow showers
   - `95-99` → Thunderstorm (SEVERO)
7. **Avaliação de Adequação para Viagem**: `isSuitableForTravel = true` SE:
   - NÃO é clima severo (thunderstorm, neve intensa)
   - windSpeed < maxWindSpeed (configurável, ex: 50 km/h)
   - temperature >= minTemperature (configurável, ex: 0°C)
   - temperature <= maxTemperature (configurável, ex: 45°C)
8. **Cache de Previsão**: Armazena resultado em cache por TTL configurável (ex: 6 horas)
9. **Tratamento de Erro na API**: Se API retornar erro ou timeout, lança WeatherCheckFailedException (503)

## Camadas e Componentes Envolvidos
| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Resource | WeatherResource | Receber requisição, validar parâmetros e retornar resposta |
| Service | WeatherService | Buscar em cache, chamar backend externo, avaliar adequação e cachear |
| Backend | WeatherApiClient | Integração REST com Open-Meteo API (RestClient Quarkus) |
| Util | WeatherCodeMapper | Mapear código numérico para descrição textual |
| Mapper | WeatherMapper | Converter resposta da API em WeatherDto |
| Config | CacheConfig | Gerenciar cache de previsões |
| Config | HotelConfig | Fornecer parâmetros de validação (maxWindSpeed, minTemperature, maxTemperature) |

## Diagrama de Fluxo
````mermaid
flowchart TD
    A[Cliente HTTP] --> B[GET /api/v1/weather/check?latitude=-23.5&longitude=-46.6&date=2026-03-15]
    B --> C[WeatherResource.checkWeather]
    C --> D{Parâmetros válidos?}
    D -->|Não| E[Retornar 400 Bad Request]
    D -->|Sim| F{Data é futura e <= 16 dias?}
    F -->|Não| G[InvalidDateRangeException → 400]
    F -->|Sim| H[WeatherService.checkWeather]
    H --> I{Cache Hit?}
    I -->|Sim| J[Retornar do Cache]
    I -->|Não| K[WeatherApiClient.getForecast]
    K --> L[HTTP GET api.open-meteo.com/v1/forecast?params]
    L --> M{API respondeu?}
    M -->|Não| N[WeatherCheckFailedException → 503]
    M -->|Sim| O[Parsear JSON response]
    O --> P[Calcular avgTemp = - tempMax + tempMin - / 2]
    P --> Q[WeatherCodeMapper.toDescription - weatherCode]
    Q --> R[Avaliar isSuitableForTravel]
    R --> S{windSpeed < max AND temp in range AND NOT severe?}
    S -->|Sim| T[isSuitableForTravel = true]
    S -->|Não| U[isSuitableForTravel = false]
    T --> V[Armazenar no Cache]
    U --> V
    V --> W[WeatherMapper.toDto]
    J --> W
    W --> X[ApiResponse.success]
    X --> Y[Retornar 200 OK]
````

## Diagrama de Sequência
````mermaid
sequenceDiagram
    participant Cliente
    participant WeatherResource
    participant WeatherService
    participant CacheConfig
    participant WeatherApiClient
    participant OpenMeteoAPI
    participant WeatherCodeMapper
    participant HotelConfig
    participant WeatherMapper
    
    Cliente->>WeatherResource: GET /api/v1/weather/check?lat=-23.5&lon=-46.6&date=2026-03-15
    WeatherResource->>WeatherResource: Validar parâmetros
    alt Parâmetros inválidos
        WeatherResource-->>Cliente: 400 VALIDATION_ERROR
    else Data inválida
        WeatherResource-->>Cliente: 400 INVALID_DATE_RANGE
    else Parâmetros válidos
        WeatherResource->>WeatherService: checkWeather(-23.5, -46.6, 2026-03-15)
        WeatherService->>CacheConfig: getObject("weather:-23.5:-46.6:2026-03-15")
        alt Cache Hit
            CacheConfig-->>WeatherService: WeatherDto
        else Cache Miss
            WeatherService->>WeatherApiClient: getForecast(-23.5, -46.6, 2026-03-15)
            WeatherApiClient->>OpenMeteoAPI: GET /v1/forecast?latitude=-23.5&longitude=-46.6&daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max,weather_code&start_date=2026-03-15&end_date=2026-03-15
            alt API com erro
                OpenMeteoAPI-->>WeatherApiClient: HTTP 500 ou Timeout
                WeatherApiClient-->>WeatherService: WeatherCheckFailedException
                WeatherService-->>WeatherResource: WeatherCheckFailedException
                WeatherResource-->>Cliente: 503 WEATHER_CHECK_FAILED
            else API OK
                OpenMeteoAPI-->>WeatherApiClient: JSON (tempMax:28, tempMin:21, windMax:15, weatherCode:0)
                WeatherApiClient->>WeatherApiClient: avgTemp = (28 + 21) / 2 = 24.5
                WeatherApiClient->>WeatherCodeMapper: toDescription(0)
                WeatherCodeMapper-->>WeatherApiClient: "Clear sky"
                WeatherApiClient->>HotelConfig: getMaxWindSpeed()
                HotelConfig-->>WeatherApiClient: 50
                WeatherApiClient->>HotelConfig: getMinTemperature()
                HotelConfig-->>WeatherApiClient: 0
                WeatherApiClient->>HotelConfig: getMaxTemperature()
                HotelConfig-->>WeatherApiClient: 45
                WeatherApiClient->>WeatherApiClient: Avaliar: 15 < 50 AND 24.5 >= 0 AND 24.5 <= 45 AND NOT severe
                WeatherApiClient->>WeatherApiClient: isSuitableForTravel = true
                WeatherApiClient-->>WeatherService: WeatherData
                WeatherService->>CacheConfig: putObject("weather:...", weatherData, TTL)
            end
        end
        WeatherService->>WeatherMapper: toDto(weatherData)
        WeatherMapper-->>WeatherService: WeatherDto
        WeatherService-->>WeatherResource: WeatherDto
        WeatherResource-->>Cliente: 200 OK + ApiResponse
    end
````

## Diagrama de Entidades
````mermaid
erDiagram
    Hotel ||--o( Reservation : "possui reservas com"
    Reservation ||--|| WeatherForecast : "valida com"
    
    Hotel (
        Long id,
        Double latitude "Usado para consulta",
        Double longitude "Usado para consulta"
    )
    
    Reservation (
        Long id,
        LocalDate checkInDate "Data da previsão",
        String weatherSummary "Resultado armazenado",
        boolean weatherChecked
    )
    
    WeatherForecast (
        String cacheKey "weather:lat:lon:date",
        Double temperature,
        Double windSpeed,
        String weatherDescription,
        boolean isSuitableForTravel,
        LocalDate forecastDate
    )
````
