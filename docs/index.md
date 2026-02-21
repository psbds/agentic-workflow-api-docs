# Documentação da API de Reservas de Hotéis

Bem-vindo à documentação completa da API de Reservas de Hotéis. Esta API fornece funcionalidades para gerenciar hotéis, quartos, hóspedes e reservas, com integração de validação climática para garantir a melhor experiência ao cliente.

## 📚 Visão Geral

Esta API foi desenvolvida com **Quarkus** e utiliza as seguintes tecnologias:

- **Framework**: Quarkus 3.x
- **Persistência**: Hibernate ORM com Panache
- **Banco de Dados**: PostgreSQL
- **Cache**: Redis
- **Validação**: Jakarta Bean Validation
- **API Externa**: Open-Meteo (previsão do tempo)
- **Mensageria**: Eventos de reserva para processamento assíncrono

## 🎯 Funcionalidades Principais

### Gestão de Hotéis
- Cadastro, atualização e remoção de hotéis
- Busca por cidade, nome ou ID
- Listagem paginada
- Informações de geolocalização (latitude/longitude)

### Gestão de Quartos
- Cadastro e atualização de quartos
- Consulta de disponibilidade por período
- Múltiplos tipos de quarto (Single, Double, Suite, Deluxe, Penthouse)
- Controle de disponibilidade individual

### Gestão de Hóspedes
- Cadastro e atualização de dados pessoais
- Busca por ID, e-mail ou nome
- Armazenamento de documentação e nacionalidade

### Gestão de Reservas
- **Criação de Reservas com Validação Inteligente**:
  - Verificação automática de disponibilidade
  - Validação de condições climáticas no destino
  - Cálculo automático de preços
  - Geração de código de confirmação único
  
- **Ciclo de Vida Completo**:
  - Confirmação de reservas
  - Check-in e Check-out
  - Cancelamento com cálculo de reembolso
  - Atualização de datas e preferências

### Consulta de Clima
- Integração com Open-Meteo API
- Validação de condições climáticas para viagens
- Cache de previsões

## 📖 Estrutura da Documentação

### [API Endpoints](endpoints/README.md)
Documentação detalhada de todos os 28 endpoints da API, organizados por recurso:

