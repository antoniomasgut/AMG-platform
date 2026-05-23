package com.amg.digitalitzacio.agents.api.dto;

public record UpdateChannelsRequest(
        String agentMode,
        Boolean isActive,
        String whatsappPhoneNumber,
        String whatsappMetaPhoneNumberId
) {}
