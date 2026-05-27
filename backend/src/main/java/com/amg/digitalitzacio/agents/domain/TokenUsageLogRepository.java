package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, Long> {

    @Query("SELECT COALESCE(SUM(t.inputTokens + t.outputTokens), 0) FROM TokenUsageLog t WHERE t.tenantId = ?1 AND t.createdAt >= ?2")
    long sumTokensSince(UUID tenantId, Instant since);

    @Query("SELECT COALESCE(SUM(t.inputTokens + t.outputTokens), 0) FROM TokenUsageLog t WHERE t.createdAt >= ?1")
    long sumAllTokensSince(Instant since);
}
