package com.amg.digitalitzacio.agents.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContactSummaryResponse(
    UUID contactId,
    String displayName,
    String phone,
    String email,
    List<ChannelInfo> channels,
    String lastMessage,
    String lastMessageRole,
    Instant lastMessageAt,
    String lastChannel,
    String lastIdentifier,
    long pendingCount
) {
    public record ChannelInfo(String channel, String identifier) {}
}
