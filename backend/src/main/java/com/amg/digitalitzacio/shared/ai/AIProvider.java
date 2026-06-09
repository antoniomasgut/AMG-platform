package com.amg.digitalitzacio.shared.ai;

import java.util.List;
import java.util.function.Function;

public interface AIProvider {
    String chat(String systemPrompt, List<ChatMessage> history, String userMessage);
    String providerName();

    /**
     * Conversa amb suport de function calling. Per defecte delega a chat() sense eines.
     * Els providers que suporten tool_use han de sobreescriure aquest mètode.
     */
    default String chatWithTools(String systemPrompt, List<ChatMessage> history, String userMessage,
                                  List<ToolDefinition> tools, Function<ToolCall, String> toolExecutor) {
        return chat(systemPrompt, history, userMessage);
    }

    record AIResponse(String text, int inputTokens, int outputTokens) {
        public int totalTokens() { return inputTokens + outputTokens; }
    }
}
