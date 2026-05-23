package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ConversationRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final KnowledgeBaseService knowledgeBaseService;

    public String build(UUID tenantId, CustomerContext context) {
        String knowledgeBlock = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        String historyBlock = buildHistoryBlock(context);

        return """
                Ets l'assistent virtual d'aquest negoci. Respons en l'idioma en el qual t'escriu el client, de forma concisa i natural.

                REGLES:
                - Si no saps alguna cosa, pregunta en lloc d'inventar
                - Confirma les dades abans de qualsevol compromís
                - En cas d'urgència o queixa greu, indica que contactin directament
                """ + knowledgeBlock + historyBlock;
    }

    private String buildHistoryBlock(CustomerContext context) {
        if (context == null) return "";

        var sb = new StringBuilder();

        if (context.summary() != null && !context.summary().isBlank()) {
            sb.append("\n\n--- RESUM DE CONVERSES ANTERIORS ---\n");
            sb.append(context.summary());
        }

        if (context.recentMessages() != null && !context.recentMessages().isEmpty()) {
            sb.append("\n\n--- CONVERSA RECENT ---\n");
            context.recentMessages().forEach(msg -> {
                String role = msg.getRole() == ConversationRole.USER ? "Client" : "Agent";
                sb.append(role).append(": ").append(msg.getContent()).append("\n");
            });
        }

        return sb.toString();
    }
}
