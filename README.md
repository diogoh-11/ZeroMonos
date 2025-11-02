# ZeroMonos - Sistema de Gestão de Recolha de Resíduos Volumosos

## Índice

1. [Visão Geral do Projeto](#visão-geral-do-projeto)
2. [Arquitetura do Sistema](#arquitetura-do-sistema)
3. [Funcionalidades Principais](#funcionalidades-principais)
4. [Modelo de Dados](#modelo-de-dados)
5. [Abordagem de Testes](#abordagem-de-testes)
6. [Metricas Sonar](#metricas-sonar)

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

### **Impacto no Projeto:**

1. **Segurança**: Prevenção de ataques via logs
2. **Manutenibilidade**: Código mais limpo e moderno
3. **Performance**: Redução de overhead desnecessário
4. **Boas práticas**: Alinhamento com padrões Java modernos

