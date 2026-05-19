# 03 — Integració de Canals

## Arquitectura de canals

```
WhatsApp (Twilio) ──┐
Telegram Bot API  ──┼──→ WebhookController → MessageRouter → AgentService
Email (Resend)    ──┘
```

Tots els canals implementen la mateixa interfície:

```java
public interface MessagingChannel {
    void sendMessage(String recipientId, String text);
    void sendDocument(String recipientId, byte[] file, String filename);
    String getChannelName();
}
```

---

## WhatsApp — Twilio

### Per què Twilio

- API REST completa per a tot (crear números, configurar webhooks per API)
- SDK oficial Java
- Provisioning automàtic de nous tenants per API
- RGPD complert (servidors europeus disponibles)
- Sandbox gratuït per a desenvolupament

### Alta a Twilio

```
1. Crear compte a twilio.com
2. Verificar identitat i empresa
3. Sol·licitar accés WhatsApp Business API
4. Crear número de WhatsApp de prova (sandbox immediat)
5. Número real: procés de verificació Meta (~1-3 setmanes)
```

### Variables necessàries

```
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_WHATSAPP_FROM=+34600000000
```

### Webhook de recepció

```java
@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    @PostMapping("/{tenantId}")
    public ResponseEntity<String> receive(
            @PathVariable String tenantId,
            @RequestParam("From") String from,
            @RequestParam("Body") String body,
            @RequestParam("MessageSid") String messageSid) {

        // Validar signatura Twilio
        if (!twilioValidator.validate(request)) {
            return ResponseEntity.status(403).build();
        }

        // Processar missatge
        IncomingMessage message = IncomingMessage.builder()
            .tenantId(tenantId)
            .customerPhone(from.replace("whatsapp:", ""))
            .content(body)
            .channel(Channel.WHATSAPP)
            .build();

        agentService.handleMessage(message);

        return ResponseEntity.ok("<Response/>");
    }
}
```

### Enviament de missatge

```java
@Service("whatsapp")
public class WhatsAppChannel implements MessagingChannel {

    private final TwilioRestClient client;

    @Override
    public void sendMessage(String phone, String text) {
        Message.creator(
            new PhoneNumber("whatsapp:" + phone),
            new PhoneNumber("whatsapp:" + twilioNumber),
            text
        ).create(client);
    }

    @Override
    public void sendDocument(String phone, byte[] pdf, String filename) {
        // Pujar PDF a storage temporal i enviar URL
        String fileUrl = storageService.upload(pdf, filename);
        Message.creator(
            new PhoneNumber("whatsapp:" + phone),
            new PhoneNumber("whatsapp:" + twilioNumber),
            "Aquí tens el teu pressupost:"
        ).setMediaUrl(List.of(URI.create(fileUrl)))
         .create(client);
    }
}
```

### Provisioning automàtic per a nous tenants

```java
@Service
public class TenantProvisioningService {

    public void provisionWhatsApp(Tenant tenant) {
        // 1. Buscar número disponible
        IncomingPhoneNumberReader numbers = IncomingPhoneNumber
            .reader()
            .setSmsEnabled(true)
            .setCountry("ES");

        String phoneNumber = numbers.firstPage(client)
            .getRecords().get(0).getPhoneNumber().toString();

        // 2. Configurar webhook
        IncomingPhoneNumber.updater(phoneNumber)
            .setSmsUrl("https://api.nexelocal.cat/webhook/whatsapp/" + tenant.getId())
            .update(client);

        // 3. Guardar a BD
        tenant.setPhoneNumberId(phoneNumber);
        tenantRepository.save(tenant);
    }
}
```

### Costos WhatsApp (Twilio)

| Concepte | Cost |
|---|---|
| Número WhatsApp | ~€1/mes |
| Conversa de servei (client escriu primer, finestra 24h) | Gratuït |
| Missatge d'utilitat (recordatori, confirmació) | ~€0.009/missatge |
| Missatge de màrqueting (reactivació) | ~€0.055/missatge |

---

## Telegram — Bot API

### Per què Telegram com a segon canal

