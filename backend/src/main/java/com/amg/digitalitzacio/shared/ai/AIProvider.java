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

    /**
     * Com chatWithTools però retorna AIResponse amb el recompte de tokens.
     * Per defecte envoltchat sense comptabilitzar (tokens = 0).
     * Els providers que retornen usage han de sobreescriure aquest mètode.
     */
    default AIResponse chatWithToolsTracked(String systemPrompt, List<ChatMessage> history, String userMessage,
                                             List<ToolDefinition> tools, Function<ToolCall, String> toolExecutor) {
        String text = chatWithTools(systemPrompt, history, userMessage, tools, toolExecutor);
        return new AIResponse(text, 0, 0);
    }

    record AIResponse(String text, int inputTokens, int outputTokens) {
        public int totalTokens() { return inputTokens + outputTokens; }
    }
}
