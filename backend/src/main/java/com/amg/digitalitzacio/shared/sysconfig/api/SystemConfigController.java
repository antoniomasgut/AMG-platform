package com.amg.digitalitzacio.shared.sysconfig.api;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService.ConfigStatus;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigTestService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemConfigController {

    private final SystemConfigService service;
    private final SystemConfigTestService testService;

    @GetMapping
    public List<ConfigStatus> list() {
        return service.listStatus();
    }

    @GetMapping("/{key}")
    public ResponseEntity<ConfigStatus> getDetail(@PathVariable String key) {
        var detail = service.getDetailed(key);
        if (detail == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(detail);
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, String>> set(
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal user) {

        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El valor no pot estar buit"));
        }

        var known = SystemConfigService.KNOWN_KEYS.stream()
                .filter(k -> k.key().equals(key))
                .findFirst();

        String description = known.map(SystemConfigService.KnownKey::description).orElse(null);
        boolean isSecret = known.map(SystemConfigService.KnownKey::secret).orElse(true);
        String valueType = known.map(SystemConfigService.KnownKey::type).orElse("secret");
        String defaultValue = known.map(SystemConfigService.KnownKey::defaultValue).orElse(null);
        String validationRules = known.map(SystemConfigService.KnownKey::validationRules).orElse(null);
        Integer sortOrder = known.map(SystemConfigService.KnownKey::sortOrder).orElse(0);

        UUID userId = user != null ? user.id() : null;
        String userEmail = user != null ? user.email() : null;
        String ip = extractIp();

        String error = service.set(key, value, isSecret, description, valueType, defaultValue, validationRules, sortOrder, userId, userEmail, ip);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        return ResponseEntity.ok(Map.of("status", "OK", "key", key));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key,
                                       @AuthenticationPrincipal UserPrincipal user) {
        UUID userId = user != null ? user.id() : null;
        String userEmail = user != null ? user.email() : null;
        String ip = extractIp();
        service.delete(key, userId, userEmail, ip);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{key}/test")
    public ResponseEntity<Map<String, Object>> test(@PathVariable String key) {
        var result = testService.test(key);
        return ResponseEntity.ok(Map.of("ok", result.ok(), "message", result.message()));
    }

    @GetMapping("/{key}/audit")
    public ResponseEntity<?> audit(@PathVariable String key) {
        return ResponseEntity.ok(service.getAuditLog(key));
    }

    private String extractIp() {
        return java.util.Optional.ofNullable(
            ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
        ).map(a -> a.getRequest().getRemoteAddr()).orElse(null);
    }
}
