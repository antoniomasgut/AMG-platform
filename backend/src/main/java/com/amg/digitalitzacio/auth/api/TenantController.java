package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.application.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        var response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<TenantResponse>> listTenants(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isFree) {
        // Native query with explicit ORDER BY — force unsorted pageable to avoid camelCase column translation
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        return ResponseEntity.ok(tenantService.listTenants(unsorted, search, isActive, isFree));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and authentication.principal.tenantId == #id)")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getTenant(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @GetMapping("/{id}/delete-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantService.DeleteTenantCheckResponse> checkDeletion(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.checkDeletion(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
    }
}
