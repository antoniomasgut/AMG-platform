package com.amg.digitalitzacio.documents.application;

import com.amg.digitalitzacio.documents.api.dto.ParsedDocumentResponse;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Puja un fitxer (PDF o text), extreu dades de contacte i opcionalment crea un lead.
     *
     * @param autoCreateLead si true, crea el lead automàticament des de les dades extretes
     */
    @PostMapping(value = "/tenants/{tenantId}/parse",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ParsedDocumentResponse> parseDocument(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoCreateLead", defaultValue = "false") boolean autoCreateLead,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var result = documentService.parseDocument(tenantId, file, autoCreateLead, principal);
        return ResponseEntity.ok(result);
    }
}
