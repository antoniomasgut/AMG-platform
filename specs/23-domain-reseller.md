# Mòdul 23: Domain Reseller — Gestió i Venda de Dominis

> **Versió:** 1.0
> **Data:** 2026-05-21
> **Dependències:** Mòdul 01 (Auth), Mòdul 04 (Engine), Mòdul 07 (Billing)

---

## 1. Objectius

- Comprar i renovar dominis programàticament via API de reseller (OpenProvider)
- Gestionar el cicle de vida complet d'un domini (registre → DNS → renovació → expiració)
- Facturar als clients el domini amb marge configurable
- Automatitzar la configuració DNS per a les landings dels clients
- Oferir panell white-label: el client veu "AMG gestiona el teu domini", no OpenProvider

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Cerca de disponibilitat de dominis (check + suggeriments alternatius)
- Registre de domini a nom del client via OpenProvider API
- Renovació automàtica anual (X dies abans de l'expiració)
- Configuració automàtica de registres DNS (A, CNAME, MX) per a landings
- Transferència de dominis entrants (el client porta el seu domini)
- Gestió del WHOIS (dades del registrant)
- Preu de venda configurable per TLD amb marge sobre cost
- Facturació del domini via Mòdul 07 (Billing)
- Alertes d'expiració (30, 15, 7 dies) via Telegram/Email
- Panell d'admin: llistat de tots els dominis gestionats, estats, venciments

### 2.2 Funcionalitats excloses

- Domini autogestionat (el client compra per compte propi) — ja cobert al Mòdul 04
- Gestió de certificats SSL — Traefik ho fa automàticament
- Registre de marques o disputa de dominis
- Dominis internacionalitzats (IDN) — futur
- Revendes de hosting — fora d'abast

### 2.3 Actors

| Actor | Permisos |
|-------|----------|
| SUPER_ADMIN | Configurar credencials OpenProvider, veure tots els dominis, registrar/renovar/transferir manualment, ajustar preus per TLD |
| ADMIN | Registrar dominis per als seus tenants, veure estat, gestionar DNS |
| CLIENT | Veure el seu domini i estat, sol·licitar transferència sortint |

---

## 3. Model de dades

### 3.1 Entitat `ManagedDomain`

```
managed_domains
├── id                  UUID PK
├── tenant_id           UUID FK → tenants.id
├── landing_id          UUID FK → landings.id (nullable)
├── domain_name         VARCHAR(253) UNIQUE NOT NULL   -- ex: "perruqueria-maria.cat"
├── tld                 VARCHAR(20) NOT NULL            -- ex: "cat", "com", "es"
├── status              VARCHAR(30) NOT NULL            -- enum DomainStatus
├── provider            VARCHAR(30) DEFAULT 'OPEN_PROVIDER'
├── provider_domain_id  VARCHAR(100)                   -- ID intern d'OpenProvider
├── registrant_name     VARCHAR(150)
├── registrant_email    VARCHAR(150)
├── registrant_phone    VARCHAR(20)
├── registrant_nif      VARCHAR(20)
├── purchase_price      DECIMAL(10,2)                  -- cost real pagat al proveïdor
├── sale_price          DECIMAL(10,2)                  -- preu facturat al client
├── registered_at       TIMESTAMPTZ
├── expires_at          TIMESTAMPTZ
├── auto_renew          BOOLEAN DEFAULT TRUE
├── renewal_notified_at TIMESTAMPTZ
├── dns_configured      BOOLEAN DEFAULT FALSE
├── created_at          TIMESTAMPTZ DEFAULT NOW()
└── updated_at          TIMESTAMPTZ
```

### 3.2 Entitat `DomainDnsRecord`

```
domain_dns_records
├── id          UUID PK
├── domain_id   UUID FK → managed_domains.id
├── type        VARCHAR(10) NOT NULL    -- A, CNAME, MX, TXT
├── name        VARCHAR(253) NOT NULL   -- "@", "www", "mail"
├── value       VARCHAR(512) NOT NULL   -- IP, hostname, etc.
├── ttl         INTEGER DEFAULT 3600
├── priority    INTEGER                 -- per MX
└── created_at  TIMESTAMPTZ
```

### 3.3 Entitat `TldPricing`

```
tld_pricing
├── tld             VARCHAR(20) PK      -- "cat", "com", "es"
├── cost_register   DECIMAL(10,2)       -- cost OpenProvider (registre)
├── cost_renew      DECIMAL(10,2)       -- cost OpenProvider (renovació)
├── sale_register   DECIMAL(10,2)       -- preu al client (registre)
├── sale_renew      DECIMAL(10,2)       -- preu al client (renovació)
├── is_active       BOOLEAN DEFAULT TRUE
└── updated_at      TIMESTAMPTZ
```

### 3.4 Enum `DomainStatus`

```java
public enum DomainStatus {
    PENDING_PURCHASE,    // sol·licitat, pendent de pagament
    REGISTERING,         // en procés de registre a OpenProvider
    ACTIVE,              // registrat i funcional
    DNS_PENDING,         // registrat, pendent de configurar DNS
    TRANSFER_IN,         // transferència entrant en procés
    TRANSFER_OUT,        // transferència sortint en procés
    EXPIRING_SOON,       // menys de 30 dies per expirar
    EXPIRED,             // expirat
    CANCELLED            // cancel·lat
}
```

---

## 4. API REST

### Base URL: `/api/v1/domains`

### 4.1 Endpoints de gestió (ADMIN+)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| `GET` | `/domains` | Llistar tots els dominis (SUPER_ADMIN) |
| `GET` | `/domains/tenant/{tenantId}` | Dominis d'un tenant |
| `GET` | `/domains/{id}` | Detall d'un domini |
| `POST` | `/domains/check` | Comprovar disponibilitat |
| `POST` | `/domains/register` | Registrar domini per a un tenant |
| `POST` | `/domains/{id}/renew` | Renovar manualment |
| `POST` | `/domains/{id}/configure-dns` | Aplicar configuració DNS automàtica |
| `PUT` | `/domains/{id}/dns` | Gestionar registres DNS manualment |
| `DELETE` | `/domains/{id}` | Cancel·lar domini |

### 4.2 Endpoints de configuració (SUPER_ADMIN)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| `GET` | `/domains/admin/tld-pricing` | Llistar preus per TLD |
| `PUT` | `/domains/admin/tld-pricing/{tld}` | Actualitzar preus d'un TLD |
| `GET` | `/domains/admin/expiring` | Dominis a punt d'expirar |
| `POST` | `/domains/admin/renew-batch` | Renovar tots els que expiren aviat |

### 4.3 Endpoints de client (CLIENT)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| `GET` | `/domains/my` | Els meus dominis |
| `POST` | `/domains/{id}/transfer-out` | Sol·licitar transferència sortint |

---

## 5. DTOs

### `DomainCheckRequest`
```java
record DomainCheckRequest(
    @NotBlank String domainName  // ex: "perruqueria-maria.cat"
)
```

### `DomainCheckResponse`
```java
record DomainCheckResponse(
    String domainName,
    boolean available,
    BigDecimal salePrice,
    BigDecimal salePriceRenew,
    List<String> alternatives  // suggeriments si no disponible
)
```

### `RegisterDomainRequest`
```java
record RegisterDomainRequest(
    @NotBlank String domainName,
    @NotNull UUID tenantId,
    UUID landingId,             // opcional — per configurar DNS automàticament
    boolean autoRenew,
    // Dades registrant (si null, agafa les del tenant)
    String registrantName,
    String registrantEmail,
    String registrantPhone,
    String registrantNif
)
```

### `ManagedDomainResponse`
```java
record ManagedDomainResponse(
    UUID id,
    UUID tenantId,
    String tenantName,
    String domainName,
    String tld,
    String status,
    BigDecimal purchasePrice,
    BigDecimal salePrice,
    Instant registeredAt,
    Instant expiresAt,
    boolean autoRenew,
    boolean dnsConfigured,
    List<DnsRecordResponse> dnsRecords
)
```

### `DnsRecordRequest`
```java
record DnsRecordRequest(
    @NotBlank String type,   // A, CNAME, MX, TXT
    @NotBlank String name,
    @NotBlank String value,
    int ttl,
    Integer priority
)
```

---

## 6. Integració OpenProvider

### 6.1 Credencials

Emmagatzemades via `SystemConfigService` (Mòdul sysconfig):
- `OPEN_PROVIDER_USERNAME` — usuari del compte reseller
- `OPEN_PROVIDER_PASSWORD` — contrasenya (xifrada AES-256)

### 6.2 Operacions principals

```java
public interface DomainRegistrarClient {
    DomainAvailability checkAvailability(String domainName);
    String registerDomain(RegisterDomainCommand cmd);    // retorna provider ID
    void renewDomain(String providerDomainId, int years);
    void setDnsRecords(String providerDomainId, List<DnsRecord> records);
    DomainInfo getDomainInfo(String providerDomainId);
    void initiateTransferIn(String domainName, String authCode);
    void cancelDomain(String providerDomainId);
}
```

### 6.3 Implementacions

- `OpenProviderDomainClient` — implementació real via API REST OpenProvider v1
- `MockDomainRegistrarClient` — mock per a tests i entorn dev

### 6.4 Configuració automàtica DNS per a landings

Quan es registra un domini per a un tenant que té una landing publicada:

```
A   @    → IP del servidor (65.108.148.62)
A   www  → IP del servidor
CNAME api → api.amgdigital.cat  (per a futurs subdominis)
```

---

## 7. Lògica de negoci

### 7.1 Flux de registre

```
1. ADMIN sol·licita registre → DomainCheckRequest
2. Sistema verifica disponibilitat a OpenProvider
3. Si disponible → crea ManagedDomain (PENDING_PURCHASE)
4. Crea Budget al Mòdul 07 (línea de domini + renovació anual)
5. Client accepta pressupost → REGISTERING
6. Sistema registra a OpenProvider → ACTIVE
7. Si té landing associada → configura DNS automàticament → DNS_CONFIGURED
8. Traefik detecta el nou domini i genera certificat SSL
```

### 7.2 Renovació automàtica

- Job diari a les 09:00 Europe/Madrid
- Comprova dominis que expiren en ≤ 30 dies
- Notifica via Telegram al SUPER_ADMIN
- Si `auto_renew = true` i el client té mètode de pagament actiu → renova automàticament
- Si no → crea pressupost de renovació i notifica al client

### 7.3 Preus per TLD (valors inicials)

| TLD | Cost registre | Cost renovació | Venda registre | Venda renovació |
|-----|--------------|----------------|----------------|-----------------|
| `.cat` | 11€ | 11€ | 20€ | 18€ |
| `.es` | 4€ | 4€ | 12€ | 10€ |
| `.com` | 8€ | 8€ | 15€ | 13€ |
| `.eu` | 5€ | 5€ | 12€ | 10€ |
| `.net` | 9€ | 9€ | 15€ | 13€ |

---

## 8. Frontend

### 8.1 Panell admin — `/portal/admin/domains`

- Taula de tots els dominis amb: domini, tenant, estat (badge), expiració, DNS ✓/✗, preu
- Filtre per estat, tenant, TLD
- Botó "Registrar domini" → modal amb cerca de disponibilitat
- Botó "Renovar" per a dominis `EXPIRING_SOON`
- Badge vermell per a dominis que expiren en < 15 dies

### 8.2 Modal de registre

```
1. Camp de cerca → "perruqueria-maria" + selector TLD (.cat / .es / .com)
2. Verificació disponibilitat en temps real (debounce 500ms)
3. Si disponible → mostra preu + preu renovació anual
4. Si no disponible → suggeriments alternatius
5. Formulari: tenant (selector), landing associada (opcional), auto-renovació
6. Confirmació → crea domini + genera pressupost
```

### 8.3 Detall domini — `/portal/admin/domains/{id}`

- Informació del domini, registrant, estat, dates
- Gestió de registres DNS (taula editable)
- Historial d'accions (registre, renovacions, canvis DNS)
- Botó "Configurar DNS automàtic" si té landing associada

### 8.4 Vista client — `/portal/my-domain`

- Estat del domini i data d'expiració
- Registres DNS (només lectura)
- Botó "Sol·licitar transferència"

---

## 9. Casos de test QA

| ID | Cas | Resultat esperat |
|----|-----|-----------------|
| DOM-01 | Comprovar disponibilitat domini lliure | `available: true`, preu correcte |
| DOM-02 | Comprovar disponibilitat domini ocupat | `available: false`, alternatives |
| DOM-03 | Registrar domini (mock) | Estat ACTIVE, DNS configurat |
| DOM-04 | DNS automàtic per landing | Registres A creats correctament |
| DOM-05 | Job renovació automàtica | Detecta dominis a ≤30 dies, crea pressupost |
| DOM-06 | Alerta expiració | Notificació Telegram enviada |
| DOM-07 | Preus TLD configurables | SUPER_ADMIN pot modificar marge |
| DOM-08 | CLIENT no pot registrar dominis | 403 Forbidden |
| DOM-09 | Domini no disponible no es registra | Error amb alternatives |
| DOM-10 | Credencials OpenProvider manquen | 503 MISSING_API_KEY |

---

## 10. Configuració

### `application.yml`

```yaml
app:
  domains:
    provider: mock          # mock | open_provider
    auto-renew-days-before: 30
    renewal-check-cron: "0 0 9 * * *"  # cada dia a les 09:00
    server-ip: ${SERVER_IP:65.108.148.62}
```

### System Config Keys

| Clau | Descripció |
|------|-----------|
| `OPEN_PROVIDER_USERNAME` | Usuari compte reseller OpenProvider |
| `OPEN_PROVIDER_PASSWORD` | Contrasenya (xifrada) |

---

## 11. Notes d'implementació

- Seguir el patró dels altres mòduls: `DomainRegistrarClient` interface + `OpenProviderDomainClient` real + `MockDomainRegistrarClient`
- El provider s'activa via `app.domains.provider` (igual que `PAYMENTS_PROVIDER=mock`)
- Les credencials van al `SystemConfigService`, mai a variables d'entorn en clar
- Els registres DNS s'apliquen tant a OpenProvider (DNS autoritatiu) com a Traefik (routing intern)
- La integració amb Billing és obligatòria: cap domini es registra sense pressupost acceptat
