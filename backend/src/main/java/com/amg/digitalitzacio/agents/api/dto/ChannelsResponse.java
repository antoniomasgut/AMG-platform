package com.amg.digitalitzacio.agents.api.dto;

import java.util.UUID;

public record ChannelsResponse(
        UUID tenantId,
        String agentMode,
        Boolean isActive,
        Boolean telegramLinked,
        Long telegramChatId,
        String whatsappPhoneNumber,
        String whatsappMetaPhoneNumberId
) {}
