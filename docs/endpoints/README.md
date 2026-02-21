# Documentação de Endpoints da API

Este diretório contém a documentação completa de todos os endpoints da API de Reservas de Hotéis. Cada endpoint está documentado em um arquivo Markdown separado com informações detalhadas sobre request, response, validações, regras de negócio e diagramas.

## Índice de Endpoints

### 🏨 Hotéis (Hotels)

| Método | Path | Descrição | Arquivo |
|--------|------|-----------|---------|
| GET | `/api/v1/hotels` | Listar hotéis (paginado) | [hotels_GET.md](hotels_GET.md) |
| GET | `/api/v1/hotels/{id}` | Buscar hotel por ID | [hotels_by_id_GET.md](hotels_by_id_GET.md) |
| GET | `/api/v1/hotels/city/{city}` | Buscar hotéis por cidade | [hotels_city_GET.md](hotels_city_GET.md) |
| GET | `/api/v1/hotels/search?name={name}` | Buscar hotéis por nome | [hotels_search_GET.md](hotels_search_GET.md) |
| POST | `/api/v1/hotels` | Criar novo hotel | [hotels_POST.md](hotels_POST.md) |
| PUT | `/api/v1/hotels/{id}` | Atualizar hotel | [hotels_by_id_PUT.md](hotels_by_id_PUT.md) |
| DELETE | `/api/v1/hotels/{id}` | Deletar hotel | [hotels_by_id_DELETE.md](hotels_by_id_DELETE.md) |

### 🧑 Hóspedes (Guests)

| Método | Path | Descrição | Arquivo |
|--------|------|-----------|---------|
| GET | `/api/v1/guests/{id}` | Buscar hóspede por ID | [guests_by_id_GET.md](guests_by_id_GET.md) |
| GET | `/api/v1/guests/email/{email}` | Buscar hóspede por e-mail | [guests_email_GET.md](guests_email_GET.md) |
| GET | `/api/v1/guests/search?name={name}` | Buscar hóspedes por nome | [guests_search_GET.md](guests_search_GET.md) |
| POST | `/api/v1/guests` | Criar novo hóspede | [guests_POST.md](guests_POST.md) |
| PUT | `/api/v1/guests/{id}` | Atualizar hóspede | [guests_by_id_PUT.md](guests_by_id_PUT.md) |

### 🛏️ Quartos (Rooms)

| Método | Path | Descrição | Arquivo |
|--------|------|-----------|---------|
| GET | `/api/v1/rooms/{id}` | Buscar quarto por ID | [rooms_by_id_GET.md](rooms_by_id_GET.md) |
| GET | `/api/v1/rooms/hotel/{hotelId}` | Listar quartos de um hotel | [rooms_hotel_GET.md](rooms_hotel_GET.md) |
| GET | `/api/v1/rooms/hotel/{hotelId}/available` | Buscar quartos disponíveis por período | [rooms_hotel_available_GET.md](rooms_hotel_available_GET.md) |
| POST | `/api/v1/rooms` | Criar novo quarto | [rooms_POST.md](rooms_POST.md) |
| PUT | `/api/v1/rooms/{id}` | Atualizar quarto | [rooms_by_id_PUT.md](rooms_by_id_PUT.md) |
| PATCH | `/api/v1/rooms/{id}/availability` | Atualizar disponibilidade do quarto | [rooms_availability_PATCH.md](rooms_availability_PATCH.md) |

### 📅 Reservas (Reservations)

| Método | Path | Descrição | Arquivo |
|--------|------|-----------|---------|
| GET | `/api/v1/reservations/{id}` | Buscar reserva por ID | [reservations_by_id_GET.md](reservations_by_id_GET.md) |
| GET | `/api/v1/reservations/confirmation/{code}` | Buscar reserva por código de confirmação | [reservations_confirmation_GET.md](reservations_confirmation_GET.md) |
| GET | `/api/v1/reservations/guest/{guestId}` | Listar reservas de um hóspede | [reservations_guest_GET.md](reservations_guest_GET.md) |
| POST | `/api/v1/reservations` | Criar nova reserva (com validação de clima) | [reservations_POST.md](reservations_POST.md) |
| PUT | `/api/v1/reservations/{id}` | Atualizar reserva | [reservations_by_id_PUT.md](reservations_by_id_PUT.md) |
| POST | `/api/v1/reservations/{id}/confirm` | Confirmar reserva | [reservations_confirm_POST.md](reservations_confirm_POST.md) |
| POST | `/api/v1/reservations/{id}/cancel` | Cancelar reserva (com cálculo de reembolso) | [reservations_cancel_POST.md](reservations_cancel_POST.md) |
| POST | `/api/v1/reservations/{id}/checkin` | Fazer check-in | [reservations_checkin_POST.md](reservations_checkin_POST.md) |
| POST | `/api/v1/reservations/{id}/checkout` | Fazer check-out | [reservations_checkout_POST.md](reservations_checkout_POST.md) |

