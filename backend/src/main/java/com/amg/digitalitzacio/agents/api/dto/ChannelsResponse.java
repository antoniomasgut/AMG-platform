package com.amg.digitalitzacio.agents.api.dto;

import java.util.UUID;

public record ChannelsResponse(
        UUID tenantId,
        String agentMode,
        Boolean isActive,
        Boolean telegramLinked,
        Long telegramChatId,
        String telegramBotLink,
        String whatsappPhoneNumber,
        String whatsappMetaPhoneNumberId,
        String emailAddress,
        String metaPageId
) {}
