# Spec 52 — Social Publisher (publicació multi-xarxa via Telegram + IA)

> **Versió:** 1.0
> **Data:** 2026-06-30
> **Estat:** Proposat
> **Depèn de:** Spec 02 (Vault), Spec 20 (Agents Telegram), Spec 40 (Google OAuth), Spec 41 (Agent Tool Calling), Spec 42 (Meta OAuth)

---

## 1. Objectiu

Permetre que cada tenant publiqui contingut a les seves xarxes socials des del mòbil, enviant un missatge al bot de Telegram. La IA genera el contingut adaptat a cada plataforma i demana confirmació abans de publicar.

**Principi clau:** el tenant no ha de saber res d'APIs. Escriu en català/castellà, adjunta o no una foto, i el sistema s'encarrega de la resta.

---

## 2. Xarxes suportades

### MVP (fase 1)

| Xarxa | Tipus de contingut | API | OAuth |
|-------|-------------------|-----|-------|
| **Instagram** | Foto, carrusel (≤10), Reel, Story | Meta Graph API v19+ | Meta (Spec 42, nou scope) |
| **Facebook** | Text, foto, vídeo, event, oferta, link | Meta Graph API v19+ | Meta (Spec 42, nou scope) |
| **Google Business** | Novetat, oferta, event, producte | Google My Business API v4.9 | Google (Spec 40, nou scope) |

### Fase 2

| Xarxa | Tipus de contingut | API | OAuth |
|-------|-------------------|-----|-------|
| **LinkedIn** | Text, foto, vídeo, article | LinkedIn API v2 | LinkedIn OAuth 2.0 (nou) |
| **TikTok** | Vídeo curt | TikTok Content Posting API | TikTok OAuth (nou) |

---

## 3. Tipus de publicació per xarxa

### 3.1 Instagram
| Tipus | Descripció | Requisits |
|-------|-----------|-----------|
| `FEED_PHOTO` | Foto única al feed | 1 imatge (JPEG/PNG, ratio 1:1 / 4:5 / 1.91:1) |
| `FEED_CAROUSEL` | Fins a 10 fotos/vídeos | 2–10 peces |
| `REEL` | Vídeo curt 15s–90s | MP4, ratio 9:16 recomanat |
| `STORY` | Foto o vídeo 24h | Imatge o vídeo ≤60s |

### 3.2 Facebook
| Tipus | Descripció |
|-------|-----------|
| `POST_TEXT` | Text pla (fins a 63.206 caràcters) |
| `POST_PHOTO` | Foto + text |
| `POST_VIDEO` | Vídeo + text |
| `POST_LINK` | URL + descripció (link preview automàtic) |
| `EVENT` | Nom, data, lloc, descripció |
| `OFFER` | Oferta amb % de descompte o preu rebaixat |

### 3.3 Google Business Profile
| Tipus | Descripció |
|-------|-----------|
| `WHATS_NEW` | Novetat general del negoci |
| `OFFER` | Oferta amb codi opcional, data inici/fi |
| `EVENT` | Event amb títol, data i hora |
| `PRODUCT` | Producte amb nom, preu, descripció |

---

## 4. Flux via Telegram

### 4.1 Iniciació (natural language o comandament)

El sistema detecta intenció de publicació si el missatge conté paraules clau:
`publica`, `post`, `instagram`, `facebook`, `xarxes`, `penja`.

També disponible com a comandament explícit: `/publica`.

### 4.2 Flux complet (conversació multi-pas)

