package com.amg.digitalitzacio.vault.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunicationRespondRequest(
        String channel,
        String channelMessageId,
        String responseType,
        @NotBlank @Size(max = 5000) String text,
        String buttonId
) {}
