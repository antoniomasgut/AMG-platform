package com.amg.digitalitzacio.shared.ai;

import java.util.List;

public interface AIProvider {
    String chat(String systemPrompt, List<ChatMessage> history, String userMessage);
    String providerName();

    record AIResponse(String text, int inputTokens, int outputTokens) {
        public int totalTokens() { return inputTokens + outputTokens; }
    }
}