```
Tenant: "vull publicar una foto de les ofertes de tardor"
                    ↓
Bot: "A quines xarxes? 📱"
     [Instagram] [Facebook] [Google Business] [Totes]
                    ↓
Tenant: [Instagram] [Facebook]
                    ↓
Bot: "Quin tipus de publicació?"
     Instagram: [Foto] [Carrusel] [Story] [Reel]
     Facebook:  [Text+foto] [Oferta] [Event]
                    ↓
Tenant: [Foto] [Text+foto]
                    ↓
Bot: "Envia'm la foto (o escriu 'sense foto' per text sol)"
                    ↓
Tenant: [envia foto]
                    ↓
Bot: "Vols que la IA generi el text o l'escrius tu?"
     [Genera text] [Escric jo]
                    ↓
Tenant: [Genera text]
                    ↓
Bot: "✍️ Generant contingut..."
     ─────────────────────────
     📸 INSTAGRAM:
     «Les ofertes de tardor ja són aquí 🍂
     Aprofita els nostres preus especials fins al 31 d'octubre.
     Vine a veure'ns o reserva cita per WhatsApp 👇
     #ofertes #tardor #[sector] #[ciutat]»

     📘 FACEBOOK:
     «Bon dia! Ja tenim les ofertes de tardor disponibles.
     [llista serveis/productes si n'hi ha a la KB]
     Contacta'ns per WhatsApp o vine directament. T'esperem! 🍂»
     ─────────────────────────
     [✅ Publicar ara] [🕐 Programar] [✏️ Editar text] [🔄 Regenerar]
                    ↓
Tenant: [✅ Publicar ara]
                    ↓
Bot: "✅ Publicat!
     • Instagram: https://www.instagram.com/p/XXXXX
     • Facebook: https://www.facebook.com/[page]/posts/XXXXX"
```

### 4.3 Programació de publicació

Si el tenant tria "Programar":
```
Bot: "Quan vols publicar-ho?"
     [Avui a les 18:00] [Demà a les 09:00] [Escull data/hora]
```
La publicació es desa a `social_posts` amb `status=SCHEDULED` i un `scheduled_at`.
Un `@Scheduled` de Spring comprova cada minut les publicacions pendents.

---

## 5. Generació de contingut (IA)

`SocialContentGeneratorService` usa Claude per generar:

