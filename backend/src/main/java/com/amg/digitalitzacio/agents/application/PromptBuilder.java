package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final KnowledgeBaseService knowledgeBaseService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public String build(UUID tenantId, CustomerContext context) {
        String basePrompt = resolveTenantSystemPrompt(tenantId);
        String knowledgeBlock = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        String languageOverride = extractLanguageInstruction(knowledgeBlock);
        String nexeBlock = buildNexeBlock(tenantId);
        String historyBlock = buildHistoryBlock(context);

        String prompt = basePrompt + knowledgeBlock + nexeBlock + historyBlock;
        if (!languageOverride.isBlank()) {
            prompt = prompt + "\n\nREGLA D'IDIOMA (màxima prioritat): " + languageOverride;
        }
        return prompt;
    }

    private String extractLanguageInstruction(String knowledgeBlock) {
        if (knowledgeBlock == null || knowledgeBlock.isBlank()) return "";
        for (String line : knowledgeBlock.split("\n")) {
            if (line.startsWith("Instrucció d'idioma:")) {
                return line.substring("Instrucció d'idioma:".length()).trim();
            }
        }
        return "";
    }

    private String resolveTenantSystemPrompt(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getAgentSystemPrompt())
                .filter(s -> s != null && !s.isBlank())
                .orElse("""
                        Ets l'assistent virtual d'aquest negoci. Respons en l'idioma en el qual t'escriu el client, de forma concisa i natural.

                        REGLES:
                        - Si no saps alguna cosa, pregunta en lloc d'inventar
                        - Confirma les dades abans de qualsevol compromís
                        - En cas d'urgència o queixa greu, indica que contactin directament
                        """);
    }

    private String buildNexeBlock(UUID tenantId) {
        var configs = nexeServiceConfigService.getAllAsMap(tenantId);
        if (configs.isEmpty()) return "";

        var sb = new StringBuilder("\n\n--- CONFIGURACIÓ DE SERVEIS ---\n");

        String agendaJson = configs.get("AGENDA");
        if (agendaJson != null) {
            sb.append(buildAgendaBlock(agendaJson));
        }

        String pressupostosJson = configs.get("PRESSUPOSTOS");
        if (pressupostosJson != null) {
            sb.append(buildPressupostosBlock(pressupostosJson));
        }

        String fidelitzacioJson = configs.get("FIDELITZACIO");
        if (fidelitzacioJson != null) {
            sb.append(buildFidelitzacioBlock(fidelitzacioJson));
        }

        String equipJson = configs.get("EQUIP");
        if (equipJson != null) {
            sb.append(buildEquipBlock(equipJson));
        }

        return sb.toString();
    }

    private String buildAgendaBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nCITES / AGENDA:\n");
            String mode = str(c, "mode", "appointment");
            switch (mode) {
                case "inspection" -> sb.append("- Gestiones visites d'inspecció al domicili del client (no cites fixes)\n");
                case "vehicle"    -> sb.append("- Gestiones deixades de vehicle al taller\n");
                case "meeting"    -> sb.append("- Gestiones reunions professionals\n");
                default           -> sb.append("- Gestiones cites de ").append(str(c, "slotDuration", "60")).append(" minuts\n");
            }
            Object questions = c.get("clientQuestions");
            if (questions instanceof java.util.List<?> qList && !qList.isEmpty()) {
                sb.append("- Preguntes obligatòries al client: ").append(String.join(", ", qList.stream().map(Object::toString).toList())).append("\n");
            }
            String confTpl = str(c, "confirmationTemplate", "");
            if (!confTpl.isBlank()) {
                sb.append("- Plantilla de confirmació: ").append(confTpl).append("\n");
            }
            // Instrucció per a la integració amb Google Calendar
            String calType = str(c, "calendar_type", "manual");
            String calId   = str(c, "google_calendar_id", "");
            if ("google".equals(calType) && !calId.isBlank()) {
                sb.append("- Integració Google Calendar activa. Quan confirmes una cita amb data i hora específiques,");
                sb.append(" afegeix EXACTAMENT al final del teu missatge (invisible per l'usuari, el sistema ho elimina):\n");
                sb.append("[CONFIRMA_CITA:{\"date\":\"YYYY-MM-DD\",\"time\":\"HH:MM\",\"duration\":60,\"name\":\"NOM_CLIENT\",\"notes\":\"NOTES\"}]\n");
                sb.append("Usa format 24h per a l'hora i ISO 8601 per a la data. No inventis la data si el client no l'ha dit.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse AGENDA config: {}", e.getMessage());
            return "";
        }
    }

    private String buildPressupostosBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nPRESSUPOSTOS / PREUS:\n");
            String mode = str(c, "mode", "formal");
            if ("pricelist".equals(mode)) {
                sb.append("- Presentes un llistat de serveis i preus (no pressupostos PDF formals)\n");
            } else {
                sb.append("- Generes pressupostos formals amb validesa de ").append(str(c, "validityDays", "30")).append(" dies\n");
            }
            Object catalog = c.get("catalogItems");
            if (catalog instanceof java.util.List<?> items && !items.isEmpty()) {
                sb.append("- Serveis disponibles:\n");
                items.stream().limit(10).forEach(item -> {
                    if (item instanceof Map<?, ?> m) {
                        sb.append("  • ").append(m.get("name")).append(": ").append(m.get("unitPrice")).append("€");
                        if (m.get("unit") != null) sb.append("/").append(m.get("unit"));
                        sb.append("\n");
                    }
                });
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse PRESSUPOSTOS config: {}", e.getMessage());
            return "";
        }
    }

    private String buildFidelitzacioBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nFIDELITZACIÓ:\n");
            String reviewsUrl = str(c, "googleReviewsUrl", "");
            if (!reviewsUrl.isBlank()) {
                sb.append("- Quan un client estigui satisfet, convida'l a deixar una ressenya: ").append(reviewsUrl).append("\n");
            }
            String followUpTpl = str(c, "followUpTemplate", "");
            if (!followUpTpl.isBlank()) {
                sb.append("- Plantilla de seguiment post-servei: ").append(followUpTpl).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse FIDELITZACIO config: {}", e.getMessage());
            return "";
        }
    }

    private String buildEquipBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nEQUIP:\n");

            Object members = c.get("members");
            if (members instanceof java.util.List<?> list && !list.isEmpty()) {
                sb.append("- Membres de l'equip:\n");
                list.forEach(m -> {
                    if (m instanceof Map<?, ?> member) {
                        String name = member.get("name") != null ? member.get("name").toString() : "";
                        String role = member.get("role") != null ? member.get("role").toString() : "";
                        if (!name.isBlank()) {
                            sb.append("  • ").append(name);
                            if (!role.isBlank()) sb.append(" (").append(role).append(")");
                            sb.append("\n");
                        }
                    }
                });
            }

            String groupName = str(c, "telegram_group_name", "");
            if (!groupName.isBlank()) {
                sb.append("- Grup de treball intern: ").append(groupName).append("\n");
            }

            Object dailyEnabled = c.get("daily_report_enabled");
            if (Boolean.TRUE.equals(dailyEnabled)) {
                sb.append("- Informe diari enviat a les ").append(str(c, "daily_report_time", "18:00")).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse EQUIP config: {}", e.getMessage());
            return "";
        }
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null && !v.toString().isBlank() ? v.toString() : def;
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
