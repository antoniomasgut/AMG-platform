package com.amg.digitalitzacio.chat.domain;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSession {

    private String id;
    private String landingSlug;
    private String landingId;
    private String tenantId;
    private String contactName;
    private String contactPhone;
    private String leadId;
    private boolean agendaEnabled;
    private int messageCount;
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant lastActivityAt = Instant.now();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
