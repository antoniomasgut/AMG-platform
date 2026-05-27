package com.amg.digitalitzacio.telegram.api;

import com.amg.digitalitzacio.telegram.api.dto.TelegramConfigResponse;
import com.amg.digitalitzacio.telegram.api.dto.TelegramConnectRequest;
import com.amg.digitalitzacio.telegram.application.TenantTelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telegram/tenants/{tenantId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TelegramConfigController {

    private final TenantTelegramService telegramService;

    @GetMapping("/config")
    public ResponseEntity<TelegramConfigResponse> getConfig(@PathVariable UUID tenantId) {
        var config = telegramService.getConfig(tenantId);
        return config != null ? ResponseEntity.ok(config) : ResponseEntity.notFound().build();
    }

    @PostMapping("/connect")
    public ResponseEntity<TelegramConfigResponse> connect(
            @PathVariable UUID tenantId,
            @RequestBody TelegramConnectRequest req) {
        return ResponseEntity.ok(telegramService.connect(tenantId, req));
    }

    @PostMapping("/verify")
    public ResponseEntity<TelegramConfigResponse> verify(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(telegramService.verify(tenantId));
    }

    @DeleteMapping("/config")
    public ResponseEntity<Void> disconnect(@PathVariable UUID tenantId) {
        telegramService.disconnect(tenantId);
        return ResponseEntity.noContent().build();
    }
}
