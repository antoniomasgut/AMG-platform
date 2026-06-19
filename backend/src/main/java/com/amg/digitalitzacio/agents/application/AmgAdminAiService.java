package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent IA per al xat admin d'AMG — respon preguntes en llenguatge natural
 * consultant les dades de la plataforma via tool calling (Claude Haiku).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmgAdminAiService {

    private final AIProviderRouter aiRouter;
    private final AmgAdminCommandService commands;
    private final TelegramBotClient telegramBotClient;

    private static final String MODEL = "claude-haiku-4-5-20251001";

    private static final String SYSTEM = """
            Ets l'assistent intern de AMG Digitalització, una agència de digitalització per a negocis locals a Mallorca.
            Tens accés a les dades de la plataforma (leads, pressupostos, clients, pipeline de vendes) via les eines disponibles.

            Respon sempre en català. Respostes curtes i directes (és un xat de Telegram).
            Pots usar format HTML de Telegram: <b>negreta</b>, <i>cursiva</i>, <code>codi</code>.
            Si la pregunta no té relació amb la plataforma, respon amablement que no pots ajudar amb això.
            No inventes dades — usa sempre les eines per consultar informació real.
            """;

    private static final List<ToolDefinition> TOOLS = List.of(
        new ToolDefinition("get_stats",
            "Retorna estadístiques generals: total leads, leads nous, pressupostos pendents, clients actius, setups pendents.",
            Map.of("type", "object", "properties", Map.of(), "required", List.of())),

        new ToolDefinition("get_pipeline",
            "Retorna el resum del kanban de vendes: nombre de leads per etapa (NEW, CONTACTED, QUALIFIED, PROPOSAL, NEGOTIATION, WON, NURTURING, LOST).",
            Map.of("type", "object", "properties", Map.of(), "required", List.of())),

        new ToolDefinition("get_new_leads",
            "Retorna els leads en estat Nou i Contactat (fins a 10). Inclou nom, contacte i etapa.",
            Map.of("type", "object", "properties", Map.of(), "required", List.of())),

        new ToolDefinition("get_recent_leads",
            "Retorna els últims leads registrats (fins a 10). Inclou nom, contacte i etapa.",
            Map.of("type", "object", "properties", Map.of(), "required", List.of())),

        new ToolDefinition("get_pending_budgets",
            "Retorna els pressupostos enviats als clients que encara no han respost (estat SENT).",
            Map.of("type", "object", "properties", Map.of(), "required", List.of())),

        new ToolDefinition("get_active_tenants",
            "Retorna la llista de clients actius (amb fases contractades actives).",
            Map.of("type", "object", "properties", Map.of(), "required", List.of()))
    );

    @Async
    public void handleAsync(Long chatId, String userMessage) {
        try {
            var provider = aiRouter.forModel(MODEL);
            String response = provider.chatWithTools(SYSTEM, List.of(), userMessage, TOOLS, toolCall -> {
                log.debug("Admin AI tool call: {}", toolCall.name());
                return switch (toolCall.name()) {
                    case "get_stats"           -> commands.buildStats();
                    case "get_pipeline"        -> commands.buildPipeline();
                    case "get_new_leads"       -> commands.buildNewLeads();
                    case "get_recent_leads"    -> commands.buildRecentLeads();
                    case "get_pending_budgets" -> commands.buildPendingBudgets();
                    case "get_active_tenants"  -> commands.buildActiveTenants();
                    default -> "Eina desconeguda: " + toolCall.name();
                };
            });

            if (response != null && !response.isBlank()) {
                telegramBotClient.sendMessage(chatId, response);
            }
        } catch (Exception e) {
            log.error("AmgAdminAiService error per chat {}: {}", chatId, e.getMessage());
            telegramBotClient.sendMessage(chatId,
                    "❌ No s'ha pogut processar la pregunta. Comprova que <code>ANTHROPIC_API_KEY</code> està configurada.");
        }
    }
}
