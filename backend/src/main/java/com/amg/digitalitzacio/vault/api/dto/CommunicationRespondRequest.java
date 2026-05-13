package com.amg.digitalitzacio.vault.api.dto;

public record CommunicationRespondRequest(
        String channel,
        String channelMessageId,
        String responseType,
        String text,
        String buttonId
) {}
