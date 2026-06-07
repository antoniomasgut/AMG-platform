package com.amg.digitalitzacio.ops.application;

import com.amg.digitalitzacio.ops.domain.*;
import com.amg.digitalitzacio.shared.notification.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckerScheduler {

    private static final int FAILURE_THRESHOLD = 6;
    private static final long ALERT_COOLDOWN_SECONDS = 1800;

    private final ServiceHealthRepository serviceHealthRepository;
    private final IncidentRepository incidentRepository;
    private final DataSource dataSource;
    private final TelegramNotifier telegramNotifier;

    @Value("${app.healthcheck.n8n-url:http://localhost:5678/healthz}")
    private String n8nUrl;

    @Value("${app.healthcheck.minio-url:http://localhost:9001/minio/health/live}")
    private String minioUrl;

    @Value("${app.healthcheck.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.healthcheck.traefik-url:http://localhost:8082/ping}")
    private String traefikUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkAll() {
        checkService("backend", this::checkBackend);
        checkService("postgres", this::checkPostgres);
        checkService("redis", this::checkRedis);
        checkService("n8n", () -> checkHttp(n8nUrl));
        checkService("minio", () -> checkHttp(minioUrl));
        checkService("frontend", () -> checkHttp(frontendUrl));
        checkService("traefik", () -> checkHttp(traefikUrl));
    }

    private void checkService(String serviceName, ServiceCheck check) {
        var start = System.currentTimeMillis();
        try {
            var healthy = check.check();
            var responseTime = System.currentTimeMillis() - start;
            var status = healthy ? ServiceStatus.UP : ServiceStatus.DOWN;
            saveHealth(serviceName, status, responseTime, healthy ? null : "Health check failed");
            handleIncident(serviceName, status);
        } catch (Exception e) {
            var responseTime = System.currentTimeMillis() - start;
            saveHealth(serviceName, ServiceStatus.DOWN, responseTime, e.getMessage());
            handleIncident(serviceName, ServiceStatus.DOWN);
        }
    }

    private boolean checkBackend() {
        return true;
    }

    private boolean checkPostgres() {
        try (var conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            var factory = dataSource.getClass().getClassLoader().loadClass(
                    "org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory");
            var conn = factory.getMethod("getConnection").invoke(null);
            var ping = conn.getClass().getMethod("ping").invoke(conn);
            return "PONG".equals(ping);
        } catch (Exception e) {
            log.debug("Redis not available, skipping health check: {}", e.getMessage());
            return true;
        }
    }

    private boolean checkHttp(String url) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveHealth(String serviceName, ServiceStatus status, long responseTime, String error) {
        var health = ServiceHealth.builder()
                .serviceName(serviceName)
                .status(status)
                .responseTimeMs(responseTime)
                .errorMessage(error)
                .checkedAt(Instant.now())
                .build();
        serviceHealthRepository.save(health);
    }

    private void handleIncident(String serviceName, ServiceStatus currentStatus) {
        var last6 = serviceHealthRepository
                .findTopByServiceNameOrderByCheckedAtDesc(serviceName, FAILURE_THRESHOLD);

        if (currentStatus == ServiceStatus.DOWN && last6.size() >= FAILURE_THRESHOLD) {
            boolean allDown = last6.stream().allMatch(h -> h.getStatus() == ServiceStatus.DOWN);
            if (!allDown) return;

            var openIncidents = incidentRepository.findByServiceNameAndStatus(serviceName, IncidentStatus.OPEN);
            if (!openIncidents.isEmpty()) {
                var incident = openIncidents.get(0);
                if (!incident.isAlertSent() && canSendAlert(serviceName)) {
                    incident.setAlertSent(true);
                    incidentRepository.save(incident);
                    telegramNotifier.send(formatDownAlert(serviceName, incident));
                }
                return;
            }

            var incident = Incident.builder()
                    .serviceName(serviceName)
                    .severity(IncidentSeverity.CRITICAL)
                    .status(IncidentStatus.OPEN)
                    .title("Servei " + serviceName + " no disponible")
                    .description("El servei " + serviceName + " porta " + (FAILURE_THRESHOLD * 30) + " segons sense respondre")
                    .startedAt(Instant.now())
                    .alertSent(true)
                    .build();
            incident = incidentRepository.save(incident);
            telegramNotifier.send(formatDownAlert(serviceName, incident));
        }

        if (currentStatus == ServiceStatus.UP) {
            var openIncidents = incidentRepository.findByServiceNameAndStatus(serviceName, IncidentStatus.OPEN);
            for (var incident : openIncidents) {
                incident.setStatus(IncidentStatus.RESOLVED);
                incident.setResolvedAt(Instant.now());
                incident.setDurationSeconds(Duration.between(incident.getStartedAt(), incident.getResolvedAt()).getSeconds());
                if (!incident.isAlertRecovered()) {
                    incident.setAlertRecovered(true);
                    telegramNotifier.send(formatRecoveryAlert(serviceName, incident));
                }
                incidentRepository.save(incident);
            }
        }
    }

    private boolean canSendAlert(String serviceName) {
        var resolved = incidentRepository
                .findTopByServiceNameAndStatusOrderByResolvedAtDesc(serviceName, IncidentStatus.RESOLVED);
        return resolved.map(r -> {
            if (r.getResolvedAt() == null) return true;
            return Duration.between(r.getResolvedAt(), Instant.now()).getSeconds() > ALERT_COOLDOWN_SECONDS;
        }).orElse(true);
    }

    private String formatDownAlert(String serviceName, Incident inc) {
        return "🔴 <b>AMG Platform</b> · " + serviceName
                + "\nEstat: <b>DOWN</b>"
                + "\nTítol: " + inc.getTitle()
                + "\nInici: " + inc.getStartedAt();
    }

    private String formatRecoveryAlert(String serviceName, Incident inc) {
        var duration = inc.getDurationSeconds() != null ? inc.getDurationSeconds() + "s" : "?";
        return "✅ <b>AMG Platform</b> · " + serviceName
                + "\nEstat: <b>RECUPERAT</b>"
                + "\nTemps de caiguda: " + duration;
    }

    @FunctionalInterface
    private interface ServiceCheck {
        boolean check() throws Exception;
    }
}
