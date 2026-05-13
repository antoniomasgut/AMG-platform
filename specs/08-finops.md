# Mòdul 08: FinOps — Facturació amb Holded + Verifactu

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 07 (Billing)

---

## 1. Objectius

- Crear **factures a Holded** automàticament quan un pressupost és acceptat (Mòdul 07 Billing)
- Enviar factures al sistema **Verifactu** de l'AEAT (obligatori per a autònoms/empreses a Espanya)
- Sincronitzar estats de cobrament entre Holded i la plataforma
- Gestionar **clients (contactes)** a Holded automàticament
- La integració serà **mockejable** per desenvolupament sense pla Holded

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **HoldedClient** (Interface + Mock + Real): Abstracció per cridar l'API REST de Holded
- **Sincronització de contactes**: Quan es crea un tenant/client a la plataforma, es crea automàticament a Holded
- **Creació de factures**: Quan un pressupost (Budget) passa a `ACCEPTED`, es genera la factura a Holded
- **Enviament a Verifactu**: Holded ja envia automàticament a Verifactu (tots els plans ho inclouen)
- **Estat de cobrament**: Consultar estat de factures a Holded (pagada, pendent, vençuda)
- **Dashboard FinOps**: Resum de facturació mensual, impagats, ingressos pendents
- **Webhook Holded**: Rebre notificacions de cobraments i canvis d'estat
- **Catàleg de productes**: Sincronitzar serveis del catàleg (`CatalogService`) com a productes a Holded

### 2.2 Funcionalitats excloses

- Gestió de nòmines / RRHH (Holded ho té però no ho integrem)
- Inventari (Holded ho té com a gemma apart, +25 €/mes)
- Facturació recurrent automàtica (es farà manualment per ara)
- Conciliació bancària automàtica (Holded ho fa, però no ho exposem)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Configuració de Holded (API Key, sync) | Accés complet a FinOps |
| ADMIN | Visualització de factures i dashboard | Veure factures del seu tenant, NO configurar Holded |
| CLIENT | Veure les seves factures al portal | Veure factures pròpies, descarregar PDF |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### HoldedConfig

Configuració de la integració amb Holded per tenant.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false, unique=true) | FK a Tenant |
| apiKeyRef | String(100) | @Column(nullable=false) | Referència a Vault (ID de la credencial) |
| holdedCompanyId | String(50) | @Column | Identificador de companyia a Holded |
| holdedContactId | String(50) | @Column | Contacte a Holded (sync automàtic) |
| isSynced | Boolean | @Column(nullable=false) | Si el contacte està sincronitzat |
| lastSyncAt | Instant | @Column | Última sincronització |
| isActive | Boolean | @Column(nullable=false) | Si està activa la integració |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

#### Invoice

Factura creada a Holded des de la plataforma.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | FK a Tenant |
| budgetId | UUID | @Column(nullable=false, unique=true) | FK a Budget (pressupost origen) |
| holdedInvoiceId | String(50) | @Column(unique=true) | ID de la factura a Holded |
| invoiceNumber | String(20) | @Column | Número de factura (ex: F-2026-001) |
| status | InvoiceStatus | @Enumerated(STRING) | PENDING, SENT, PAID, OVERDUE, CANCELLED |
| amount | BigDecimal | @Column(nullable=false) | Import total |
| taxAmount | BigDecimal | @Column | Import IVA |
| currency | String(3) | @Column(length=3) | EUR per defecte |
| verifactuStatus | VerifactuStatus | @Enumerated(STRING) | SENT (Holded ho gestiona) |
| invoicePdfUrl | String(500) | @Column | URL del PDF a Holded |
| dueDate | Instant | @Column | Data de venciment |
| paidAt | Instant | @Column | Data de cobrament |
| errorMessage | String(500) | @Column | Si falla la creació |
| isActive | Boolean | @Column(nullable=false) | |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

#### Expense (Despeses eventuals)

Per gestionar despeses associades a un tenant (ex: compra de domini).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | FK a Tenant |
| description | String(255) | @Column(nullable=false) | Concepte de la despesa |
| amount | BigDecimal | @Column(nullable=false) | Import |
| category | String(50) | @Column | Domini, WhatsApp, etc. |
| holdedExpenseId | String(50) | @Column | ID a Holded (si es sincronitza) |
| createdAt | Instant | @CreatedDate | |

### 3.2 Enums

```java
public enum InvoiceStatus {
    PENDING,    // Creada pero no enviada a Holded
    SENT,       // Enviada a Holded / Verifactu
    PAID,       // Cobrada
    OVERDUE,    // Vençuda i no pagada
    CANCELLED   // Anul·lada
}

public enum VerifactuStatus {
    NOT_REQUIRED,  // No aplica (factura sense obligació Verifactu)
    SENT,          // Enviada a Verifactu via Holded
    FAILED         // Error en l'enviament
}
```