**Inputs:**
- Sector del tenant + context del negoci (des de `sector-contexts`)
- Tipus de publicació per xarxa
- Contingut base (missatge del tenant)
- Imatge adjunta (si n'hi ha — descripció via vision)
- Historial de publicacions recents (evitar repeticions)

**Adaptació per xarxa:**
| Xarxa | Estil | Hashtags | CTA |
|-------|-------|----------|-----|
| Instagram | Breu, visual, emojis | 5–15 rellevants | "Reserva per DM o link a la bio" |
| Facebook | Més informatiu, conversacional | 1–3 màxim | "Escriu-nos per WhatsApp" |
| Google Business | Concís, CTA directe, sense emojis | Cap | "Truca o reserva online" |

**Idioma:** detectat de la configuració del tenant (`preferredLocale`); si no, català per defecte.

---

## 6. Arquitectura backend

### 6.1 Entitats

```java
@Entity @Table(name = "social_posts")
public class SocialPost {
    UUID id;
    UUID tenantId;
    String network;          // INSTAGRAM, FACEBOOK, GOOGLE_BUSINESS, LINKEDIN
    String postType;         // FEED_PHOTO, REEL, POST_TEXT, OFFER, EVENT...
    String caption;          // text generat/editat
    String mediaUrl;         // URL a MinIO (si té imatge/vídeo)
    String externalPostId;   // ID retornat per la xarxa social
    String externalPostUrl;  // URL pública del post
    String status;           // DRAFT, SCHEDULED, PUBLISHED, FAILED
    Instant scheduledAt;
    Instant publishedAt;
    String errorMessage;
    Instant createdAt;
}
```

### 6.2 Serveis

| Servei | Responsabilitat |
|--------|----------------|
| `SocialPublisherOrchestrator` | Punt d'entrada: rep intent de Telegram, coordina flux |
| `SocialContentGeneratorService` | Genera captions per xarxa via Claude |
| `InstagramPublisherService` | Publica via Meta Graph API |
| `FacebookPublisherService` | Publica via Meta Graph API |
| `GoogleBusinessPublisherService` | Publica via Google My Business API |
| `LinkedInPublisherService` | (Fase 2) Publica via LinkedIn API |
| `SocialSchedulerJob` | `@Scheduled` cada minut — llança publicacions programades |

### 6.3 Extensions de Telegram (Spec 20)

`TelegramWebhookController` detecta intencions de publicació i delega a `SocialPublisherOrchestrator`. El flux multi-pas emmagatzema l'estat de la conversa a Redis (clau `social:draft:{chatId}`, TTL 30 min).

### 6.4 Taula de migració (Flyway V71)

```sql
CREATE TABLE social_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    network VARCHAR(30) NOT NULL,
    post_type VARCHAR(30) NOT NULL,
    caption TEXT,
    media_url TEXT,
    external_post_id VARCHAR(255),
    external_post_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_social_posts_tenant ON social_posts(tenant_id);
CREATE INDEX idx_social_posts_scheduled ON social_posts(status, scheduled_at)
    WHERE status = 'SCHEDULED';
```

---

## 7. Credencials (Vault — Spec 02)

### 7.1 Instagram + Facebook (Meta)

Reutilitza el flux OAuth de Spec 42, però amb scopes addicionals:
- `pages_manage_posts` — publicar a Facebook Pages
- `instagram_basic` + `instagram_content_publish` — publicar a Instagram

Dades a guardar (a `whatsapp_waba_configs` o nova taula `social_meta_configs`):
- `page_access_token` (long-lived, 60 dies — cal renovar)
- `facebook_page_id`
- `instagram_account_id` (Business Account vinculada a la Page)

### 7.2 Google Business Profile

Reutilitza el Google OAuth de Spec 40 amb scope addicional:
- `https://www.googleapis.com/auth/business.manage`

Dades addicionals:
- `google_business_location_id` (obtenible via `accounts.locations.list`)

Es desa a `google_module_configs` (nova columna `business_location_id`).

### 7.3 LinkedIn (Fase 2)

Nou flux OAuth:
- `w_member_social` + `r_organization_social` (per a pàgines d'empresa)
- Nova taula `linkedin_configs (tenant_id, organization_id, encrypted_access_token, token_expires_at)`

---

## 8. Endpoints API

| Mètode | Path | Rol | Descripció |
|--------|------|-----|-----------|
| `GET` | `/api/v1/social/tenants/{id}/status` | ADMIN+ | Xarxes connectades i estat |
| `POST` | `/api/v1/social/tenants/{id}/meta/connect` | ADMIN+ | Inicia OAuth Meta per social |
| `POST` | `/api/v1/social/tenants/{id}/google-business/connect` | ADMIN+ | Afegeix scope GBP a Google OAuth |
| `GET` | `/api/v1/social/tenants/{id}/posts` | ADMIN+ | Historial de publicacions |
| `POST` | `/api/v1/social/tenants/{id}/posts` | ADMIN+ | Publicació manual (des del portal) |
| `DELETE` | `/api/v1/social/tenants/{id}/posts/{postId}` | ADMIN+ | Cancel·la publicació programada |

---

## 9. Frontend (portal)

### 9.1 Pàgina de connexió: `/portal/admin/tenants/[id]/social`

- Estat de cada xarxa (connectada / desconnectada)
- Botó "Connectar" per a cada xarxa → OAuth flow
- Número de compte connectat (pàgina FB, compte IG, location GBP)

### 9.2 Historial de publicacions: `/portal/admin/tenants/[id]/social/posts`

Taula amb: xarxa, tipus, preview caption, data, estat (publicat/programat/error), link extern.

---

## 10. Seguretat i límits

- **Rate limiting** per xarxa:
  - Instagram: màx 25 posts API/dia per compte
  - Facebook: màx 200 posts/dia per Page
  - Google Business: sense límit documentat, però màx 1 post/hora recomanat
- **Renovació de tokens**: job diari que comprova tokens a expirar en <7 dies i notifica el tenant via Telegram
- **Imatges**: es pugen primer a MinIO, s'envien la URL a l'API de la xarxa (o en base64 si l'API ho requereix)
- **Vídeos (Reels/TikTok)**: flux de "resumable upload" — pot trigar minuts; el bot informa "Pujant vídeo..."

---

## 11. QA

| Cas | Resultat esperat |
|-----|-----------------|
| Tenant sense xarxes connectades envia `/publica` | Bot respon "Primer has de connectar almenys una xarxa a /portal" |
| Publicació amb foto a Instagram + Facebook | Post apareix a les dues plataformes en <30s |
| Publicació programada per a les 09:00 | El job la llança entre 09:00 i 09:01 |
| Token Meta expirat | Bot notifica "El token de Facebook ha caducat, reconnecta a /portal" |
| IA genera caption | S'adapta a l'estil de cada xarxa (hashtags IG, sense hashtags GBP) |
| Cancel·lació de publicació programada | `status=CANCELLED`, no es publica |
| Reel > 90s | Bot respon "El vídeo supera els 90 segons. Retalla'l i torna-l'hi a enviar." |

---

## 12. Ressenyes de Google Business a les landings (Spec 04 + 05)

La connexió amb Google Business Profile (§7.2) habilita automàticament la sincronització de ressenyes per al bloc `reviews` de les landings (PRO). Integrat a les Spec 04 (Engine) i Spec 05 (Factory) — no és un mòdul separat.

### 12.1 Model de dades

```sql
-- Flyway V72
CREATE TABLE google_business_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    review_id VARCHAR(255) NOT NULL,         -- ID de Google
    author_name VARCHAR(255),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    reply TEXT,                              -- resposta del propietari (si n'hi ha)
    review_time TIMESTAMPTZ,
    synced_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (tenant_id, review_id)
);
CREATE INDEX idx_gbr_tenant ON google_business_reviews(tenant_id, rating DESC, review_time DESC);
```

### 12.2 Sincronització

`GoogleBusinessReviewSyncJob` (`@Scheduled` cada 24h):
1. Per cada tenant amb `google_business_location_id` configurat
2. Crida `accounts.locations.reviews.list` (Google My Business API v4.9)
3. Upsert a `google_business_reviews` per `(tenant_id, review_id)`
4. Endpoint manual: `POST /api/v1/social/tenants/{id}/google-business/reviews/sync`

### 12.3 Canvi al bloc `reviews` (Spec 04 + 05)

El bloc `reviews` afegeix el camp `source` a les seves props:

```typescript
interface ReviewsBlockProps {
  title: string;
  source: 'manual' | 'google_business';  // NOU — default: 'manual'
  minRating: number;                      // NOU — filtre mínim (default: 4)
  maxItems: number;                       // NOU — màxim a mostrar (default: 6)
  googleMapsUrl: string;
  items: ReviewItem[];                    // només usat si source='manual'
}
```

**Render (Engine — Spec 04):**
- `source = 'manual'` → renderitza `props.items` (comportament actual)
- `source = 'google_business'` → llegeix de `google_business_reviews` (server-side, SSR) filtrant per `minRating` i ordenant per `rating DESC, review_time DESC`, limitat a `maxItems`

**Editor (Factory — Spec 05 `BlockProperties`):**
- Toggle "Font: Manual / Google Business"
- Si Google Business: mostra estat de sincronització + botó "Sincronitzar ara" (crida l'endpoint manual)
- Si `source = 'google_business'` però el tenant no té GBP connectat: avís "Connecta Google Business Profile a Configuració → Social"

### 12.4 SEO

Les ressenyes renderitzades server-side inclouen `schema.org/Review` JSON-LD per a rich snippets a Google:

```json
{
  "@type": "Review",
  "author": { "@type": "Person", "name": "Maria G." },
  "reviewRating": { "@type": "Rating", "ratingValue": "5" },
  "reviewBody": "Excel·lent servei..."
}
```

---

## 13. Ordre d'implementació

1. **Taula `social_posts`** (Flyway V71)
2. **Connexió Meta per social** (nous scopes a Spec 42 OAuth flow)
3. **`InstagramPublisherService` + `FacebookPublisherService`** (FEED_PHOTO primer)
4. **`SocialContentGeneratorService`** (generació IA de captions)
5. **Integració Telegram** (`SocialPublisherOrchestrator` + estat Redis)
6. **`SocialSchedulerJob`** (publicacions programades)
7. **Connexió Google Business** (nou scope a Spec 40 OAuth + `google_business_location_id`)
8. **`GoogleBusinessPublisherService`** (WHATS_NEW + OFFER)
9. **Taula `google_business_reviews`** (Flyway V72) + `GoogleBusinessReviewSyncJob`
10. **Bloc `reviews` amb `source: google_business`** (Engine + Factory + JSON-LD)
11. **Frontend** (pàgina connexió + historial publicacions)
12. **LinkedIn** (Fase 2)
