package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.KnowledgeBaseService;
import com.amg.digitalitzacio.agents.domain.KnowledgeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeBaseResponse> getKnowledge(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(knowledgeBaseService.getKnowledge(tenantId));
    }

    @PutMapping("/{tenantId}/entries/{category}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> updateEntries(
            @PathVariable UUID tenantId,
            @PathVariable String category,
            @RequestBody UpdateEntriesRequest request) {
        var cat = KnowledgeCategory.valueOf(category.toUpperCase());
        knowledgeBaseService.updateEntries(tenantId, cat, request.entries());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeDocumentResponse> addDocument(
            @PathVariable UUID tenantId,
            @RequestBody AddDocumentRequest request) {
        var response = knowledgeBaseService.addDocument(tenantId, request.filename(), request.content());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}/documents/{docId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID tenantId,
            @PathVariable UUID docId) {
        knowledgeBaseService.deleteDocument(tenantId, docId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> previewPromptBlock(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(knowledgeBaseService.buildKnowledgeBlock(tenantId));
    }
}
