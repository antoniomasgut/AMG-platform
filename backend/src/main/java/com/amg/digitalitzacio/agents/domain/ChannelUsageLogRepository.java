package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ChannelUsageLogRepository extends JpaRepository<ChannelUsageLog, Long> {

    @Query("SELECT COUNT(l) FROM ChannelUsageLog l WHERE l.tenantId = ?1 AND l.channel = ?2 AND l.createdAt BETWEEN ?3 AND ?4")
    long countByTenantAndChannel(UUID tenantId, String channel, Instant from, Instant to);

    @Query("SELECT COUNT(l) FROM ChannelUsageLog l WHERE l.channel = ?1 AND l.createdAt BETWEEN ?2 AND ?3")
    long countByChannel(String channel, Instant from, Instant to);
}