### 🌦️ Clima (Weather)

| Método | Path | Descrição | Arquivo |
|--------|------|-----------|---------|
| GET | `/api/v1/weather/check` | Consultar previsão do tempo | [weather_check_GET.md](weather_check_GET.md) |

## Estatísticas

- **Total de Endpoints**: 28
- **Hotéis**: 7 endpoints
- **Hóspedes**: 5 endpoints
- **Quartos**: 6 endpoints
- **Reservas**: 9 endpoints
- **Clima**: 1 endpoint

## Características Técnicas da API

### Autenticação
Atualmente, a API não possui autenticação configurada. Todos os endpoints estão abertos.

### Cache
A API utiliza Redis para cache em vários endpoints:
- **Hotéis**: Cache de listagem paginada e busca por ID
- **Quartos**: Cache de disponibilidade e busca por ID
- **Reservas**: Cache de busca por ID
- **Clima**: Cache de previsões do tempo

### Validações
- **Bean Validation**: Validações declarativas nos DTOs usando Jakarta Validation
- **Regras de Negócio**: Validações customizadas nos Services
- **Integridade Referencial**: Validações de relacionamentos entre entidades

### Integrações Externas
- **Open-Meteo API**: Consulta de previsão do tempo para validar condições climáticas nas reservas

### Mensageria
O sistema publica eventos de reserva (criação, confirmação, cancelamento, check-in, check-out) para processamento assíncrono.

### Transações
Operações de criação, atualização e exclusão são transacionais (`@Transactional`) para garantir integridade dos dados.

## Estrutura de Cada Documento

Cada arquivo de endpoint contém as seguintes seções:

1. **Descrição**: Objetivo e contexto do endpoint
2. **Informações Gerais**: Método HTTP, path, autenticação, cache, transação
3. **Comunicações Externas**: Serviços externos utilizados (DB, cache, APIs)
4. **Request**: Headers, body, query parameters, path parameters
5. **Response**: Exemplo de sucesso, campos da resposta, erros possíveis
6. **Regras de Negócio**: Validações e lógica de negócio aplicadas
7. **Camadas e Componentes**: Classes envolvidas no processamento
8. **Diagrama de Fluxo**: Fluxograma Mermaid do processamento
9. **Diagrama de Sequência**: Interação entre componentes
10. **Diagrama de Entidades**: Modelo de dados relacionado

## Convenções

### Formato de Datas
- Formato ISO 8601: `YYYY-MM-DD` (ex: `2026-02-21`)
- Timestamps: `YYYY-MM-DDTHH:mm:ss` (ex: `2026-02-21T05:50:00`)

### Formato de Resposta Padrão
Todos os endpoints retornam respostas no formato `ApiResponse<T>`:

````json
{
  "success": true,
  "message": "Mensagem descritiva",
  "data": { },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Códigos HTTP
- **200 OK**: Operação de leitura ou atualização bem-sucedida
- **201 CREATED**: Recurso criado com sucesso
- **204 NO CONTENT**: Remoção bem-sucedida
- **400 BAD REQUEST**: Erro de validação
- **404 NOT FOUND**: Recurso não encontrado
- **409 CONFLICT**: Conflito de estado (ex: quarto não disponível)
- **503 SERVICE UNAVAILABLE**: Serviço externo indisponível (ex: API de clima)

### Tipos de Dados
- **long**: Identificadores únicos
- **string**: Textos
- **int**: Números inteiros
- **double**: Números decimais (latitude, longitude)
- **BigDecimal**: Valores monetários
- **LocalDate**: Datas sem hora
- **LocalDateTime**: Datas com hora
- **boolean**: Valores verdadeiro/falso

## Enums Utilizados

### ReservationStatus
- `PENDING`: Reserva pendente de confirmação
- `CONFIRMED`: Reserva confirmada
- `CHECKED_IN`: Hóspede fez check-in
- `CHECKED_OUT`: Hóspede fez check-out
- `CANCELLED`: Reserva cancelada
- `EXPIRED`: Reserva expirada

### PaymentStatus
- `PENDING`: Pagamento pendente
- `PAID`: Pagamento efetuado
- `REFUNDED`: Valor totalmente reembolsado
- `PARTIALLY_REFUNDED`: Valor parcialmente reembolsado

### RoomType
- `SINGLE`: Quarto single
- `DOUBLE`: Quarto double
- `SUITE`: Suíte
- `DELUXE`: Quarto deluxe
- `PENTHOUSE`: Cobertura

## Contribuindo

Para adicionar documentação de novos endpoints, siga o padrão estabelecido nos arquivos existentes, garantindo que todas as seções obrigatórias estejam presentes.
