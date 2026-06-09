package com.amg.digitalitzacio.agents.api.dto;

public record UpdateChannelsRequest(
        String agentMode,
        Boolean isActive,
        Boolean widgetEnabled,
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        String whatsappPhoneNumber,
        String whatsappMetaPhoneNumberId,
        String emailAddress,
        String metaPageId
) {}
