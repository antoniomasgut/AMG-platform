package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, UUID> {
    List<KnowledgeEntry> findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(UUID knowledgeBaseId);
    List<KnowledgeEntry> findByKnowledgeBaseIdAndCategoryAndIsActiveTrueOrderBySortOrder(UUID knowledgeBaseId, KnowledgeCategory category);
    Optional<KnowledgeEntry> findByKnowledgeBaseIdAndKey(UUID knowledgeBaseId, String key);

    @Modifying
    @Query("DELETE FROM KnowledgeEntry e WHERE e.knowledgeBaseId = :kbId AND e.category = :category")
    void deleteByKnowledgeBaseIdAndCategory(@Param("kbId") UUID knowledgeBaseId, @Param("category") KnowledgeCategory category);
}
