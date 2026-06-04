package com.amg.digitalitzacio.shared.notification;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/tenants/{tenantId}/config")
@RequiredArgsConstructor
public class TenantNotificationConfigController {

    private final TenantNotificationConfigRepository configRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #tenantId == #principal.tenantId()) or (hasRole('CLIENT') and #tenantId == #principal.tenantId())")
    public ResponseEntity<NotificationConfigResponse> getConfig(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var config = configRepository.findById(tenantId)
                .orElse(TenantNotificationConfig.defaultFor(tenantId));
        return ResponseEntity.ok(NotificationConfigResponse.from(config));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #tenantId == #principal.tenantId()) or (hasRole('CLIENT') and #tenantId == #principal.tenantId())")
    public ResponseEntity<NotificationConfigResponse> updateConfig(
            @PathVariable UUID tenantId,
            @RequestBody NotificationConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        var config = configRepository.findById(tenantId)
                .orElse(TenantNotificationConfig.builder().tenantId(tenantId).build());

        config.setEnabled(req.enabled());

        if (req.telegram() != null) {
            config.setTgContactForm(req.telegram().contactForm());
            config.setTgChatWidgetNew(req.telegram().chatWidgetNew());
            config.setTgWhatsappNew(req.telegram().whatsappNew());
            config.setTgEmailNew(req.telegram().emailNew());
            config.setTgLeadCreated(req.telegram().leadCreated());
            config.setTgBooking(req.telegram().booking());
        }
        if (req.email() != null) {
            config.setEmContactForm(req.email().contactForm());
            config.setEmBooking(req.email().booking());
        }
        if (req.quietHours() != null) {
            config.setQuietStart(req.quietHours().start());
            config.setQuietEnd(req.quietHours().end());
            if (req.quietHours().timezone() != null && !req.quietHours().timezone().isBlank()) {
                config.setTimezone(req.quietHours().timezone());
            }
        }
        config.setCooldownMinutes(req.cooldownMinutes());
        config.setUpdatedAt(Instant.now());

        configRepository.save(config);
        return ResponseEntity.ok(NotificationConfigResponse.from(config));
    }
}