- **[Hotéis](endpoints/README.md#-hotéis-hotels)** - 7 endpoints
- **[Hóspedes](endpoints/README.md#-hóspedes-guests)** - 5 endpoints
- **[Quartos](endpoints/README.md#️-quartos-rooms)** - 6 endpoints
- **[Reservas](endpoints/README.md#-reservas-reservations)** - 9 endpoints
- **[Clima](endpoints/README.md#️-clima-weather)** - 1 endpoint

Cada endpoint está documentado com:
- ✅ Descrição completa do propósito
- ✅ Informações técnicas (método, path, autenticação, cache, transação)
- ✅ Estrutura de request e response com exemplos JSON
- ✅ Lista completa de validações e regras de negócio
- ✅ Códigos de erro possíveis
- ✅ Diagramas Mermaid (fluxo, sequência, entidades)

## 🚀 Início Rápido

### Exemplo: Criar uma Reserva

**1. Criar um hóspede:**
````bash
POST /api/v1/guests
{
  "firstName": "João",
  "lastName": "Silva",
  "email": "joao.silva@example.com",
  "phoneNumber": "+55 11 98765-4321",
  "documentType": "CPF",
  "documentNumber": "123.456.789-00",
  "nationality": "Brasileira"
}
````

**2. Buscar quartos disponíveis:**
````bash
GET /api/v1/rooms/hotel/1/available?checkIn=2026-03-01&checkOut=2026-03-05
````

**3. Criar a reserva (com validação automática de clima):**
````bash
POST /api/v1/reservations
{
  "guestId": 1,
  "roomId": 5,
  "checkInDate": "2026-03-01",
  "checkOutDate": "2026-03-05",
  "numberOfGuests": 2,
  "specialRequests": "Quarto com vista para o mar"
}
````

**4. Confirmar a reserva:**
````bash
POST /api/v1/reservations/1/confirm
````

**5. Fazer check-in:**
````bash
POST /api/v1/reservations/1/checkin
````

## 🔑 Recursos Avançados

### Cache Inteligente
A API utiliza Redis para cache em múltiplos níveis:
- Cache de hotéis (listagem e individual)
- Cache de disponibilidade de quartos
- Cache de previsões do tempo
- Cache de reservas

### Validação Climática
Antes de criar uma reserva, a API:
1. Consulta a previsão do tempo para a data de check-in
2. Verifica temperatura, velocidade do vento e condições gerais
3. Bloqueia a reserva se as condições forem inadequadas (tempestades, vento excessivo, etc.)
4. Armazena o resumo climático na reserva

### Cálculo Automático de Preços
- Preço total baseado na diária do quarto × número de noites
- Cálculo de reembolso em cancelamentos:
  - **>48h antes do check-in**: Reembolso total
  - **<48h antes do check-in**: Reembolso de 50%
  - **Após check-in**: Sem reembolso

### Eventos de Reserva
O sistema publica eventos para processamento assíncrono:
- `CREATED`: Nova reserva criada
- `CONFIRMED`: Reserva confirmada
- `CANCELLED`: Reserva cancelada
- `CHECKED_IN`: Check-in realizado
- `CHECKED_OUT`: Check-out realizado
- `UPDATED`: Reserva atualizada

## 📊 Modelo de Dados

### Entidades Principais
- **Hotel**: Informações do hotel (nome, endereço, classificação, coordenadas)
- **Room**: Quartos do hotel (tipo, preço, capacidade, disponibilidade)
- **Guest**: Dados do hóspede (nome, e-mail, documentos, nacionalidade)
- **Reservation**: Reservas (datas, status, pagamento, código de confirmação)

### Relacionamentos
- Hotel → Rooms (1:N, cascade delete)
- Guest → Reservations (1:N)
- Room → Reservations (1:N)

### Estados de Reserva
````
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED
````

## ⚙️ Configurações

A API utiliza as seguintes configurações principais (via application.properties):

- `hotel.cache-ttl-minutes`: TTL do cache em minutos
- `hotel.max-reservation-days`: Duração máxima de uma reserva
- `hotel.cancellation-hours-before`: Horas antes do check-in para cancelamento gratuito
- `hotel.max-wind-speed`: Velocidade máxima de vento permitida (km/h)
- `hotel.min-temperature`: Temperatura mínima adequada (°C)
- `hotel.max-temperature`: Temperatura máxima adequada (°C)

## 📝 Convenções da API

### Formato de Resposta Padrão
````json
{
  "success": true,
  "message": "Mensagem descritiva",
  "data": { },
  "timestamp": "2026-02-21T05:50:00"
}
````

### Códigos HTTP
- `200 OK`: Operação bem-sucedida
- `201 CREATED`: Recurso criado
- `204 NO CONTENT`: Remoção bem-sucedida
- `400 BAD REQUEST`: Erro de validação
- `404 NOT FOUND`: Recurso não encontrado
- `409 CONFLICT`: Conflito (ex: quarto não disponível)
- `503 SERVICE UNAVAILABLE`: Serviço externo indisponível

### Formato de Datas
- Datas: `YYYY-MM-DD` (ISO 8601)
- Timestamps: `YYYY-MM-DDTHH:mm:ss`

## 🔗 Links Úteis

- [Documentação Completa de Endpoints](endpoints/README.md)
- [Quarkus Documentation](https://quarkus.io/)
- [Open-Meteo API](https://open-meteo.com/)

## 📞 Suporte

Para dúvidas ou sugestões, consulte a documentação detalhada de cada endpoint ou entre em contato com a equipe de desenvolvimento.

---

**Versão da API**: 1.0  
**Última Atualização**: 2026-02-21
