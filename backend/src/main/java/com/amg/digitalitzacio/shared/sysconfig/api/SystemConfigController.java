package com.amg.digitalitzacio.shared.sysconfig.api;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService.ConfigStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemConfigController {

    private final SystemConfigService service;

    @GetMapping
    public List<ConfigStatus> list() {
        return service.listStatus();
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, String>> set(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {

        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El valor no pot estar buit"));
        }

        var known = SystemConfigService.KNOWN_KEYS.stream()
                .filter(k -> k.key().equals(key))
                .findFirst();

        String description = known.map(SystemConfigService.KnownKey::description).orElse(null);
        boolean isSecret = known.map(SystemConfigService.KnownKey::secret).orElse(true);

        service.set(key, value, isSecret, description);
        return ResponseEntity.ok(Map.of("status", "OK", "key", key));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        service.delete(key);
        return ResponseEntity.noContent().build();
    }
}
