package com.amg.digitalitzacio.shared.notification;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantNotificationService {

    private static final String COOLDOWN_KEY = "notif:cooldown:%s:%s";

    private final TenantNotificationConfigRepository configRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final TelegramBotClient telegramBotClient;
    private final EmailService emailService;
    private final StringRedisTemplate redis;

    public void notify(UUID tenantId, NotificationEvent event, Map<String, String> params) {
        try {
            var config = configRepository.findById(tenantId)
                    .orElse(TenantNotificationConfig.defaultFor(tenantId));

            if (!config.isEnabled()) return;

            if (config.isTelegramEnabledFor(event) && !isInQuietHours(config) && !isOnCooldown(tenantId, event, config)) {
                sendTelegram(tenantId, event, params);
                startCooldown(tenantId, event, config);
            }

            if (config.isEmailEnabledFor(event)) {
                sendEmail(tenantId, event, params);
            }
        } catch (Exception e) {
            log.warn("[Notif] Error enviant notificació {} per tenant {}: {}", event, tenantId, e.getMessage());
        }
    }

    private void sendTelegram(UUID tenantId, NotificationEvent event, Map<String, String> params) {
        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null || chatLink.getTelegramChatId() == null) return;

        String message = buildTelegramMessage(event, params);
        telegramBotClient.sendMessage(chatLink.getTelegramChatId(), message);
        log.debug("[Notif] Telegram {} enviat al tenant {}", event, tenantId);
    }

    private void sendEmail(UUID tenantId, NotificationEvent event, Map<String, String> params) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isBlank()) return;

        var subjectAndBody = buildEmailContent(event, params);
        emailService.sendEmail(tenant.getEmail(), subjectAndBody[0], subjectAndBody[1]);
        log.debug("[Notif] Email {} enviat al tenant {}", event, tenantId);
    }

    private String buildTelegramMessage(NotificationEvent event, Map<String, String> p) {
        return switch (event) {
            case CONTACT_FORM -> """
                    📬 <b>Nou contacte</b> · %s
                    👤 %s · %s · %s
                    💬 %s
                    🔗 <a href="https://amgdl.com/portal/leads">Veure leads →</a>""".formatted(
                    get(p, "landing_title"),
                    get(p, "nom"), get(p, "email"), get(p, "phone"),
                    truncate(get(p, "message"), 120));

            case CHAT_WIDGET_NEW -> """
                    💬 <b>Nou xat</b> · %s
                    🌐 Primera visita des del widget de la web""".formatted(get(p, "business_name"));

            case WHATSAPP_NEW -> """
                    📱 <b>Nou WhatsApp</b> · %s
                    💬 %s""".formatted(get(p, "identifier"), truncate(get(p, "message"), 100));

            case EMAIL_NEW -> """
                    ✉️ <b>Nou email</b> · %s
                    📋 %s
                    💬 %s""".formatted(get(p, "identifier"), get(p, "subject"), truncate(get(p, "message"), 100));

            case BOOKING_CONFIRMED -> """
                    📅 <b>Cita confirmada</b>
                    👤 %s · %s %s (%s min)
                    📝 %s""".formatted(
                    get(p, "nom_client"),
                    get(p, "data"), get(p, "hora"), get(p, "duracio"),
                    get(p, "notes"));

            case LEAD_CREATED -> """
                    ✅ <b>Nou lead creat</b>
                    👤 %s · %s
                    📌 Etapa: %s""".formatted(get(p, "nom"), get(p, "contact"), get(p, "stage"));
        };
    }

    private String[] buildEmailContent(NotificationEvent event, Map<String, String> p) {
        return switch (event) {
            case CONTACT_FORM -> new String[]{
                "Nou contacte: " + get(p, "landing_title"),
                "Nou contacte des de '%s':\n\nNom: %s\nEmail: %s\nTelèfon: %s\nMissatge: %s\n\nVeure leads: https://amgdl.com/portal/leads"
                    .formatted(get(p, "landing_title"), get(p, "nom"), get(p, "email"), get(p, "phone"), get(p, "message"))
            };
            case BOOKING_CONFIRMED -> new String[]{
                "Cita confirmada · " + get(p, "nom_client"),
                "S'ha confirmat una cita:\n\nClient: %s\nData: %s a les %s\nDurada: %s min\nNotes: %s"
                    .formatted(get(p, "nom_client"), get(p, "data"), get(p, "hora"), get(p, "duracio"), get(p, "notes"))
            };
            default -> new String[]{"Notificació", event.name()};
        };
    }

    private boolean isInQuietHours(TenantNotificationConfig config) {
        if (config.getQuietStart() == null || config.getQuietEnd() == null) return false;
        try {
            ZoneId zone = ZoneId.of(config.getTimezone());
            int hour = LocalTime.now(zone).getHour();
            int start = config.getQuietStart();
            int end   = config.getQuietEnd();
            if (start < end) return hour >= start && hour < end;
            return hour >= start || hour < end; // overnight (e.g. 22-8)
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOnCooldown(UUID tenantId, NotificationEvent event, TenantNotificationConfig config) {
        if (config.getCooldownMinutes() <= 0) return false;
        return Boolean.TRUE.equals(redis.hasKey(COOLDOWN_KEY.formatted(tenantId, event.name())));
    }

    private void startCooldown(UUID tenantId, NotificationEvent event, TenantNotificationConfig config) {
        if (config.getCooldownMinutes() <= 0) return;
        redis.opsForValue().set(COOLDOWN_KEY.formatted(tenantId, event.name()), "1",
                config.getCooldownMinutes(), TimeUnit.MINUTES);
    }

    private static String get(Map<String, String> p, String key) {
        return p.getOrDefault(key, "—");
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s != null ? s : "—";
        return s.substring(0, max) + "…";
    }
}
