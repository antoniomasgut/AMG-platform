package com.amg.digitalitzacio.agents.api.dto;

import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import java.time.Instant;

public record ConversationResponse(
        Long id,
        String customerIdentifier,
        ConversationChannel channel,
        ConversationRole role,
        String content,
        Boolean pendingApproval,
        Instant createdAt
) {}
