package com.amg.digitalitzacio.agents.api.dto;

import java.util.UUID;

public record ChannelsResponse(
        UUID tenantId,
        String agentMode,
        String emailMode,
        String whatsappMode,
        String widgetMode,
        Boolean isActive,
        Boolean widgetEnabled,
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        Boolean telegramLinked,
        Long telegramChatId,
        String telegramBotLink,
        String whatsappPhoneNumber,
        String whatsappMetaPhoneNumberId,
        String emailAddress,
        String metaPageId
) {}