---

## 4. Endpoints API

Tots els endpoints porten prefix `/api/v1/finops`. Tots requereixen JWT + autenticació.

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| POST | /api/v1/finops/configure | SUPER_ADMIN | Configurar Holded per un tenant (API Key + company ID) |
| GET | /api/v1/finops/configure/{tenantId} | SUPER_ADMIN, ADMIN | Veure configuració Holded del tenant |
| POST | /api/v1/finops/configure/{tenantId}/sync | SUPER_ADMIN | Forçar sincronització contacte → Holded |
| GET | /api/v1/finops/invoices | SUPER_ADMIN, ADMIN | Llistar factures (paginat, filtre per tenant/estat/data) |
| GET | /api/v1/finops/invoices/{invoiceId} | SUPER_ADMIN, ADMIN, CLIENT | Detall de factura (CLIENT només les seves) |
| GET | /api/v1/finops/invoices/{invoiceId}/pdf | SUPER_ADMIN, ADMIN, CLIENT | Descarregar PDF de factura (redirect a Holded) |
| GET | /api/v1/finops/dashboard | SUPER_ADMIN, ADMIN | Dashboard FinOps del tenant |
| GET | /api/v1/finops/dashboard/global | SUPER_ADMIN | Dashboard global de tots els tenants |
| POST | /api/v1/finops/webhook | CAP DE LES DUES (públic amb API Key pròpia) | Webhook de Holded per notificacions de cobrament |

### Integració automàtica (no endpoints)

Quan un **Budget** passa a `ACCEPTED` (Mòdul 07):
1. Es crea `Invoice` a la BD (status PENDING)
2. Es crida HoldedClient.createInvoice() → Holded crea la factura
3. Es guarda holdedInvoiceId i invoiceNumber
4. Holded envia automàticament a Verifactu
5. L'Invoice passa a SENT

---

## 5. Serveis

### 5.1 FinOpsClient (Interface)

```java
public interface FinOpsClient {
    // Contactes
    String createContact(String tenantName, String email, String phone, String nif);
    void updateContact(String holdedContactId, String tenantName, String email, String phone);
    boolean contactExists(String holdedContactId);
    
    // Factures
    String createInvoice(String holdedContactId, BigDecimal amount, BigDecimal taxAmount,
                         String description, String dueDate);
    InvoiceStatusDto getInvoiceStatus(String holdedInvoiceId);
    String getInvoicePdfUrl(String holdedInvoiceId);
    void cancelInvoice(String holdedInvoiceId);
    
    // Dashboard
    DashboardData getDashboard(String holdedContactId);
    
    // Health
    boolean isConnected();
}
```

### 5.2 Implementacions

- **FinOpsMockClient**: Retorna dades falses per desenvolupament sense pla Holded
- **FinOpsHoldedClient**: Crida l'API REST de Holded real (api.holded.com/v1)

### 5.3 FinOpsService

```java
public interface FinOpsService {
    // Configuració
    void configure(HoldedConfigRequest request);
    HoldedConfig getConfig(UUID tenantId);
    void syncContact(UUID tenantId);
    
    // Factures
    Page<InvoiceResponse> listInvoices(UUID tenantId, String status, int page, int size);
    InvoiceResponse getInvoice(UUID invoiceId, UUID currentTenantId);
    String getInvoicePdfUrl(UUID invoiceId, UUID currentTenantId);
    void cancelInvoice(UUID invoiceId);
    
    // Automàtic (cridat des de Billing)
    InvoiceResponse createInvoiceFromBudget(UUID budgetId);
    
    // Dashboard
    FinOpsDashboardResponse getDashboard(UUID tenantId);
    FinOpsDashboardGlobalResponse getGlobalDashboard();
    
    // Webhook
    WebhookResponse processWebhook(WebhookRequest request);
}
```

### 5.4 FinOpsOrchestrator

Implementació `@Service` principal que:
- Injecció de `FinOpsClient` (mock o real segons perfil)
- Injecció de `BudgetRepository` (Mòdul 07) per crear factures des de pressupostos
- Injecció de `TenantRepository` per sincronitzar contactes
- Lògica de creació de factures: Budget → Invoice → Holded

---

## 6. Integració amb Holded

### 6.1 API Holded

Base URL: `https://api.holded.com/api/v1`

Autenticació: API Key via header `X-API-KEY: {apiKey}`

Endpoints Holded que utilitzarem:

