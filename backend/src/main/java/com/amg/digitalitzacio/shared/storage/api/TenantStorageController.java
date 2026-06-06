package com.amg.digitalitzacio.shared.storage.api;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.storage.api.dto.ProviderConfigRequest;
import com.amg.digitalitzacio.shared.storage.api.dto.ProviderConfigResponse;
import com.amg.digitalitzacio.shared.storage.api.dto.StorageStatusResponse;
import com.amg.digitalitzacio.shared.storage.application.TenantStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/storage")
@RequiredArgsConstructor
public class TenantStorageController {

    private final TenantStorageService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<ProviderConfigResponse>> listConfigs(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.listConfigs(tenantId));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<StorageStatusResponse> getStatus(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.getStatus(tenantId));
    }

    @GetMapping("/{providerKey}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ProviderConfigResponse> getConfig(
            @PathVariable UUID tenantId, @PathVariable String providerKey) {
        return ResponseEntity.ok(service.getConfig(tenantId, providerKey));
    }

    @PutMapping("/{providerKey}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ProviderConfigResponse> saveConfig(
            @PathVariable UUID tenantId, @PathVariable String providerKey,
            @RequestBody ProviderConfigRequest req) {
        var actual = new ProviderConfigRequest(providerKey, req.config(), req.active());
        return ResponseEntity.ok(service.saveConfig(tenantId, actual));
    }

    @DeleteMapping("/{providerKey}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable UUID tenantId, @PathVariable String providerKey) {
        service.deleteConfig(tenantId, providerKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{providerKey}/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> testConnection(
            @PathVariable UUID tenantId, @PathVariable String providerKey) {
        return ResponseEntity.ok(service.testConnection(tenantId, providerKey));
    }
}
