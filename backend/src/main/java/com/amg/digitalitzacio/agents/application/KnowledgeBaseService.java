package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.api.dto.KnowledgeBaseResponse;
import com.amg.digitalitzacio.agents.api.dto.KnowledgeDocumentResponse;
import com.amg.digitalitzacio.agents.api.dto.KnowledgeEntryRequest;
import com.amg.digitalitzacio.agents.api.dto.KnowledgeEntryResponse;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AIProviderRouter aiProviderRouter;

    @Transactional
    public KnowledgeBase findOrCreate(UUID tenantId) {
        return knowledgeBaseRepository.findByTenantId(tenantId)
            .orElseGet(() -> knowledgeBaseRepository.save(
                KnowledgeBase.builder().tenantId(tenantId).build()));
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getKnowledge(UUID tenantId) {
        var kb = findOrCreate(tenantId);
        var entries = knowledgeEntryRepository
            .findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId());
        var docs = knowledgeDocumentRepository
            .findByKnowledgeBaseIdAndIsActiveTrueOrderByUploadedAt(kb.getId());

        Map<String, List<KnowledgeEntryResponse>> byCategory = entries.stream()
            .collect(Collectors.groupingBy(
                e -> e.getCategory().name(),
                Collectors.mapping(this::toEntryResponse, Collectors.toList())
            ));

        List<KnowledgeDocumentResponse> docResponses = docs.stream()
            .map(d -> new KnowledgeDocumentResponse(d.getId(), d.getFilename(),
                d.getStoragePath(), d.getFileSize(), d.getContentType(), d.getIsProcessed(), d.getUploadedAt()))
            .toList();

        return new KnowledgeBaseResponse(kb.getId(), kb.getTenantId(), kb.getIsActive(),
            kb.getVersion(), byCategory, docResponses);
    }

    @Transactional
    public void updateEntries(UUID tenantId, KnowledgeCategory category, List<KnowledgeEntryRequest> requests) {
        var kb = findOrCreate(tenantId);
        knowledgeEntryRepository.deleteByKnowledgeBaseIdAndCategory(kb.getId(), category);

        var newEntries = requests.stream()
            .filter(r -> r.content() != null && !r.content().isBlank())
            .map(r -> KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId())
                .category(category)
                .key(r.key() != null ? r.key() : UUID.randomUUID().toString())
                .content(r.content())
                .sortOrder(r.sortOrder() != null ? r.sortOrder() : 0)
                .build())
            .toList();

        knowledgeEntryRepository.saveAll(newEntries);
        bumpVersion(kb);
        log.debug("Updated {} entries for category {} tenant {}", newEntries.size(), category, tenantId);
    }

    @Transactional
    public KnowledgeDocumentResponse addDocument(UUID tenantId, String filename, String content,
            String storagePath, Long fileSize, String contentType) {
        var kb = findOrCreate(tenantId);
        var doc = knowledgeDocumentRepository.save(
            KnowledgeDocument.builder()
                .knowledgeBaseId(kb.getId())
                .filename(filename)
                .extractedText(content)
                .storagePath(storagePath)
                .fileSize(fileSize)
                .contentType(contentType)
                .isProcessed(true)
                .build());
        bumpVersion(kb);
        return new KnowledgeDocumentResponse(doc.getId(), doc.getFilename(),
            doc.getStoragePath(), doc.getFileSize(), doc.getContentType(), doc.getIsProcessed(), doc.getUploadedAt());
    }

    @Transactional
    public KnowledgeDocumentResponse updateDocument(UUID tenantId, UUID docId, String filename, String content) {
        var kb = findOrCreate(tenantId);
        var doc = knowledgeDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (!doc.getKnowledgeBaseId().equals(kb.getId())) {
            throw new IllegalArgumentException("Document does not belong to tenant");
        }
        doc.setFilename(filename);
        doc.setExtractedText(content);
        knowledgeDocumentRepository.save(doc);
        bumpVersion(kb);
        return new KnowledgeDocumentResponse(doc.getId(), doc.getFilename(),
                doc.getStoragePath(), doc.getFileSize(), doc.getContentType(), doc.getIsProcessed(), doc.getUploadedAt());
    }

    public void deleteDocument(UUID tenantId, UUID docId) {
        var kb = findOrCreate(tenantId);
        knowledgeDocumentRepository.findById(docId).ifPresent(doc -> {
            if (!doc.getKnowledgeBaseId().equals(kb.getId())) {
                throw new IllegalArgumentException("Document does not belong to tenant");
            }
            doc.setIsActive(false);
            knowledgeDocumentRepository.save(doc);
            bumpVersion(kb);
        });
    }

    @Transactional(readOnly = true)
    public String buildKnowledgeBlock(UUID tenantId) {
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        if (kbOpt.isEmpty() || !Boolean.TRUE.equals(kbOpt.get().getIsActive())) return "";

        var kb = kbOpt.get();
        var entries = knowledgeEntryRepository
            .findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId());
        var docs = knowledgeDocumentRepository
            .findByKnowledgeBaseIdAndIsActiveTrueOrderByUploadedAt(kb.getId());

        if (entries.isEmpty() && docs.isEmpty()) return "";

        var sb = new StringBuilder();
        sb.append("\n\n--- CONEIXEMENT DEL NEGOCI ---\n");

        for (var cat : KnowledgeCategory.values()) {
            var catEntries = entries.stream()
                .filter(e -> e.getCategory() == cat)
                .toList();
            if (!catEntries.isEmpty()) {
                sb.append("\n[").append(categoryLabel(cat)).append("]\n");
                catEntries.forEach(e -> sb.append(e.getContent()).append("\n"));
            }
        }

        if (!docs.isEmpty()) {
            sb.append("\n[DOCUMENTS]\n");
            docs.forEach(d -> sb
                .append("# ").append(d.getFilename()).append("\n")
                .append(d.getExtractedText()).append("\n\n"));
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String testResponse(UUID tenantId, String userMessage) {
        String knowledgeBlock = buildKnowledgeBlock(tenantId);
        String systemPrompt = """
                Ets l'assistent virtual d'aquest negoci. Respons en l'idioma en el qual t'escriu el client, de forma concisa i natural.

                REGLES:
                - Si no saps alguna cosa, pregunta en lloc d'inventar
                - Confirma les dades abans de qualsevol compromís
                - En cas d'urgència o queixa greu, indica que contactin directament
                """ + knowledgeBlock;

        var provider = aiProviderRouter.forModel(null); // usa el model per defecte del sistema
        return provider.chat(systemPrompt, List.of(), userMessage);
    }

    private void bumpVersion(KnowledgeBase kb) {
        kb.setVersion(kb.getVersion() + 1);
        knowledgeBaseRepository.save(kb);
    }

    private KnowledgeEntryResponse toEntryResponse(KnowledgeEntry e) {
        return new KnowledgeEntryResponse(e.getId(), e.getCategory().name(), e.getKey(),
            e.getContent(), e.getSortOrder());
    }

    private String categoryLabel(KnowledgeCategory cat) {
        return switch (cat) {
            case BEHAVIOR       -> "COMPORTAMENT";
            case BUSINESS_INFO  -> "INFORMACIÓ DEL NEGOCI";
            case SCHEDULE       -> "HORARIS";
            case SERVICE        -> "SERVEIS";
            case FAQ            -> "PREGUNTES FREQÜENTS";
            case RESTRICTION    -> "RESTRICCIONS";
            case EXTRA          -> "INFORMACIÓ ADDICIONAL";
            case BOOKING_RULES  -> "REGLES DE CITES";
            case QUOTE_RULES    -> "REGLES DE PRESSUPOSTOS";
            case FOLLOWUP_RULES -> "REGLES DE SEGUIMENT";
            case TEAM_INFO      -> "INFORMACIÓ D'EQUIP";
        };
    }
}
