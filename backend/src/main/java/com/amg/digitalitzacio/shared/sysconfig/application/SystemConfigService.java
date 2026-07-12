package com.amg.digitalitzacio.shared.sysconfig.application;

import com.amg.digitalitzacio.shared.sysconfig.domain.SystemConfigAuditLog;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemConfigAuditLogRepository;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSetting;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSettingRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final Environment env;
    private final SystemSettingRepository repo;
    private final SystemConfigAuditLogRepository auditRepo;
    private final VaultEncryption encryption;
    private final ObjectMapper objectMapper;

    public static final List<KnownKey> KNOWN_KEYS = List.of(
        new KnownKey("ANTHROPIC_API_KEY",      "Anthropic API Key",          "Clau per als agents IA conversacionals (Claude)", "AGENTS",   true,  "secret",   null, null, 10),
        new KnownKey("DEEPSEEK_API_KEY",        "DeepSeek API Key",           "Clau per als models DeepSeek V3 / R1 (alternativa a Claude)", "AGENTS", true, "secret", null, null, 20),
        new KnownKey("OLLAMA_BASE_URL",         "Ollama Base URL",            "URL de la instància Ollama (per defecte: http://localhost:11434)", "AGENTS", false, "url", "http://localhost:11434", null, 30),
        new KnownKey("TELEGRAM_BOT_TOKEN",      "Telegram Bot Token",         "Token del bot Telegram per a agents i notificacions", "AGENTS", true, "secret", null, null, 40),
        new KnownKey("TELEGRAM_WEBHOOK_SECRET", "Telegram Webhook Secret",    "Token secret per verificar peticions del webhook de Telegram (X-Telegram-Bot-Api-Secret-Token). Configurar a setWebhook?secret_token=<valor>", "AGENTS", true, "secret", null, null, 41),
        new KnownKey("STT_API_KEY",             "Speech-to-Text API Key",     "Clau per transcriure notes de veu de Telegram (OpenAI Whisper o compatible, p. ex. Groq)", "AGENTS", true, "secret", null, null, 45),
        new KnownKey("INFRAOPS_DISABLED_RECS",  "InfraOps — recomanacions desactivades", "Tipus de recomanació d'escalat que NO s'han de generar (separats per coma). Ex: SEPARATE_N8N. Tipus: UPGRADE_CPU, UPGRADE_RAM, UPGRADE_DISK, SEPARATE_DB, SEPARATE_N8N, MIGRATE_HETZNER_CLOUD, MEMORY_NO_SWAP.", "GENERAL", false, "string", null, null, 200),
        new KnownKey("INFRAOPS_CPU_WARN",       "InfraOps — CPU avís (%)",   "Llindar d'avís de CPU sostinguda 30 min (default 80).", "GENERAL", false, "number", "80", null, 201),
        new KnownKey("INFRAOPS_CPU_CRIT",       "InfraOps — CPU crític (%)", "Llindar crític de CPU (default 95).", "GENERAL", false, "number", "95", null, 202),
        new KnownKey("INFRAOPS_RAM_WARN",       "InfraOps — RAM avís (%)",   "Llindar d'avís de RAM sostinguda 15 min (default 85).", "GENERAL", false, "number", "85", null, 203),
        new KnownKey("INFRAOPS_RAM_CRIT",       "InfraOps — RAM crític (%)", "Llindar crític de RAM (default 95).", "GENERAL", false, "number", "95", null, 204),
        new KnownKey("INFRAOPS_DISK_WARN",      "InfraOps — Disc avís (%)",  "Llindar d'avís de disc (default 75).", "GENERAL", false, "number", "75", null, 205),
        new KnownKey("INFRAOPS_DISK_CRIT",      "InfraOps — Disc crític (%)","Llindar crític de disc (default 90).", "GENERAL", false, "number", "90", null, 206),
        new KnownKey("INFRAOPS_DB_WARN",        "InfraOps — Connexions DB avís (%)", "Llindar d'avís de connexions DB (default 80).", "GENERAL", false, "number", "80", null, 207),
        new KnownKey("INFRAOPS_AGENT_TOKEN",    "InfraOps — Token de l'agent de contenidors", "Secret compartit amb l'agent del host que reporta l'estat dels contenidors (capçalera X-Agent-Token).", "GENERAL", true, "secret", null, null, 208),
        new KnownKey("STT_BASE_URL",            "Speech-to-Text Base URL",    "Endpoint compatible OpenAI (per defecte: https://api.openai.com/v1; Groq: https://api.groq.com/openai/v1)", "AGENTS", false, "url", "https://api.openai.com/v1", null, 46),
        new KnownKey("STT_MODEL",               "Speech-to-Text Model",       "Model de transcripció (whisper-1 a OpenAI; whisper-large-v3-turbo a Groq)", "AGENTS", false, "string", "whisper-1", null, 47),
        new KnownKey("TELEGRAM_CHAT_ID",        "Telegram Chat ID (InfraOps)","ID del chat on enviar alertes d'infraestructura", "INFRAOPS", false, "string", null, null, 10),
        new KnownKey("AMG_SALES_CHAT_ID",       "Telegram Chat ID (Vendes)", "ID del chat Telegram per a alertes comercials (pressupostos, clients nous)", "VENDES", false, "string", "614492062", null, 10),
        new KnownKey("REFERRAL_SETUP_DISCOUNT_PCT", "Descompte setup referits (%)", "Percentatge de descompte al setup per al client nou que arriba amb codi de referit", "VENDES", false, "string", "50", null, 20),
        new KnownKey("PLATFORM_TENANT_ID",      "Platform Tenant ID",         "UUID del tenant AMG per a leads del formulari web (opcional)", "GENERAL", false, "string", null, null, 10),
        new KnownKey("TWILIO_ACCOUNT_SID",      "Twilio Account SID",         "SID del compte Twilio per a WhatsApp Business", "AGENTS",    false, "string", null, null, 50),
        new KnownKey("TWILIO_AUTH_TOKEN",       "Twilio Auth Token",          "Token d'autenticació Twilio", "AGENTS",                        true,  "secret", null, null, 60),
        new KnownKey("TWILIO_WHATSAPP_FROM",    "Twilio WhatsApp From",       "Número WhatsApp sender (format whatsapp:+34...)", "AGENTS",   false, "string", null, null, 70),
        new KnownKey("BREVO_API_KEY",           "Brevo API Key",              "Clau Brevo EU per a enviament d'emails transaccionals", "AGENTS", true, "secret", null, null, 80),
        new KnownKey("BREVO_SENDER_EMAIL",      "Brevo Sender Email",         "Adreça remitent pels emails (per defecte: noreply@amgdl.com)", "AGENTS", false, "string", "noreply@amgdl.com", null, 90),
        new KnownKey("GOOGLE_PLACES_API_KEY",   "Google Places API Key",      "Clau Google Places per a prospecció de negocis", "PROSPECTING", true, "secret", null, null, 10),
        new KnownKey("STRIPE_API_KEY",          "Stripe API Key (secret)",    "Clau secreta Stripe per a pagaments", "PAYMENTS",              true, "secret", null, null, 10),
        new KnownKey("STRIPE_WEBHOOK_SECRET",   "Stripe Webhook Secret",      "Secret per verificar signatures HMAC dels webhooks de Stripe (whsec_...)", "PAYMENTS", true, "secret", null, null, 20),
        new KnownKey("GOCARDLESS_WEBHOOK_SECRET","GoCardless Webhook Secret",  "Secret per verificar signatures HMAC dels webhooks de GoCardless (SEPA)", "PAYMENTS", true, "secret", null, null, 30),
        new KnownKey("HOLDED_API_KEY",          "Holded API Key",             "Clau API Holded per a facturació FinOps", "FINOPS",             true, "secret", null, null, 10),
        new KnownKey("HOLDED_API_URL",          "Holded API URL",             "URL base de l'API Holded", "FINOPS",                            false, "url", null, null, 20),
        new KnownKey("HOLDED_WEBHOOK_SECRET",   "Holded Webhook Secret",      "Token secret per verificar peticions del webhook de Holded (X-Webhook-Token)", "FINOPS", true, "secret", null, null, 30),
        new KnownKey("GCS_PROJECT_ID",          "GCS Project ID",             "ID del projecte Google Cloud Storage per a backups", "BACKUP", false, "string", null, null, 10),
        new KnownKey("GCS_BUCKET_NAME",         "GCS Bucket Name",            "Nom del bucket GCS per a backups", "BACKUP",                   false, "string", null, null, 20),
        new KnownKey("N8N_API_URL",             "n8n API URL",                "URL base de la instància n8n", "AUTOMATIONS",                   false, "url", "http://n8n:5678", null, 10),
        new KnownKey("N8N_API_KEY",             "n8n API Key",                "Clau API n8n per a gestió de workflows", "AUTOMATIONS",         true, "secret", null, null, 20),
        new KnownKey("WHATSAPP_WEBHOOK_SECRET",         "WhatsApp Webhook Secret",              "Token de verificació del webhook de WhatsApp (Twilio/Meta)", "AGENTS", true, "secret", null, null, 100),
        new KnownKey("WHATSAPP_TWILIO_COST_EUR_MICROS", "WhatsApp Twilio — cost/missatge (micros €)", "Cost total per missatge sortint via Twilio (Twilio + Meta). 1 micro = €0.000001. Default: 55000 = €0.055", "AGENTS", false, "number", "55000", "{\"min\":1000,\"max\":200000}", 110),
        new KnownKey("WHATSAPP_META_COST_EUR_MICROS", "WhatsApp Meta — cost/missatge (micros €)", "Cost per missatge via Meta Cloud API per a AMG. 0 = el tenant paga Meta directament (WABA propi). Posar cost real només si AMG passa a BSP.", "AGENTS", false, "number", "0", "{\"min\":0,\"max\":200000}", 111),
        new KnownKey("EMAIL_INBOUND_DOMAIN",      "Domini inbound d'email",    "Subdomini Cloudflare per rebre emails del bot (per defecte: inbound.amgdl.com).", "EMAIL_INBOUND", false, "string", "inbound.amgdl.com", null, 10),
        new KnownKey("MAILGUN_WEBHOOK_SIGNING_KEY","Mailgun Webhook Signing Key","Clau HMAC de Mailgun per verificar la signatura dels webhooks d'email entrant (timestamp+token)", "EMAIL_INBOUND", true, "secret", null, null, 20),
        new KnownKey("GOOGLE_CALENDAR_SA_JSON", "Google Calendar Service Account JSON", "JSON del Service Account de Google per crear i compartir calendaris (F2 Agenda)", "CALENDAR", true, "json", null, null, 10),
        new KnownKey("GOOGLE_OAUTH_CLIENT_ID",  "Google OAuth Client ID",               "Client ID de Google Cloud per al flux OAuth (connexió compte propi del client)", "CALENDAR", false, "string", null, null, 20),
        new KnownKey("GOOGLE_OAUTH_CLIENT_SECRET",  "Google OAuth Client Secret",         "Client Secret de Google Cloud per al flux OAuth del client", "CALENDAR", true, "secret", null, null, 30),
        new KnownKey("META_WEBHOOK_VERIFY_TOKEN",   "Meta Webhook Verify Token",          "Token de verificació per al webhook de Meta Lead Ads (mínim 20 caràcters)", "META_ADS", true, "secret", null, null, 10),
        new KnownKey("META_PAGE_ACCESS_TOKEN",      "Meta Page Access Token (global)",    "Token d'accés de pàgina de Meta (fallback si no hi ha token específic per pàgina)", "META_ADS", true, "secret", null, null, 20),
        new KnownKey("META_APP_SECRET",             "Meta App Secret",                   "App Secret de l'aplicació de Meta per verificar signatures HMAC dels webhooks", "META_ADS", true, "secret", null, null, 30),
        new KnownKey("OPENAI_API_KEY",              "OpenAI API Key",                     "Clau OpenAI per a generació d'imatges amb DALL·E 3 (Meta Ads creatius)", "IMAGE_GEN", true, "secret", null, null, 10),
        new KnownKey("META_ADS_MANAGEMENT_TOKEN",   "Meta Ads Management Token",          "System User Token de Meta amb permisos ads_management per crear i gestionar campanyes.", "META_ADS", true, "secret", null, null, 40),
        new KnownKey("META_ADS_APP_ID",             "Meta Ads App ID",                    "App ID de la Facebook App per a Meta Ads Management.", "META_ADS", false, "string", null, null, 50),

        // DOMAINS (OpenProvider reseller)
        new KnownKey("OPENPROVIDER_USERNAME",    "OpenProvider Username",   "Nom d'usuari del compte reseller d'OpenProvider per a registre de dominis.", "DOMAINS", false, "string", null, null, 10),
        new KnownKey("OPENPROVIDER_PASSWORD",    "OpenProvider Password",   "Contrasenya del compte reseller d'OpenProvider (es xifra a la BD).", "DOMAINS", true, "secret", null, null, 20),

        // MAINTENANCE
        new KnownKey("MAINTENANCE_MODE",     "Mode manteniment",          "Bloqueja l'accés a usuaris no-admin.", "MAINTENANCE", false, "boolean", "false", null, 10),
        new KnownKey("MAINTENANCE_MESSAGE",  "Missatge de manteniment",  "Missatge a mostrar durant el manteniment.", "MAINTENANCE", false, "string", null, null, 20),
        new KnownKey("PLATFORM_NAME",        "Nom de la plataforma",     "Nom comercial de la plataforma.", "MAINTENANCE", false, "string", "AMG Digitalització", null, 30),
        new KnownKey("PLATFORM_LOGO_URL",    "URL del logo",             "URL pública del logo global de la plataforma.", "MAINTENANCE", false, "url", null, null, 40),
        new KnownKey("DEFAULT_LOCALE",       "Locale per defecte",       "Idioma per defecte (ca, es, en, de).", "MAINTENANCE", false, "string", "ca", "{\"pattern\":\"^(ca|es|en|de)$\"}", 50),
        new KnownKey("DEFAULT_CURRENCY",     "Moneda per defecte",       "Moneda per defecte (EUR, USD, GBP).", "MAINTENANCE", false, "string", "EUR", "{\"pattern\":\"^(EUR|USD|GBP)$\"}", 60),
        new KnownKey("AGENTS_MAX_TOKENS",    "Límit màxim de tokens",    "Límit global de tokens per petició d'agent.", "MAINTENANCE", false, "number", "4096", "{\"min\":256,\"max\":128000}", 70),

        // AI MODELS
        new KnownKey("AI_MODEL_CHAT",           "Model IA — Xat general",         "Model per al xat general amb l'agent.", "AI_MODELS", false, "string", "claude-sonnet-4-20250514", null, 10),
        new KnownKey("AI_MODEL_QUOTES",         "Model IA — Pressupostos",        "Model per generar pressupostos.", "AI_MODELS", false, "string", "claude-sonnet-4-20250514", null, 20),
        new KnownKey("AI_MODEL_CONTRACTS",      "Model IA — Contractes",          "Model per generar contractes.", "AI_MODELS", false, "string", "claude-sonnet-4-20250514", null, 30),
        new KnownKey("AI_MODEL_EXTRACTION",     "Model IA — Extracció de dades",  "Model per extracció de dades de documents.", "AI_MODELS", false, "string", "gemini-2.5-pro", null, 40),
        new KnownKey("AI_MODEL_CLASSIFICATION", "Model IA — Classificació",       "Model per classificació documental.", "AI_MODELS", false, "string", "gpt-5-mini", null, 50),
        new KnownKey("AI_MODEL_OCR",            "Model IA — OCR",                 "Model per OCR d'imatges i PDFs.", "AI_MODELS", false, "string", "gemini-2.5-pro", null, 60),
        new KnownKey("AI_MODEL_SUMMARIES",      "Model IA — Resums",              "Model per resums automàtics.", "AI_MODELS", false, "string", "gpt-5-mini", null, 70),
        new KnownKey("AI_MODEL_IMAGE_GEN",      "Model IA — Generació imatges",   "Model per generació d'imatges (DALL·E, Stable Diffusion...).", "AI_MODELS", false, "string", "dall-e-3", null, 80),
        new KnownKey("DEMO_AI_PROVIDER",        "Model IA — Demo (proveïdor)",    "Proveïdor per al xat de les demos: anthropic o deepseek.", "AI_MODELS", false, "string", "anthropic", "{\"pattern\":\"^(anthropic|deepseek)$\"}", 90),
        new KnownKey("DEMO_AI_MODEL",           "Model IA — Demo (model)",        "Model concret per al xat de les demos (per defecte: claude-haiku-4-5-20251001).", "AI_MODELS", false, "string", "claude-haiku-4-5-20251001", null, 91),

        // STORAGE
        new KnownKey("STORAGE_DEFAULT_PROVIDER",  "Proveïdor per defecte",      "minio, google_drive, o local.", "STORAGE", false, "string", "minio", "{\"pattern\":\"^(minio|google_drive|local)$\"}", 10),
        new KnownKey("STORAGE_DOCUMENTS",         "Documents generats",         "On es guarden PDFs de documents.", "STORAGE", false, "string", "minio", "{\"pattern\":\"^(minio|google_drive|local)$\"}", 20),
        new KnownKey("STORAGE_LOGOS",             "Logos",                      "On es guarden logos d'empresa.", "STORAGE", false, "string", "minio", "{\"pattern\":\"^(minio|google_drive|local)$\"}", 30),
        new KnownKey("STORAGE_IMAGES",            "Imatges (Assets)",           "On es guarden imatges pujades.", "STORAGE", false, "string", "minio", "{\"pattern\":\"^(minio|google_drive|local)$\"}", 40),
        new KnownKey("STORAGE_BACKUPS",           "Còpies de seguretat",        "On es guarden els backups.", "STORAGE", false, "string", "minio", "{\"pattern\":\"^(minio|local)$\"}", 50),
        new KnownKey("STORAGE_MAX_FILE_SIZE_MB",  "Mida màxima de fitxer (MB)", "Límit de mida per fitxer pujat.", "STORAGE", false, "number", "50", "{\"min\":1,\"max\":500}", 60),
        new KnownKey("STORAGE_RETENTION_DAYS",    "Retenció (dies)",            "Dies que es conserven els fitxers.", "STORAGE", false, "number", "365", "{\"min\":30,\"max\":3650}", 70),
        new KnownKey("ASSETS_QUOTA_MB",           "Quota d'assets per tenant (MB)", "Espai màxim d'emmagatzematge per tenant. Per defecte: 500 MB.", "STORAGE", false, "number", "500", "{\"min\":10,\"max\":10240}", 80),

        // NOTIFICACIONS COMERCIALS (Telegram AMG)
        new KnownKey("AMG_NOTIFY_LEAD_CREATED",     "Notificar: nou lead",            "Enviar Telegram a vendes quan es crea un nou lead.", "VENDES", false, "boolean", "true", null, 20),
        new KnownKey("AMG_NOTIFY_BUDGET_SENT",       "Notificar: pressupost enviat",   "Enviar Telegram a vendes quan s'envia un pressupost.", "VENDES", false, "boolean", "true", null, 30),
        new KnownKey("AMG_NOTIFY_BUDGET_ACCEPTED",   "Notificar: pressupost acceptat", "Enviar Telegram a vendes quan un client accepta un pressupost.", "VENDES", false, "boolean", "true", null, 40),
        new KnownKey("AMG_NOTIFY_PAYMENT_RECEIVED",  "Notificar: pagament rebut",      "Enviar Telegram a vendes quan es confirma un pagament (Stripe o GoCardless).", "VENDES", false, "boolean", "true", null, 50),
        new KnownKey("AMG_NOTIFY_AGENT_ERROR",       "Notificar: error d'agent",       "Enviar Telegram a vendes quan un agent IA falla de forma crítica.", "VENDES", false, "boolean", "true", null, 60),
        new KnownKey("AMG_NOTIFY_CONTACT_FORM",      "Notificar: formulari de contacte","Enviar Telegram a vendes quan s'envia un formulari des d'una landing.", "VENDES", false, "boolean", "true", null, 70),
        new KnownKey("AMG_NOTIFY_WEB_CONTACT",       "Notificar: consulta web amgdl.com","Enviar Telegram a vendes quan arriba una consulta del formulari d'amgdl.com.", "VENDES", false, "boolean", "true", null, 71),
        new KnownKey("AMG_NOTIFY_WIDGET_VISIT",      "Notificar: visitant al widget",  "Enviar Telegram a vendes per cada sessió nova del chat widget. Desactivar si genera soroll.", "VENDES", false, "boolean", "true", null, 72),
        new KnownKey("AMG_NOTIFY_PROSPECT_PRIORITY", "Notificar: prospect PRIORITY",   "Enviar Telegram a vendes quan un prospect arriba a tier PRIORITY (score 81+).", "VENDES", false, "boolean", "true", null, 73),
        new KnownKey("AMG_DAILY_DIGEST",             "Digest del matí (8:00)",         "Resum diari accionable al xat de vendes: leads nous, pressupostos pendents, fitxes sense omplir, cobraments fallits.", "VENDES", false, "boolean", "true", null, 74)
    );

    public String get(String key) {
        String envVal = env.getProperty(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return repo.findById(key)
                .map(s -> {
                    try {
                        return encryption.decrypt(s.getEncryptedValue());
                    } catch (Exception e) {
                        log.error("Error desxifrant system setting {}: {}", key, e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    public boolean isConfigured(String key) {
        String val = get(key);
        return val != null && !val.isBlank();
    }

    @Transactional
    public void setInternal(String key, String value) {
        set(key, value, false, null, "text", null, null, null, null, null, null);
    }

    @Transactional
    public String set(String key, String rawValue, boolean isSecret, String description,
                      String valueType, String defaultValue, String validationRules, Integer sortOrder,
                      UUID userId, String userEmail, String ip) {
        Optional<KnownKey> known = KNOWN_KEYS.stream().filter(k -> k.key().equals(key)).findFirst();
        String resolvedType = known.map(KnownKey::type).orElse(valueType != null ? valueType : "secret");
        String validationMsg = validate(rawValue, resolvedType, known.map(KnownKey::validationRules).orElse(validationRules));
        if (validationMsg != null) return validationMsg;

        String previousValue = null;
        var setting = repo.findById(key).orElse(null);
        if (setting != null) {
            previousValue = setting.getEncryptedValue();
        }

        var builder = setting != null
                ? setting.toBuilder()
                : SystemSetting.builder().key(key);
        builder
            .encryptedValue(encryption.encrypt(rawValue))
            .isSecret(isSecret)
            .description(description != null ? description : known.map(KnownKey::description).orElse(null))
            .valueType(resolvedType)
            .defaultValue(defaultValue != null ? defaultValue : known.map(KnownKey::defaultValue).orElse(null))
            .validationRules(validationRules != null ? validationRules : known.map(KnownKey::validationRules).orElse(null))
            .sortOrder(sortOrder != null ? sortOrder : known.map(KnownKey::sortOrder).orElse(0));
        repo.save(builder.build());
        log.info("System setting {} actualitzat", key);

        if (userId != null) {
            auditLog(key, "SET", previousValue, userId, userEmail, ip);
        }
        return null;
    }

    @Transactional
    public void delete(String key, UUID userId, String userEmail, String ip) {
        var setting = repo.findById(key).orElse(null);
        if (setting != null) {
            String prevEncrypted = setting.getEncryptedValue();
            repo.delete(setting);
            if (userId != null) {
                auditLog(key, "DELETE", prevEncrypted, userId, userEmail, ip);
            }
        }
    }

    private void auditLog(String configKey, String action, String previousValue, UUID userId, String userEmail, String ip) {
        var logEntry = SystemConfigAuditLog.builder()
                .id(UUID.randomUUID())
                .configKey(configKey)
                .action(action)
                .previousValue(previousValue)
                .userId(userId)
                .userEmail(userEmail)
                .ip(ip)
                .changedAt(java.time.Instant.now())
                .build();
        auditRepo.save(logEntry);
    }

    public List<ConfigStatus> listStatus() {
        var dbKeys = repo.findAll();
        return KNOWN_KEYS.stream().map(k -> {
            boolean envConfigured = env.getProperty(k.key()) != null && !env.getProperty(k.key(), "").isBlank();
            boolean dbConfigured = dbKeys.stream().anyMatch(s -> s.getKey().equals(k.key()));
            boolean hasDefault = k.defaultValue() != null && !k.defaultValue().isBlank();
            String source = envConfigured ? "ENV" : dbConfigured ? "DB" : hasDefault ? "DEFAULT" : "MISSING";
            return new ConfigStatus(k.key(), k.label(), k.description(), k.category(), k.secret(), k.type(),
                    envConfigured || dbConfigured || hasDefault, source, null);
        }).toList();
    }

    public ConfigStatus getDetailed(String key) {
        return KNOWN_KEYS.stream()
                .filter(k -> k.key().equals(key))
                .findFirst()
                .map(k -> {
                    boolean envConfigured = env.getProperty(k.key()) != null && !env.getProperty(k.key(), "").isBlank();
                    boolean dbConfigured = repo.findById(k.key()).isPresent();
                    boolean hasDefault = k.defaultValue() != null && !k.defaultValue().isBlank();
                    String source = envConfigured ? "ENV" : dbConfigured ? "DB" : hasDefault ? "DEFAULT" : "MISSING";
                    String currentValue = k.secret() ? null : get(k.key());
                    return new ConfigStatus(k.key(), k.label(), k.description(), k.category(), k.secret(), k.type(),
                            envConfigured || dbConfigured || hasDefault, source, currentValue);
                })
                .orElse(null);
    }

    public List<SystemConfigAuditLog> getAuditLog(String key) {
        return auditRepo.findByConfigKeyOrderByChangedAtDesc(key);
    }

    private String validate(String value, String type, String validationRules) {
        if (value == null || value.isBlank()) return null;
        return switch (type) {
            case "url" -> {
                try {
                    new URL(value);
                    yield null;
                } catch (Exception e) {
                    yield "Ha de ser una URL vàlida (http:// o https://).";
                }
            }
            case "number" -> {
                try {
                    double n = Double.parseDouble(value);
                    if (validationRules != null) {
                        try {
                            JsonNode rules = objectMapper.readTree(validationRules);
                            if (rules.has("min") && n < rules.get("min").asDouble())
                                yield "El valor mínim és " + rules.get("min").asDouble() + ".";
                            if (rules.has("max") && n > rules.get("max").asDouble())
                                yield "El valor màxim és " + rules.get("max").asDouble() + ".";
                        } catch (Exception ignored) {}
                    }
                    yield null;
                } catch (NumberFormatException e) {
                    yield "Ha de ser un número vàlid.";
                }
            }
            case "boolean" -> {
                if ("true".equals(value) || "false".equals(value)) yield null;
                yield "Ha de ser 'true' o 'false'.";
            }
            case "json" -> {
                try {
                    objectMapper.readTree(value);
                    yield null;
                } catch (Exception e) {
                    yield "Ha de ser un JSON vàlid.";
                }
            }
            case "secret" -> {
                if (value.length() < 8) yield "Mínim 8 caràcters per a claus secretes.";
                yield null;
            }
            default -> {
                if (validationRules != null) {
                    try {
                        JsonNode rules = objectMapper.readTree(validationRules);
                        if (rules.has("pattern") && !value.matches(rules.get("pattern").asText()))
                            yield "El valor no compleix el format requerit.";
                        if (rules.has("minLength") && value.length() < rules.get("minLength").asInt())
                            yield "Mínim " + rules.get("minLength").asInt() + " caràcters.";
                        if (rules.has("maxLength") && value.length() > rules.get("maxLength").asInt())
                            yield "Màxim " + rules.get("maxLength").asInt() + " caràcters.";
                    } catch (Exception ignored) {}
                }
                yield null;
            }
        };
    }

    public record KnownKey(String key, String label, String description, String category, boolean secret,
                           String type, String defaultValue, String validationRules, int sortOrder) {}
    public record ConfigStatus(String key, String label, String description, String category, boolean secret,
                               String type, boolean configured, String source, String currentValue) {}
}
