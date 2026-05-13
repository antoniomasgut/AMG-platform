package com.amg.digitalitzacio.assets.api;

import com.amg.digitalitzacio.assets.api.dto.AssetResponse;
import com.amg.digitalitzacio.assets.application.AssetService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse upload(@RequestParam("file") MultipartFile file,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return assetService.upload(file, principal.tenantId());
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public List<AssetResponse> listByTenant(@PathVariable UUID tenantId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return assetService.listByTenant(tenantId);
    }

    @GetMapping("/{assetId}/file")
    public ResponseEntity<Resource> serveFile(@PathVariable UUID assetId) {
        var resource = assetService.serveFile(assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource);
    }

    @GetMapping("/{assetId}/thumbnail")
    public ResponseEntity<Resource> serveThumbnail(@PathVariable UUID assetId) {
        var resource = assetService.serveThumbnail(assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource);
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID assetId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        assetService.delete(assetId, principal.tenantId(), principal.role());
    }
}
