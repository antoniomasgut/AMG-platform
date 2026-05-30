package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.KnowledgeBaseService;
import com.amg.digitalitzacio.agents.application.PdfTextExtractor;
import com.amg.digitalitzacio.agents.application.PromptBuilder;
import com.amg.digitalitzacio.agents.domain.KnowledgeCategory;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final PdfTextExtractor pdfTextExtractor;
    private final PromptBuilder promptBuilder;
    private final AIProviderRouter aiProviderRouter;

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024; // 10 MB

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

    @PostMapping(value = "/{tenantId}/documents/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> uploadDocument(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("Fitxer buit");
        if (file.getSize() > MAX_UPLOAD_BYTES) return ResponseEntity.badRequest().body("El fitxer supera el límit de 10 MB");
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("application/pdf") && !ct.equals("text/plain"))) {
            return ResponseEntity.badRequest().body("Només s'accepten PDF i TXT");
        }
        try {
            String text = pdfTextExtractor.extract(file);
            if (text.isBlank()) return ResponseEntity.badRequest().body("No s'ha pogut extreure text del fitxer");
            var response = knowledgeBaseService.addDocument(tenantId, file.getOriginalFilename(), text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processant el fitxer: " + e.getMessage());
        }
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
        return ResponseEntity.ok(promptBuilder.build(tenantId, null));
    }

    @PostMapping("/{tenantId}/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeTestResponse> testResponse(
            @PathVariable UUID tenantId,
            @RequestBody KnowledgeTestRequest request) {
        String systemPrompt = promptBuilder.build(tenantId, null);
        String response = aiProviderRouter.forModel(null).chat(systemPrompt, List.of(), request.message());
        return ResponseEntity.ok(new KnowledgeTestResponse(response, systemPrompt));
    }
}