| Mètode | Ruta Holded | Propòsit |
|--------|------------|----------|
| POST | /contacts | Crear contacte (client) |
| GET | /contacts/{id} | Obtenir dades de contacte |
| PUT | /contacts/{id} | Actualitzar contacte |
| POST | /invoices | Crear factura |
| GET | /invoices/{id} | Estat de factura |
| GET | /invoices/{id}/pdf | Descarregar PDF |
| DELETE | /invoices/{id} | Anul·lar factura |
| GET | /dashboard | Dashboard financer |

### 6.2 Flux de creació de factura

```
Budget ACCEPTED (Mòdul 07)
    → FinOpsService.createInvoiceFromBudget(budgetId)
        → Buscar Budget + BudgetLines
        → Calcular amount, taxAmount
        → Si no existeix contacte a Holded → crear-lo
        → FinOpsClient.createInvoice(contactId, amount, taxes, lines, dueDate)
        → Guardar Invoice amb holdedInvoiceId, invoiceNumber
        → Retornar InvoiceResponse
    → Budget.status = INVOICED (nou estat o simplement ja ACCEPTED)
```

### 6.3 Verifactu

No cal fer res explícitament — **Holded ja envia automàticament a Verifactu** totes les factures creades (tots els plans ho inclouen). Nosaltres només emmagatzemem l'estat retornat per Holded.

---

## 7. Seguretat

- La **API Key de Holded** s'emmagatzema xifrada al Vault (Mòdul 02) com a credencial de tipus `API_KEY` amb referència des de `HoldedConfig.apiKeyRef`
- El webhook de Holded porta una API Key pròpia configurable (no JWT)
- CLIENT només pot veure les seves pròpies factures
- CLIENT NO pot veure ni modificar la configuració de Holded

---

## 8. Tests d'integració

15 tests mínims (patró: `FinOpsControllerTest.java`):

1. **Configurar Holded** → 201 (SUPER_ADMIN)
2. **Veure configuració** → 200 (SUPER_ADMIN, ADMIN)
3. **CLIENT no pot veure configuració** → 403
4. **Sincronitzar contacte** → 200 (SUPER_ADMIN)
5. **Llistar factures buit** → 200
6. **Crear factura des de pressupost** → 201
7. **Veure detall factura** → 200
8. **CLIENT veu la seva factura** → 200
9. **CLIENT no veu factura d'altre tenant** → 403
10. **Anul·lar factura** → 200
11. **Dashboard** → 200 (ADMIN)
12. **Dashboard global** → 200 (SUPER_ADMIN, ADMIN)
13. **Sense JWT** → 401
14. **CLIENT no pot configurar** → 403
15. **Webhook Holded** → 200 (amb API Key, sense JWT)

---

## 9. QA / Casos de prova

| # | Escenari | Esperat |
|---|----------|---------|
| FIN-01 | Pressupost acceptat → factura creada a Holded | Invoice.status = SENT, holdedInvoiceId no null |
| FIN-02 | Factura creada → Verifactu automàtic | verifactuStatus = SENT |
| FIN-03 | Client nou → contacte sincronitzat a Holded | holdedContactId no null |
| FIN-04 | Webhook de cobrament → Invoice.status = PAID | Canvi d'estat a PAID, paidAt no null |
| FIN-05 | Factura vençuda → status OVERDUE | Dashboard mostra impagats |
| FIN-06 | Anul·lar pressupost → anul·lar factura a Holded | Invoice.status = CANCELLED |
| FIN-07 | Sense API Key configurada → error clar | 400 "Holded not configured" |
| FIN-08 | Mock activat → tot funciona sense Holded real | MockClient retorna dades falses |

---

## 10. Configuració

### application.yml

```yaml
app:
  finops:
    provider: mock   # mock | holded
    holded:
      api-url: https://api.holded.com/api/v1
      webhook-secret: ${HOLDED_WEBHOOK_SECRET}
    default-due-days: 30    # Venciment per defecte
```

### Perfils

- **`dev`**: `app.finops.provider=mock` (no cal pla Holded)
- **`prod`**: `app.finops.provider=holded` (requereix pla Holded i API Key)

---

## 11. Dependències Maven (noves)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

Necessitem WebClient per cridar l'API REST de Holded (la implementació real).

---

## 12. Resum d'entitats

| Entitat | Repositori | DTOs | Endpoints |
|---------|-----------|------|-----------|
| HoldedConfig | HoldedConfigRepository | HoldedConfigRequest, HoldedConfigResponse | configure, get, sync |
| Invoice | InvoiceRepository | InvoiceResponse, InvoiceListResponse | llistar, detall, pdf, cancel·lar |
| Expense | ExpenseRepository | ExpenseRequest, ExpenseResponse | (futur) |
| FinOpsClient | — | — | interface + 2 implementacions |
