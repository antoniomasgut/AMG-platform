package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_usage_logs", indexes = {
        @Index(name = "idx_token_usage_tenant_ts", columnList = "tenant_id, created_at")
})
@Getter @Setter @NoArgsConstructor
public class TokenUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType; // CHAT, REASONING, TEST

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public static TokenUsageLog of(UUID tenantId, String model, String taskType, int input, int output) {
        var log = new TokenUsageLog();
        log.tenantId = tenantId;
        log.model = model;
        log.taskType = taskType;
        log.inputTokens = input;
        log.outputTokens = output;
        return log;
    }
}
