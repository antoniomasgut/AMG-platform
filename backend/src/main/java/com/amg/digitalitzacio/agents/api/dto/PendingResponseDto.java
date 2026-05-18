package com.amg.digitalitzacio.agents.api.dto;

import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import java.time.Instant;

public record PendingResponseDto(
        Long id,
        String customerIdentifier,
        ConversationChannel channel,
        String customerMessage,
        String suggestedResponse,
        Instant createdAt
) {}