- Completament gratuït, sense límits de missatges
- Alta immediata sense aprovació de Meta
- Ideal per a clients tech-friendly
- Perfecte per a proves i pilots inicials

### Alta d'un bot de Telegram

```
1. Obrir Telegram i buscar @BotFather
2. Enviar /newbot
3. Posar nom al bot: "García Pintura Assistent"
4. Posar username: garciapintura_bot
5. BotFather retorna el TOKEN
6. Registrar webhook al servidor
```

### Registrar webhook

```bash
curl -X POST \
  "https://api.telegram.org/bot{TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://api.nexelocal.cat/webhook/telegram/{tenantId}"}'
```

### Webhook de recepció

```java
@RestController
@RequestMapping("/webhook/telegram")
public class TelegramWebhookController {

    @PostMapping("/{tenantId}")
    public ResponseEntity<Void> receive(
            @PathVariable String tenantId,
            @RequestBody TelegramUpdate update) {

        if (update.getMessage() == null) {
            return ResponseEntity.ok().build();
        }

        IncomingMessage message = IncomingMessage.builder()
            .tenantId(tenantId)
            .customerPhone(String.valueOf(update.getMessage().getFrom().getId()))
            .content(update.getMessage().getText())
            .channel(Channel.TELEGRAM)
            .chatId(String.valueOf(update.getMessage().getChat().getId()))
            .build();

        agentService.handleMessage(message);
        return ResponseEntity.ok().build();
    }
}
```

### Enviament de missatge

```java
@Service("telegram")
public class TelegramChannel implements MessagingChannel {

    private static final String API_URL = "https://api.telegram.org/bot";

    @Override
    public void sendMessage(String chatId, String text) {
        restTemplate.postForObject(
            API_URL + botToken + "/sendMessage",
            Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "Markdown"
            ),
            String.class
        );
    }

    @Override
    public void sendDocument(String chatId, byte[] pdf, String filename) {
        // Enviar PDF directament per Telegram
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("document", new ByteArrayResource(pdf) {
            @Override public String getFilename() { return filename; }
        });
        restTemplate.postForObject(
            API_URL + botToken + "/sendDocument", body, String.class
        );
    }
}
```

### Cost Telegram

```
Completament gratuït — sense límits de missatges ni usuaris.
```

---

## Email — Resend

### Per què Resend

- Integració molt senzilla (REST API simple)
- Gratuït fins a 3.000 emails/mes
- Suport per a webhooks d'email entrant
- Bones taxes de lliurament

### Alta a Resend

```
1. Crear compte a resend.com
2. Verificar domini nexelocal.cat (registres DNS a cdmon)
3. Obtenir API key
4. Configurar inbound webhook (email → servidor)
```

### Configuració DNS (cdmon)

```
Registres MX per a inbound:
  MX 10 inbound.resend.com

Registres per a outbound (SPF/DKIM):
  TXT "v=spf1 include:_spf.resend.com ~all"
  CNAME resend._domainkey → resend._domainkey.resend.com
```

### Enviament d'email

```java
@Service("email")
public class EmailChannel implements MessagingChannel {

    private static final String RESEND_API = "https://api.resend.com/emails";

    @Override
    public void sendMessage(String toEmail, String text) {
        restTemplate.postForObject(
            RESEND_API,
            Map.of(
                "from", "assistent@nexelocal.cat",
                "to", List.of(toEmail),
                "subject", "Resposta del teu assistent",
                "text", text
            ),
            String.class
        );
    }

    public void sendQuotePdf(String toEmail, String subject,
                              String body, byte[] pdf) {
        String base64Pdf = Base64.getEncoder().encodeToString(pdf);
        restTemplate.postForObject(
            RESEND_API,
            Map.of(
                "from", "pressupostos@nexelocal.cat",
                "to", List.of(toEmail),
                "subject", subject,
                "text", body,
                "attachments", List.of(Map.of(
                    "filename", "pressupost.pdf",
                    "content", base64Pdf
                ))
            ),
            String.class
        );
    }
}
```

### Recepció d'emails entrants

