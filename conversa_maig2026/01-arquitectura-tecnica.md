# 01 — Arquitectura Tècnica

## Stack tecnològic

```
Spring Boot 3.x (Java 21)
PostgreSQL 16
Redis 7
Maven
Docker + Docker Compose
```

---

## Estructura del projecte

```
nexelocal/
├── src/main/java/com/nexelocal/
│   ├── NexeLocalApplication.java
│   ├── config/
│   │   ├── AnthropicConfig.java
│   │   ├── TwilioConfig.java
│   │   ├── RedisConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── WebhookController.java       # Webhooks WhatsApp, Telegram, Email
│   │   ├── TenantController.java        # API REST gestió tenants
│   │   └── PanelController.java         # Panel client (mode híbrid)
│   ├── service/
│   │   ├── AgentService.java            # Lògica principal de l'agent IA
│   │   ├── ConversationService.java     # Gestió historial converses
│   │   ├── TenantService.java           # Gestió tenants
│   │   ├── ToolExecutorService.java     # Execució d'eines (cites, pressupostos)
│   │   └── NotificationService.java     # Notificacions al dueño
│   ├── channel/
│   │   ├── MessagingChannel.java        # Interfície comuna canals
│   │   ├── WhatsAppChannel.java         # Implementació Twilio
│   │   ├── TelegramChannel.java         # Implementació Telegram Bot API
│   │   └── EmailChannel.java            # Implementació Resend
│   ├── agent/
│   │   ├── PromptBuilder.java           # Construeix system prompt per tenant
│   │   ├── ToolDefinitions.java         # Definició d'eines per Claude
│   │   └── ModelRouter.java             # Decideix Claude Haiku vs Mistral
│   ├── entity/
│   │   ├── Tenant.java
│   │   ├── Customer.java
│   │   ├── Conversation.java
│   │   ├── Appointment.java
│   │   └── Quote.java
│   ├── repository/
│   │   ├── TenantRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── ConversationRepository.java
│   │   ├── AppointmentRepository.java
│   │   └── QuoteRepository.java
│   └── dto/
│       ├── IncomingMessage.java
│       ├── TenantConfig.java
│       └── AgentResponse.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── db/migration/                    # Flyway migrations
│       ├── V1__create_tenants.sql
│       ├── V2__create_customers.sql
│       ├── V3__create_conversations.sql
│       ├── V4__create_appointments.sql
│       └── V5__create_quotes.sql
└── docker-compose.yml
```

---

## Esquema de base de dades

```sql
-- Negocis clients (tenants)
CREATE TABLE tenants (
    id                  VARCHAR(50) PRIMARY KEY,
    business_name       VARCHAR(200) NOT NULL,
    sector              VARCHAR(50) NOT NULL,
    size                VARCHAR(20) NOT NULL,       -- autonomo | petit | mig
    agent_mode          VARCHAR(20) DEFAULT 'AUTO', -- AUTO | HYBRID | MANUAL
    phone_number_id     VARCHAR(100),               -- Twilio número WhatsApp
    telegram_bot_token  VARCHAR(200),               -- Token bot Telegram
    email_address       VARCHAR(100),               -- Email del negoci
    owner_phone         VARCHAR(20),                -- Tel. dueño per notificacions
    owner_email         VARCHAR(100),
    tone                VARCHAR(100) DEFAULT 'professional i proper',
    active              BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMP DEFAULT NOW(),
    -- Configuració negoci
    schedule            JSONB DEFAULT '{}',
    services            JSONB DEFAULT '[]',
    pricing             JSONB DEFAULT '{}',
    employees           JSONB DEFAULT '[]',
    custom_instructions TEXT DEFAULT ''
);

-- Clients finals de cada negoci
CREATE TABLE customers (
    id              VARCHAR(50) PRIMARY KEY,
    tenant_id       VARCHAR(50) REFERENCES tenants(id),
    phone           VARCHAR(20),
    name            VARCHAR(200),
    email           VARCHAR(100),
    notes           TEXT DEFAULT '',
    last_contact    TIMESTAMP,
    total_visits    INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, phone)
);

-- Historial de converses
CREATE TABLE conversations (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) REFERENCES tenants(id),
    customer_phone  VARCHAR(20) NOT NULL,
    channel         VARCHAR(20) NOT NULL,           -- whatsapp | telegram | email
    role            VARCHAR(10) NOT NULL,            -- user | assistant
    content         TEXT NOT NULL,
    tool_calls      JSONB,
    pending_approval BOOLEAN DEFAULT FALSE,         -- mode híbrid
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Cites
CREATE TABLE appointments (
    id              VARCHAR(50) PRIMARY KEY,
    tenant_id       VARCHAR(50) REFERENCES tenants(id),
    customer_phone  VARCHAR(20),
    customer_name   VARCHAR(200),
    service         VARCHAR(200),
    date            DATE NOT NULL,
    time            TIME NOT NULL,
    duration_min    INTEGER DEFAULT 60,
    status          VARCHAR(20) DEFAULT 'pending',  -- pending|confirmed|cancelled|done
    notes           TEXT DEFAULT '',
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Pressupostos
CREATE TABLE quotes (
    id              VARCHAR(50) PRIMARY KEY,
    tenant_id       VARCHAR(50) REFERENCES tenants(id),
    customer_phone  VARCHAR(20),
    customer_name   VARCHAR(200),
    description     TEXT NOT NULL,
    items           JSONB NOT NULL,
    total           DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) DEFAULT 'sent',     -- sent|accepted|rejected
    notes           TEXT DEFAULT '',
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Índexs
CREATE INDEX idx_conversations_tenant_phone ON conversations(tenant_id, customer_phone);
CREATE INDEX idx_appointments_tenant_date ON appointments(tenant_id, date);
CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_conversations_pending ON conversations(tenant_id, pending_approval) WHERE pending_approval = TRUE;
```

