package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleConnectionRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Vigilant de connexions Google (mòdul 40): un token revocat o caducat sense
 * refresh fa que calendari/Drive/Gmail deixin de funcionar EN SILENCI — les
 * cites no es creen i ningú se n'assabenta. Comprova cada dia que el refresh
 * funciona i alerta l'equip AMG si una connexió ha mort.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class GoogleConnectionWatchdogScheduler {

    private static final String REDIS_KEY = "google-dead:%s";
    private static final int REALERT_DAYS = 3;

    private final GoogleConnectionRepository connectionRepository;
    private final GoogleTokenService tokenService;
    private final TenantRepository tenantRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "0 30 8 * * *")
    public void checkConnections() {
        var connections = connectionRepository.findAll().stream()
                .filter(com.amg.digitalitzacio.google.domain.GoogleConnection::isActive)
                .toList();
        if (connections.isEmpty()) return;

        int dead = 0;
        for (var conn : connections) {
            try {
                // Forcem un refresh: si el token de refresh és mort, expiresAt no avança
                var refreshed = tokenService.refreshTokens(conn);
                boolean alive = refreshed.getTokenExpiresAt() != null
                        && refreshed.getTokenExpiresAt().isAfter(Instant.now().plus(5, ChronoUnit.MINUTES));
                if (alive) {
                    redis.delete(REDIS_KEY.formatted(conn.getTenantId()));
                    continue;
                }
                dead++;
                String redisKey = REDIS_KEY.formatted(conn.getTenantId());
                if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue; // ja alertat fa poc

                String tenantName = tenantRepository.findById(conn.getTenantId())
                        .map(t -> t.getName() != null ? t.getName() : "—").orElse("—");
                sendAlert(tenantName, conn.getTenantId().toString(), conn.getGoogleAccountEmail());
                redis.opsForValue().set(redisKey, "1", REALERT_DAYS, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("[GoogleWatchdog] Error comprovant connexió del tenant {}: {}",
                        conn.getTenantId(), e.getMessage());
            }
        }
        log.info("[GoogleWatchdog] {} connexions comprovades, {} mortes", connections.size(), dead);
    }

    private void sendAlert(String tenantName, String tenantId, String googleEmail) {
        try {
            String chatIdStr = sysConfig.get("AMG_SALES_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) return;
            String msg = """
                    🔌 <b>Connexió Google caiguda</b> — %s
                    El compte %s no es pot refrescar: calendari, Drive i Gmail del client han deixat de funcionar.
                    Cal que el client torni a autoritzar l'accés.
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(tenantName, googleEmail != null ? googleEmail : "—", tenantId);
            telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), msg);
        } catch (Exception e) {
            log.warn("[GoogleWatchdog] Error enviant alerta: {}", e.getMessage());
        }
    }
}
