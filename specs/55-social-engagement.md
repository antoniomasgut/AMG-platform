# Spec 55 · Social Engagement & Analytics (extensió del Mòdul 52)

**Estat:** ✅ Completat (backend + toggles frontend). Pendent config Meta a producció (subscripció `feed`, revisió permisos App per Insights).
**Depèn de:** Mòdul 52 (Social Publisher), Mòdul 54 (Google Reviews), Mòdul 42 (Meta OAuth), Mòdul 20 (Agents Telegram)

---

## 1. Objectiu

Ampliar el Social Publisher (només sortida: publicar) amb **entrada, mesura i proactivitat**, tot **activable/desactivable per tenant**:

1. **Comentaris → Telegram + resposta** — avís quan algú comenta un post d'IG/FB + respondre des de Telegram.
2. **Analytics + resum setmanal** — mètriques (reach, likes, comentaris) via Meta Insights + digest setmanal per Telegram.
3. **Suggeriments IA proactius** — idea de contingut setmanal per sector/temporada amb botó per crear el post.
4. **Ressenya 5★ → post automàtic** — convertir una ressenya excel·lent en post social (amb aprovació del tenant).

---

## 2. Toggles per tenant (activar/desactivar)

Els 4 comportaments es desen com a flags JSON dins la config `SOCIAL_PUBLISHER` de `nexe_service_configs` (mateix patró que ja fa de gate d'activació). **Tots opt-in (default false).**

```json
{
  "comments_to_telegram": false,
  "weekly_analytics": false,
  "ai_suggestions": false,
  "auto_post_reviews": false
}
```

- Servei: `SocialFeatureService.get(tenantId)` / `update(tenantId, flags)` — merge no destructiu de la resta de claus.
- Endpoint: `GET/PUT /api/v1/social/tenants/{tenantId}/features` (SUPER_ADMIN/ADMIN + CLIENT propi).
- Frontend: secció de toggles a la pàgina social del tenant.
- La publicació manual (flux `/publica`) segueix gated només per **presència** de la config SOCIAL_PUBLISHER, sense canvis.

---

## 3. Feature 1 · Comentaris → Telegram + resposta

- Subscripció Meta Graph al camp `feed` (FB) / `comments` (IG) al webhook existent (patró `MetaLeadWebhookController` amb verificació HMAC).
- Comentari nou → si `comments_to_telegram` → Telegram al tenant amb autor + text + botó **✍️ Respondre**.
- Estat pendent Redis (patró `GoogleReviewReplyService`) → el següent missatge es publica com a resposta via Graph API `POST /{comment-id}/replies`.

## 4. Feature 2 · Analytics + resum setmanal

- `SocialPost` amb camps de mètriques (`reach`, `likes`, `comments`, `metricsSyncedAt`).
- Scheduler diari: per posts PUBLISHED recents, fetch Meta Insights (`/{ig-media-id}/insights`, `/{post-id}?fields=likes.summary,comments.summary`).
- Scheduler setmanal (dilluns): si `weekly_analytics` → digest Telegram amb el millor post i totals.

## 5. Feature 3 · Suggeriments IA proactius

- `SocialSuggestionScheduler` (setmanal): per tenant amb `ai_suggestions` + Telegram vinculat → `SocialContentGeneratorService` genera una idea per sector/temporada → Telegram amb botó **📢 Crear aquest post** (callback `sugg:` → `SocialPublisherOrchestrator.startFlow`).

## 6. Feature 4 · Ressenya 5★ → post

- Quan Mòdul 54 notifica una ressenya nova de 5★ i `auto_post_reviews` està actiu → botó extra **📢 Compartir a xarxes** a l'avís.
- Callback `gshare:<reviewId>` → genera caption a partir del text de la ressenya → draft de post + confirmació abans de publicar.

---

## 7. Producció (ddl-auto: validate)
```sql
ALTER TABLE social_posts ADD COLUMN reach INTEGER, ADD COLUMN likes INTEGER,
    ADD COLUMN comments INTEGER, ADD COLUMN metrics_synced_at TIMESTAMPTZ;
```
(Flyway V91.)

## 8. Ordre d'implementació
1. Toggles (base) ✅ — `SocialFeatureService` + GET/PUT `/api/v1/social/tenants/{id}/features` + toggles UI a la pàgina Social.
2. Suggeriments IA (autònom) ✅ — `SocialSuggestionScheduler.sendWeeklySuggestions` (dilluns 10h).
3. Ressenya 5★ → post ✅ — botó `gshare:` a l'avís del Mòdul 54 → `ReviewSocialShareService` (preview + `gpub:` publica).
4. Analytics + digest ✅ — `SocialAnalyticsService.syncMetrics/buildWeeklyDigest` + `sendWeeklyDigests` (dilluns 9h). V91 afegeix reach/likes/comments/metrics_synced_at.
5. Comentaris webhook ✅ — `MetaLeadWebhookController` processa camp `feed`/`comment` → `SocialCommentService` avisa Telegram amb botó `cmt:` → `SocialCommentReplyService` publica la resposta via Graph API.

### Accions operatives pendents a producció
- **Meta App**: subscriure la pàgina al camp `feed` (a més de `leadgen`) al mateix webhook `/api/v1/leads/meta-webhook`.
- **Insights**: `read_insights` requereix App Review de Meta perquè les mètriques (reach) es puguin llegir; sense això, likes/comments igualment funcionen via camps públics.
- **`SocialMetaConfig.facebookPageId`** ha d'estar ple per resoldre el tenant des del `page_id` del webhook.