---

## Entitat Tenant — camps clau

```java
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    private String id;                          // ex: "garcia-pintura"

    private String businessName;
    private String sector;                      // pintor | fisio | perruqueria...
    private String size;                        // autonomo | petit | mig

    @Enumerated(EnumType.STRING)
    private AgentMode agentMode = AgentMode.AUTO;

    // Canals
    private String phoneNumberId;               // Twilio
    private String telegramBotToken;
    private String emailAddress;

    // Notificacions dueño
    private String ownerPhone;
    private String ownerEmail;

    // Configuració agent
    private String tone;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, String> schedule;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private List<String> services;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Double> pricing;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private List<String> employees;             // per a equips (F5)

    private String customInstructions;
    private boolean active = true;
}

public enum AgentMode {
    AUTO,       // agent respon tot automàticament
    HYBRID,     // agent suggereix, dueño aprova
    MANUAL      // dueño respon, agent en silenci
}
```

---

## Flux principal d'un missatge

```
1. Webhook entra (WhatsApp/Telegram/Email)
        ↓
2. WebhookController identifica el canal i extreu:
   - tenant_id (per número de telèfon o bot token)
   - customer_phone
   - missatge de text
        ↓
3. TenantService carrega config del tenant
   (primer Redis caché, si no PostgreSQL)
        ↓
4. ConversationService carrega últims 20 missatges
        ↓
5. AgentService construeix:
   - System prompt personalitzat (PromptBuilder)
   - Historial de conversa
   - Eines disponibles (ToolDefinitions)
        ↓
6. ModelRouter decideix:
   - Dades personals → Claude Haiku 4.5
   - Tasques genèriques → Mistral Small 3.1
        ↓
7. Crida a l'API de la IA seleccionada
        ↓
8. Si Claude usa una eina → ToolExecutorService l'executa
   (comprovar disponibilitat, crear cita, generar pressupost...)
        ↓
9. Comprovació AgentMode:
   - AUTO   → envia resposta immediatament
   - HYBRID → guarda com a pendent, notifica dueño
   - MANUAL → notifica dueño, no envia res
        ↓
10. Guarda missatges a PostgreSQL
        ↓
11. Respon per WhatsApp/Telegram/Email
```

---

## Eines disponibles per als agents (Tool Use)

| Eina | Descripció | Sectors |
|---|---|---|
| `check_availability` | Consulta slots disponibles per data | Tots |
| `create_appointment` | Crea una cita | Tots |
| `cancel_appointment` | Cancel·la una cita | Tots |
| `create_quote` | Genera un pressupost amb PDF | Serveis llar, professionals |
| `get_customer_history` | Historial del client | Tots |
| `notify_owner` | Notificació urgent al dueño | Tots |
| `list_employees` | Llista empleats disponibles | F5 equips |
| `assign_employee` | Assigna empleat a tasca | F5 equips |

---

## Caché Redis

```java
// Configuració caché
@Cacheable("tenants")
public Tenant getTenant(String tenantId) { ... }

@CacheEvict("tenants")
public void updateTenant(String tenantId, TenantConfig config) { ... }

// Claus Redis
"tenant:{tenantId}"              → config del tenant (TTL: 1h)
"conv:{tenantId}:{phone}"        → conversa activa (TTL: 48h)
"ratelimit:{tenantId}"           → control de rate limiting
```

---

## Variables d'entorn (application-prod.yml)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: 6379

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model-haiku: claude-haiku-4-5-20251001
  model-sonnet: claude-sonnet-4-6

mistral:
  api-key: ${MISTRAL_API_KEY}
  model: mistral-small-latest

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN}

resend:
  api-key: ${RESEND_API_KEY}
  from-domain: nexelocal.cat

server:
  port: 8080
  tomcat:
    threads:
      max: 100
```

---

## Docker Compose (desenvolupament local)

```yaml
version: '3.8'
services:

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: nexelocal
      POSTGRES_USER: nexelocal
      POSTGRES_PASSWORD: nexelocal_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/nexelocal
      DB_USER: nexelocal
      DB_PASSWORD: nexelocal_dev
      REDIS_HOST: redis
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      TWILIO_ACCOUNT_SID: ${TWILIO_ACCOUNT_SID}
      TWILIO_AUTH_TOKEN: ${TWILIO_AUTH_TOKEN}
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
```
