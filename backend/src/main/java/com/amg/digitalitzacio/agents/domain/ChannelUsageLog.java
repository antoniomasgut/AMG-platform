package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_usage_logs", indexes = {
        @Index(name = "idx_channel_usage_tenant_ts", columnList = "tenant_id, created_at")
})
@Getter @NoArgsConstructor
public class ChannelUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** WHATSAPP, WHATSAPP_META, TELEGRAM, EMAIL, CHAT */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public static ChannelUsageLog of(UUID tenantId, String channel) {
        var log = new ChannelUsageLog();
        log.tenantId = tenantId;
        log.channel = channel;
        return log;
    }
}
