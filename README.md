# ZeroMonos - Sistema de Gestão de Recolha de Resíduos Volumosos

## Índice

1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Arquitetura do Sistema](#2-arquitetura-do-sistema)
3. [Funcionalidades Principais](#3-funcionalidades-principais)
4. [Modelo de Dados](#4-modelo-de-dados)
5. [Abordagem de Testes](#5-abordagem-de-testes)
6. [Metricas Sonar](#6-metricas-sonar)

---

## 1-Visão Geral do Projeto

**ZeroMonos** é uma aplicação web para gestão de recolha de resíduos volumosos (monos) desenvolvida com Spring Boot. O sistema permite que cidadãos agendem a recolha de itens grandes (móveis, eletrodomésticos, etc.) e que funcionários municipais gerenciem essas solicitações.

### Tecnologias Utilizadas

- **Backend**: Spring Boot 3.x, Java 21
- **Frontend**: HTML, CSS, JavaScript (Vanilla)
- **Banco de Dados**: PostgreSQL/H2 (via TestContainers nos testes)
- **Testes**: 
  - JUnit 5
  - Cucumber (BDD)
  - Selenium WebDriver
  - RestAssured
  - Testcontainers

---

## 2-Arquitetura do Sistema

### Estrutura de Camadas

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Controllers REST + Static Pages)  │
├─────────────────────────────────────┤
│         Service Layer               │
│    (Business Logic)                 │
├─────────────────────────────────────┤
│         Data Layer                  │
│    (Repositories + Entities)        │
└─────────────────────────────────────┘
```

### Componentes Principais

#### 1. **Boundary Layer** (`boundary/`)

- **`BookingController`**: API REST para criação e consulta de reservas
  - `POST /api/bookings` - Criar nova reserva
  - `GET /api/bookings/{token}` - Consultar reserva por token
  - `PUT /api/bookings/{token}/cancel` - Cancela reserva por token
  - `GET /api/bookings/municipalities` - Listar municipios disponiveis
  
- **`StaffBookingController`**: API REST para gestão de reservas (staff)
  - `GET /api/staff/bookings` - Listar todas as reservas recebe tambem o municipio por default fica Todas
  - `PATCH /api/staff/bookings/{token}/status` - Atualizar status

- **`RestExceptionHandler`**: Tratamento centralizado de exceções

#### 2. **Service Layer** (`services/`)

- **`BookingService`**: Interface de serviço
- **`BookingServiceImpl`**: Implementação da lógica de negócio
  - Validações de regras de negócio
  - Geração de tokens únicos
  - Gestão de mudanças de estado
  - Validação de datas (não permite domingos, datas passadas)

- **`MunicipalityImportService`**: Importação de municípios (dados iniciais)

#### 3. **Data Layer** (`data/`)

**Entidades:**

- **`Booking`**: Representa uma reserva
- **`Municipality`**: Representa um município
- **`StateChange`**: Histórico de mudanças de estado
 

**Enums:**

- **`BookingStatus`**: Estados possíveis
- **`TimeSlot`**: Períodos de recolha


**Repositórios:**

- **`BookingRepository`**: JPA Repository para Booking
- **`MunicipalityRepository`**: JPA Repository para Municipality

#### 4. **DTOs** (`dto/`)

- **`BookingRequestDTO`**: Dados para criar reserva
- **`BookingResponseDTO`**: Resposta com dados da reserva

---

## 3-Funcionalidades Principais

### 1. **Criação de Reserva (Cidadão)**

**Página**: `booking-form.html`

**Fluxo:**
1. Cidadão acessa o formulário
2. Seleciona município (com autocomplete)
3. Escolhe data (não permite domingos nem datas passadas)
4. Seleciona período (Manhã/Tarde)
5. Descreve o item a recolher
6. Submete o formulário
7. Sistema gera token único
8. Exibe token para consulta futura

**Validações:**
- Data não pode ser passada
- Data não pode ser domingo
- Município deve existir
- Descrição é obrigatória

### 2. **Consulta de Reserva (Cidadão)**

**Página**: `booking-view.html`

**Fluxo:**
1. Cidadão insere token recebido
2. Sistema busca reserva
3. Exibe detalhes: status, data, município, descrição

### 3. **Gestão de Reservas (Staff)**

**Página**: `staff-bookings.html`

**Funcionalidades:**
- Listar todas as reservas
- Filtrar por município
- Ver detalhes completos
- Alterar status das reservas:
  - `RECEIVED` → `ASSIGNED` (Atribuir)
  - `ASSIGNED` → `IN_PROGRESS` (Iniciar)
  - `IN_PROGRESS` → `COMPLETED` (Concluir)
  - Qualquer → `CANCELLED` (Cancelar)

**Validações de Transição de Estado:**
- Apenas transições válidas são permitidas
- Histórico de mudanças é mantido em `StateChange`

---

## 4-Modelo de Dados

### Diagrama de Relacionamentos

```
┌──────────────┐         ┌──────────────┐
│ Municipality │◄────────│   Booking    │
│──────────────│ N     1 │──────────────│
│ id           │         │ id           │
│ name         │         │ token        │
│ district     │         │ municipality │
└──────────────┘         │ requestedDate│
                         │ timeSlot     │
                         │ description  │
                         │ status       │
                         │ createdAt    │
                         └──────┬───────┘
                                │ 1
                                │
                                │ N
                         ┌──────▼───────┐
                         │ StateChange  │
                         │──────────────│
                         │ id           │
                         │ booking      │
                         │ fromStatus   │
                         │ toStatus     │
                         │ changedAt    │
                         └──────────────┘
```

---

## 5-Abordagem de Testes

### Pirâmide de Testes Implementada

```
           ╱ ╲
          ╱E2E╲           ← Testes Funcionais (Cucumber + Selenium)
         ╱─────╲
        ╱ API   ╲         ← Testes de Integração (RestAssured)
       ╱─────────╲
      ╱  Service  ╲       ← Testes Unitários (JUnit + Mockito)
     ╱─────────────╲
    ╱  Repository   ╲     ← Testes de Repositório (Testcontainers)
   ╱─────────────────╲
```

### Tipos de Testes

#### A) Testes Unitários - Regras de Domínio

**Localização**: `test/java/tqs/zeromonos/BookingServiceImplUnitTest.java`

**Objetivo**: Validar regras de negócio da camada de serviço isoladamente, usando mocks para todas as dependências externas.

**Regras de Domínio Testadas:**

1. **Validação de Data - Não permite domingos**
2. **Validação de Data - Não permite datas passadas**
3. **Validação de Município - Deve existir**
4. **Geração de Token - Deve ser único e ter 12 caracteres**
5. **Status Inicial - Deve ser RECEIVED**
6. **Cancelamento - Deve adicionar StateChange**

**Exemplo: `BookingServiceImplUnitTest.java`**


#### B) Testes de Serviço com Isolamento de Dependências

**Localização**: `test/java/tqs/zeromonos/isolationtests/`

**Objetivo**: Testar a camada de serviço com **isolamento de dependências externas** (repositórios são mockados), mas com foco em fluxos mais complexos e integrações entre componentes internos.

**Diferença do Teste Unitário:**
- Testes **unitários** focam em regras de domínio individuais
- Testes de **serviço** focam em fluxos completos e coordenação entre componentes

**Exemplo: `BookingServiceTest.java`**


#### C) Testes com RestAssured (API Completa)

**Localização**: `test/java/tqs/zeromonos/RestAssureTest.java`

**Objetivo**: Testar endpoints REST de forma integrada, garantindo serialização JSON, status HTTP e validações.



### D) Testes Funcionais (BDD com Selenium)

**Localização**: `test/java/tqs/zeromonos/functionals/`

**Objetivo**: Testar a interface web completa seguindo especificações em linguagem natural (Gherkin), simulando interação real do usuário.

**Estrutura:**

```
functionals/
├── RunCucumberTest.java          # Runner do Cucumber
├── TestContext.java              # Gerencia WebDriver e compartilha dados
├── CommonSteps.java              # Steps reutilizáveis (navegação comum)
├── BookingSteps.java             # Steps de criação de reserva (cidadão)
├── BookingViewSteps.java         # Steps de consulta de reserva
└── StaffBookingSteps.java        # Steps de gestão (staff)
```

**Features**


```
resources/
├── 1-booking.feature             # Feature para fazer uma reserva
├── 2-searchReserve.feature       # Feature para procurar por uma reserva
└── 3-cahngeStateStaff            # Feature para mudar o estado de uma reserva 
```

---

## 6-Metricas Sonar


### **1. Uso de `Stream.toList()` vs `Collectors.toList()`**
- **Problema**: Uso de API antiga e verbosa
- **Correção**: Substituí por `Stream.toList()` (Java 16+)
- **Melhoria**: Código mais conciso e lista imutável por padrão

### **2. Log de dados controlados pelo usuário**
- **Problema**: Risco de Log Injection e exposição de dados
- **Correção**: Remover/Sanitizar dados do usuário nos logs
- **Melhoria**: Segurança reforçada e logs mais limpos

### **3. Tratamento desnecessário de exceções**
- **Problema**: Catch apenas para log e rethrow da mesma exceção
- **Correção**: Remover try-catch ou adicionar contexto
- **Melhoria**: Código mais limpo e exceções mais informativas


### **4. Melhorias de Coverage**
- **Correção**: Adicionados testes específicos para aumentar cobertura de código e cenários críticos



### **Impacto no Projeto:**

1. **Segurança**: Prevenção de ataques via logs
2. **Manutenibilidade**: Código mais limpo e moderno
3. **Performance**: Redução de overhead desnecessário
4. **Boas práticas**: Alinhamento com padrões Java modernos





LightHouse Results:

Testing: Homepage
Results:
  PASS performance     : 92/100 (min: 70)
  PASS accessibility   : 100/100 (min: 80)
  PASS best-practices  : 96/100 (min: 80)
  PASS seo             : 91/100 (min: 80)

Testing: Booking Form
Results:
  PASS performance     : 92/100 (min: 70)
  PASS accessibility   : 100/100 (min: 80)
  PASS best-practices  : 96/100 (min: 80)
  PASS seo             : 91/100 (min: 80)

Testing: Booking View
Results:
  PASS performance     : 84/100 (min: 70)
  PASS accessibility   : 100/100 (min: 80)
  PASS best-practices  : 96/100 (min: 80)
  PASS seo             : 91/100 (min: 80)

Testing: Staff Bookings
Results:
  PASS performance     : 90/100 (min: 70)
  PASS accessibility   : 100/100 (min: 80)
  PASS best-practices  : 96/100 (min: 80)
  PASS seo             : 91/100 (min: 80)

LIGHTHOUSE TEST SUMMARY
Pages tested: 4 | Passed: 4 | Failed: 0


K6 - Performance Test

THRESHOLDS 

    errors
    ✓ 'rate<0.1' rate=0.00%

    http_req_duration
    ✓ 'p(95)<500' p(95)=13.03ms

    http_req_failed
    ✓ 'rate<0.1' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 10148   65.760383/s
    checks_succeeded...: 100.00% 10148 out of 10148
    checks_failed......: 0.00%   0 out of 10148

    ✓ municipalities status 200
    ✓ municipalities response time OK
    ✓ create booking status 200
    ✓ create booking response time OK
    ✓ create booking returns token
    ✓ get booking status 200
    ✓ get booking response time OK
    ✓ get booking correct data
    ✓ cancel booking successful
    ✓ cancel booking response time OK

    CUSTOM
    errors.........................: 0.00%  0 out of 0

    HTTP
    http_req_duration..............: avg=6.84ms min=1.01ms med=6.54ms max=150.18ms p(90)=10.5ms p(95)=13.03ms
      { expected_response:true }...: avg=6.84ms min=1.01ms med=6.54ms max=150.18ms p(90)=10.5ms p(95)=13.03ms
    http_req_failed................: 0.00%  0 out of 3897
    http_reqs......................: 3897   25.253076/s

    EXECUTION
    iteration_duration.............: avg=5.02s  min=5.01s  med=5.02s  max=5.2s     p(90)=5.03s  p(95)=5.04s  
    iterations.....................: 1178   7.633596/s
    vus............................: 1      min=1         max=100
    vus_max........................: 100    min=100       max=100

    NETWORK
    data_received..................: 6.5 MB 42 kB/s
    data_sent......................: 623 kB 4.0 kB/s


K6 - Spike-test

  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(99)<2000' p(99)=59.84ms

    http_req_failed
    ✓ 'rate<0.2' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 32214   458.3397/s
    checks_succeeded...: 100.00% 32214 out of 32214
    checks_failed......: 0.00%   0 out of 32214

    ✓ status 200
    ✓ response time acceptable

    HTTP
    http_req_duration..............: avg=11.06ms min=1.49ms  med=8.33ms   max=201.32ms p(90)=20.27ms p(95)=30.37ms 
      { expected_response:true }...: avg=11.06ms min=1.49ms  med=8.33ms   max=201.32ms p(90)=20.27ms p(95)=30.37ms 
    http_req_failed................: 0.00%  0 out of 16107
    http_reqs......................: 16107  229.16985/s

    EXECUTION
    iteration_duration.............: avg=512.4ms min=501.7ms med=509.63ms max=702.18ms p(90)=522.4ms p(95)=532.47ms
    iterations.....................: 16107  229.16985/s
    vus............................: 1      min=1          max=200
    vus_max........................: 200    min=200        max=200

    NETWORK
    data_received..................: 72 MB  1.0 MB/s
    data_sent......................: 1.6 MB 22 kB/s


K6 - Stress-Test


█ THRESHOLDS 

    errors
    ✓ 'rate<0.3' rate=0.00%

    http_req_duration
    ✓ 'p(95)<2000' p(95)=1.59s

    http_req_failed
    ✓ 'rate<0.3' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 15711   440.402547/s
    checks_succeeded...: 100.00% 15711 out of 15711
    checks_failed......: 0.00%   0 out of 15711

    ✓ municipalities OK
    ✓ booking created

    CUSTOM
    errors.........................: 0.00%  0 out of 0

    HTTP
    http_req_duration..............: avg=430.71ms min=2.14ms   med=215.79ms max=2.2s  p(90)=1.12s p(95)=1.59s
      { expected_response:true }...: avg=430.71ms min=2.14ms   med=215.79ms max=2.2s  p(90)=1.12s p(95)=1.59s
    http_req_failed................: 0.00%  0 out of 15712
    http_reqs......................: 15712  440.430578/s

    EXECUTION
    iteration_duration.............: avg=933.29ms min=502.73ms med=718.14ms max=2.71s p(90)=1.62s p(95)=2.09s
    iterations.....................: 15711  440.402547/s
    vus............................: 122    min=6          max=991 
    vus_max........................: 1000   min=1000       max=1000

    NETWORK
    data_received..................: 27 MB  755 kB/s
    data_sent......................: 3.2 MB 89 kB/s