```java
@PostMapping("/webhook/email/{tenantId}")
public ResponseEntity<Void> receiveEmail(
        @PathVariable String tenantId,
        @RequestBody ResendInboundEmail email) {

    IncomingMessage message = IncomingMessage.builder()
        .tenantId(tenantId)
        .customerPhone(email.getFrom())  // email com a identificador
        .content(email.getText())
        .channel(Channel.EMAIL)
        .build();

    agentService.handleMessage(message);
    return ResponseEntity.ok().build();
}
```

---

## Mode híbrid — implementació

El mode híbrid permet al dueño aprovar, editar o descartar les respostes de l'agent abans d'enviar-les.

```java
@Service
public class AgentService {

    public void handleMessage(IncomingMessage msg) {
        Tenant tenant = tenantService.getTenant(msg.getTenantId());
        String aiResponse = callAI(msg, tenant);

        switch (tenant.getAgentMode()) {

            case AUTO -> {
                // Envia immediatament
                getChannel(msg.getChannel())
                    .sendMessage(msg.getCustomerPhone(), aiResponse);
                saveConversation(msg, aiResponse, false);
            }

            case HYBRID -> {
                // Guarda pendent d'aprovació
                saveConversation(msg, aiResponse, true);
                // Notifica el dueño
                notifyOwnerPendingResponse(tenant, msg, aiResponse);
            }

            case MANUAL -> {
                // Només notifica el dueño, no envia res
                notifyOwnerNewMessage(tenant, msg);
            }
        }
    }
}
```

### Notificació al dueño (mode híbrid)

```
📱 WhatsApp del dueño:

"💬 Missatge nou de Maria López (WhatsApp):
'Vull un pressupost per al saló'

🤖 Resposta suggerida per l'agent:
'Hola Maria! Per fer-te un pressupost necessito
saber els m² aproximats del saló...'

👉 Revisa-ho a: nexelocal.cat/panel/garcia-pintura"
```

### API per al panel del client

```java
@RestController
@RequestMapping("/api/panel/{tenantId}")
public class PanelController {

    // Llistar respostes pendents d'aprovació
    @GetMapping("/pending")
    public List<PendingResponse> getPending(@PathVariable String tenantId) {
        return conversationRepository.findPendingByTenant(tenantId);
    }

    // Aprovar i enviar resposta de l'agent
    @PostMapping("/approve/{conversationId}")
    public void approve(@PathVariable Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId);
        getChannel(conv.getChannel()).sendMessage(conv.getCustomerPhone(), conv.getContent());
        conv.setPendingApproval(false);
        conversationRepository.save(conv);
    }

    // Editar i enviar resposta modificada
    @PostMapping("/edit/{conversationId}")
    public void editAndSend(@PathVariable Long conversationId,
                             @RequestBody String newContent) {
        Conversation conv = conversationRepository.findById(conversationId);
        getChannel(conv.getChannel()).sendMessage(conv.getCustomerPhone(), newContent);
        conv.setContent(newContent);
        conv.setPendingApproval(false);
        conversationRepository.save(conv);
    }

    // Descartar resposta de l'agent i escriure la pròpia
    @PostMapping("/manual/{tenantId}/{customerPhone}")
    public void sendManual(@PathVariable String tenantId,
                            @PathVariable String customerPhone,
                            @RequestBody ManualMessageRequest req) {
        getChannel(req.getChannel())
            .sendMessage(customerPhone, req.getContent());
        saveManualConversation(tenantId, customerPhone, req);
    }

    // Canviar mode de l'agent
    @PutMapping("/mode")
    public void setMode(@PathVariable String tenantId,
                         @RequestBody AgentMode mode) {
        tenantService.updateMode(tenantId, mode);
        // Invalida caché Redis
        cacheManager.getCache("tenants").evict(tenantId);
    }
}
```

---

## Taula resum de canals

| Canal | Cost | Setup | Aprovació | Ideal per a |
|---|---|---|---|---|
| WhatsApp (Twilio) | ~€1-7/client/mes | 1-3 setmanes | Meta | Tots els sectors |
| Telegram | Gratuït | 10 minuts | No | Pilots, clients tech |
| Email | ~€0/mes | 1 dia | No | Pressupostos, documents |
