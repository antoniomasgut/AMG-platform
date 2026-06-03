package com.amg.digitalitzacio.shared.sysconfig.application;

import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSetting;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSettingRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Llegeix API keys: primer variable d'entorn, si no DB xifrada.
 * Permet que l'admin les configuri des del portal sense reiniciar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final Environment env;
    private final SystemSettingRepository repo;
    private final VaultEncryption encryption;

    /** Definició de totes les claus conegudes del sistema */
    public static final List<KnownKey> KNOWN_KEYS = List.of(
        new KnownKey("ANTHROPIC_API_KEY",      "Anthropic API Key",          "Clau per als agents IA conversacionals (Claude)", "AGENTS",   true),
        new KnownKey("DEEPSEEK_API_KEY",        "DeepSeek API Key",           "Clau per als models DeepSeek V3 / R1 (alternativa a Claude)", "AGENTS", true),
        new KnownKey("OLLAMA_BASE_URL",         "Ollama Base URL",            "URL de la instància Ollama (per defecte: http://localhost:11434)", "AGENTS", false),
        new KnownKey("TELEGRAM_BOT_TOKEN",      "Telegram Bot Token",         "Token del bot Telegram per a agents i notificacions", "AGENTS", true),
        new KnownKey("TELEGRAM_CHAT_ID",        "Telegram Chat ID (InfraOps)","ID del chat on enviar alertes d'infraestructura", "INFRAOPS", false),
        new KnownKey("PLATFORM_TENANT_ID",      "Platform Tenant ID",         "UUID del tenant AMG per a leads del formulari web (opcional)", "GENERAL", false),
        new KnownKey("TWILIO_ACCOUNT_SID",      "Twilio Account SID",         "SID del compte Twilio per a WhatsApp Business", "AGENTS",    false),
        new KnownKey("TWILIO_AUTH_TOKEN",       "Twilio Auth Token",          "Token d'autenticació Twilio", "AGENTS",                        true),
        new KnownKey("TWILIO_WHATSAPP_FROM",    "Twilio WhatsApp From",       "Número WhatsApp sender (format whatsapp:+34...)", "AGENTS",   false),
        new KnownKey("BREVO_API_KEY",           "Brevo API Key",              "Clau Brevo EU per a enviament d'emails transaccionals", "AGENTS", true),
        new KnownKey("BREVO_SENDER_EMAIL",      "Brevo Sender Email",         "Adreça remitent pels emails (per defecte: noreply@amgdl.com)", "AGENTS", false),
        new KnownKey("GOOGLE_PLACES_API_KEY",   "Google Places API Key",      "Clau Google Places per a prospecció de negocis", "PROSPECTING", true),
        new KnownKey("STRIPE_API_KEY",          "Stripe API Key (secret)",    "Clau secreta Stripe per a pagaments", "PAYMENTS",              true),
        new KnownKey("HOLDED_API_KEY",          "Holded API Key",             "Clau API Holded per a facturació FinOps", "FINOPS",             true),
        new KnownKey("HOLDED_API_URL",          "Holded API URL",             "URL base de l'API Holded", "FINOPS",                            false),
        new KnownKey("GCS_PROJECT_ID",          "GCS Project ID",             "ID del projecte Google Cloud Storage per a backups", "BACKUP", false),
        new KnownKey("GCS_BUCKET_NAME",         "GCS Bucket Name",            "Nom del bucket GCS per a backups", "BACKUP",                   false),
        new KnownKey("N8N_API_URL",             "n8n API URL",                "URL base de la instància n8n", "AUTOMATIONS",                   false),
        new KnownKey("N8N_API_KEY",             "n8n API Key",                "Clau API n8n per a gestió de workflows", "AUTOMATIONS",         true),
        new KnownKey("WHATSAPP_WEBHOOK_SECRET", "WhatsApp Webhook Secret",     "Token de verificació del webhook de WhatsApp (Twilio/Meta)", "AGENTS", true),
        new KnownKey("EMAIL_INBOUND_DOMAIN",   "Domini inbound d'email", "Subdomini Cloudflare per rebre emails del bot (per defecte: inbound.amgdl.com). Només informatiu — canviar-lo aquí no modifica el DNS.", "EMAIL_INBOUND", false),
        new KnownKey("GOOGLE_CALENDAR_SA_JSON", "Google Calendar Service Account JSON", "JSON del Service Account de Google per crear i compartir calendaris (F2 Agenda)", "CALENDAR", true),
        new KnownKey("GOOGLE_OAUTH_CLIENT_ID",  "Google OAuth Client ID",               "Client ID de Google Cloud per al flux OAuth (connexió compte propi del client)", "CALENDAR", false),
        new KnownKey("GOOGLE_OAUTH_CLIENT_SECRET", "Google OAuth Client Secret",         "Client Secret de Google Cloud per al flux OAuth del client", "CALENDAR", true)
    );

    /**
     * Retorna el valor d'una clau: env var → DB. Null si no configurada.
     * Mètode principal que han d'usar els serveis.
     */
    public String get(String key) {
        // 1. Variable d'entorn té prioritat (permet overrides sense reinici de BD)
        String envVal = env.getProperty(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        // 2. Fallback a DB xifrada
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
    public void set(String key, String rawValue, boolean isSecret, String description) {
        var setting = repo.findById(key)
                .orElse(SystemSetting.builder().key(key).isSecret(isSecret).description(description).build());
        setting.setEncryptedValue(encryption.encrypt(rawValue));
        setting.setSecret(isSecret);
        if (description != null) setting.setDescription(description);
        repo.save(setting);
        log.info("System setting {} actualitzat", key);
    }

    @Transactional
    public void delete(String key) {
        repo.deleteById(key);
    }

    public List<ConfigStatus> listStatus() {
        var dbKeys = repo.findAll();
        return KNOWN_KEYS.stream().map(k -> {
            boolean envConfigured = env.getProperty(k.key()) != null && !env.getProperty(k.key(), "").isBlank();
            boolean dbConfigured = dbKeys.stream().anyMatch(s -> s.getKey().equals(k.key()));
            return new ConfigStatus(k.key(), k.label(), k.description(), k.category(), k.secret(),
                    envConfigured || dbConfigured, envConfigured ? "ENV" : dbConfigured ? "DB" : "MISSING");
        }).toList();
    }

    public record KnownKey(String key, String label, String description, String category, boolean secret) {}
    public record ConfigStatus(String key, String label, String description, String category, boolean secret,
                               boolean configured, String source) {}
}
