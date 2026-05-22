package com.amg.digitalitzacio.shared.ai;

import java.util.List;

public interface AIProvider {
    String chat(String systemPrompt, List<ChatMessage> history, String userMessage);
    String providerName();
}
