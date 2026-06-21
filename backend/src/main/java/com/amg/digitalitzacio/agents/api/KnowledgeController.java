package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.*;
import com.amg.digitalitzacio.agents.domain.KnowledgeBaseRepository;
import com.amg.digitalitzacio.agents.domain.KnowledgeCategory;
import com.amg.digitalitzacio.agents.domain.KnowledgeEntryRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final PdfTextExtractor pdfTextExtractor;
    private final PromptBuilder promptBuilder;
    private final AIProviderRouter aiProviderRouter;
    private final VectorSearchService vectorSearchService;
    private final EmbeddingService embeddingService;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

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
            @Valid @RequestBody UpdateEntriesRequest request) {
        var cat = KnowledgeCategory.valueOf(category.toUpperCase());
        knowledgeBaseService.updateEntries(tenantId, cat, request.entries());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeDocumentResponse> addDocument(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AddDocumentRequest request) {
        var response = knowledgeBaseService.addDocument(tenantId, request.filename(), request.content(),
            null, null, "text/plain");
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
            var response = knowledgeBaseService.addDocument(tenantId, file.getOriginalFilename(), text,
                null, file.getSize(), file.getContentType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processant el fitxer: " + e.getMessage());
        }
    }

    @PutMapping("/{tenantId}/documents/{docId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeDocumentResponse> updateDocument(
            @PathVariable UUID tenantId,
            @PathVariable UUID docId,
            @RequestBody AddDocumentRequest request) {
        var response = knowledgeBaseService.updateDocument(tenantId, docId, request.filename(), request.content());
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
        return ResponseEntity.ok(promptBuilder.build(tenantId, null));
    }

    @PostMapping("/{tenantId}/vectorize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> vectorize(@PathVariable UUID tenantId) {
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        if (kbOpt.isEmpty()) return ResponseEntity.ok(Map.of("error", "No knowledge base found"));

        var entries = knowledgeEntryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kbOpt.get().getId());
        if (entries.isEmpty()) return ResponseEntity.ok(Map.of("vectorized", 0, "failed", 0, "total", 0));

        int ok = 0, fail = 0;
        var toSave = new java.util.ArrayList<com.amg.digitalitzacio.agents.domain.KnowledgeEntry>();
        int chunkSize = 500;
        for (int start = 0; start < entries.size(); start += chunkSize) {
            var chunk = entries.subList(start, Math.min(start + chunkSize, entries.size()));
            var texts = chunk.stream().map(e -> e.getContent()).toList();
            var vectors = embeddingService.embedBatchToJson(texts);
            for (int i = 0; i < chunk.size(); i++) {
                var vec = i < vectors.size() ? vectors.get(i) : null;
                if (vec != null) {
                    chunk.get(i).setEmbedding(vec);
                    chunk.get(i).setIsVectorized(true);
                    toSave.add(chunk.get(i));
                    ok++;
                } else {
                    fail++;
                }
            }
        }
        knowledgeEntryRepository.saveAll(toSave);
        return ResponseEntity.ok(Map.of("vectorized", ok, "failed", fail, "total", entries.size()));
    }

    @PostMapping("/{tenantId}/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> search(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> body) {
        var query = body.getOrDefault("query", "");
        var limit = Math.min(Integer.parseInt(body.getOrDefault("limit", "10")), 50);
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        if (kbOpt.isEmpty()) return ResponseEntity.ok(List.of());
        var results = vectorSearchService.search(kbOpt.get().getId(), query, limit);
        var response = results.stream()
            .map(r -> Map.<String, Object>of(
                "entryKey", r.entry().getKey(),
                "category", r.entry().getCategory().name(),
                "content", r.entry().getContent(),
                "score", Math.round(r.score() * 1000) / 1000.0
            ))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{tenantId}/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KnowledgeTestResponse> testResponse(
            @PathVariable UUID tenantId,
            @Valid @RequestBody KnowledgeTestRequest request) {
        String systemPrompt = promptBuilder.build(tenantId, null);
        String response = aiProviderRouter.forModel(null).chat(systemPrompt, List.of(), request.message());
        return ResponseEntity.ok(new KnowledgeTestResponse(response, systemPrompt));
    }
}
