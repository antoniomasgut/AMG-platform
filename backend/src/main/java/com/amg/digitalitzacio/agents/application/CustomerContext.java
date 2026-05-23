package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.Conversation;

import java.util.List;

public record CustomerContext(
    List<Conversation> recentMessages,
    String summary,
    long totalMessageCount
) {}
