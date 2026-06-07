package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.KnowledgeEntry;
import com.amg.digitalitzacio.agents.domain.KnowledgeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final KnowledgeEntryRepository entryRepository;
    private final EmbeddingService embeddingService;

    private static final int DEFAULT_LIMIT = 10;
    private static final double MIN_SIMILARITY = 0.3;

    public List<SearchResult> search(UUID knowledgeBaseId, String query, int limit) {
        var queryVec = embeddingService.embed(query);
        if (queryVec == null) return List.of();

        var entries = entryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(knowledgeBaseId);
        var limitVal = limit > 0 ? limit : DEFAULT_LIMIT;

        return entries.stream()
            .filter(e -> e.getIsVectorized() && e.getEmbedding() != null)
            .map(e -> {
                var entryVec = embeddingService.parseEmbedding(e.getEmbedding());
                var score = entryVec != null
                    ? embeddingService.cosineSimilarity(queryVec, entryVec)
                    : 0;
                return new SearchResult(e, score);
            })
            .filter(r -> r.score >= MIN_SIMILARITY)
            .sorted(Comparator.<SearchResult>comparingDouble(r -> r.score).reversed())
            .limit(limitVal)
            .collect(Collectors.toList());
    }

    public List<SearchResult> search(UUID knowledgeBaseId, String query) {
        return search(knowledgeBaseId, query, DEFAULT_LIMIT);
    }

    public record SearchResult(KnowledgeEntry entry, double score) {}
}
