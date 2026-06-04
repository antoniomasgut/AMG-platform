package com.amg.digitalitzacio.widget.domain;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WidgetSession {

    private String id;
    private String siteId;
    private String tenantId;
    private int messageCount;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant lastActivityAt = Instant.now();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
