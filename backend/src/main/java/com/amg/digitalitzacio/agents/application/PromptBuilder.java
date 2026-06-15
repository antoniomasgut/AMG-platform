package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.PhaseDepType;
import com.amg.digitalitzacio.auth.domain.SectorPhase;
import com.amg.digitalitzacio.auth.domain.SectorPhaseRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
        "ca", "català",
        "es", "espanyol (castellà)",
        "en", "anglès",
        "de", "alemany"
    );

    private final KnowledgeBaseService knowledgeBaseService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final TenantRepository tenantRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final SectorPhaseRepository sectorPhaseRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String build(UUID tenantId, CustomerContext context) {
        String responseLanguage = resolveResponseLanguage(tenantId);
        String languageRule     = buildLanguageRule(responseLanguage);
        String basePrompt       = resolveTenantSystemPrompt(tenantId);
        String knowledgeBlock   = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        String nexeBlock        = buildNexeBlock(tenantId);
        String catalogBlock     = buildSectorCatalogBlock(tenantId);
        String historyBlock     = buildHistoryBlock(context);

        return languageRule
            + "<business_context>\n" + basePrompt + knowledgeBlock + "\n</business_context>\n"
            + "<services>\n" + nexeBlock + "\n</services>\n"
            + (catalogBlock.isBlank() ? "" : "<sector_catalog>\n" + catalogBlock + "\n</sector_catalog>\n")
            + "<history>\n" + historyBlock + "\n</history>";
    }

    private String resolveResponseLanguage(UUID tenantId) {
        return tenantAIConfigRepository.findByTenantId(tenantId)
            .map(c -> c.getResponseLanguage())
            .filter(l -> l != null && !l.isBlank() && !l.equals("auto"))
            .orElse(null);
    }

    private String buildLanguageRule(String responseLanguage) {
        if (responseLanguage != null) {
            String langName = LANGUAGE_NAMES.getOrDefault(responseLanguage, responseLanguage);
            return String.format("""
                <language_rule>
                IMPORTANT: Respon SEMPRE en %s (%s), independentment de l'idioma en el qual escrigui el client.
                El context intern (<business_context>, <services>) està en català per organització; no influeix en l'idioma de resposta.
                </language_rule>
                %n""", langName, responseLanguage);
        }
        return """
            <language_rule>
            Detecta l'idioma en el qual escriu el client i respon SEMPRE en aquell idioma.
            El context intern (<business_context>, <services>) està en català per organització; no influeix en l'idioma de resposta.
            </language_rule>
            %n""".formatted();
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

        appendIfEnabled(sb, configs.get("AGENDA"),       this::buildAgendaBlock);
        appendIfEnabled(sb, configs.get("PRESSUPOSTOS"), this::buildPressupostosBlock);
        appendIfEnabled(sb, configs.get("FIDELITZACIO"), this::buildFidelitzacioBlock);
        appendIfEnabled(sb, configs.get("EQUIP"),        this::buildEquipBlock);
        appendIfEnabled(sb, configs.get("RAG"),          this::buildRagBlock);
        appendIfEnabled(sb, configs.get("HORARI"),       this::buildHorariBlock);

        return sb.toString();
    }

    public String buildAgendaBlock(String json) {
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

    // Injecta el catàleg complet de fases sectorials quan el tenant és una agència IA (agent de vendes)
    private String buildSectorCatalogBlock(UUID tenantId) {
        var sector = tenantRepository.findById(tenantId)
                .map(t -> t.getSector())
                .orElse(null);
        if (sector != BusinessSector.AGENCIA_IA) return "";

        var allPhases = sectorPhaseRepository.findAll();
        if (allPhases.isEmpty()) return "";

        var bySector = new java.util.LinkedHashMap<BusinessSector, List<SectorPhase>>();
        for (var phase : allPhases) {
            bySector.computeIfAbsent(phase.getSector(), k -> new java.util.ArrayList<>()).add(phase);
        }
        bySector.values().forEach(list -> list.sort(java.util.Comparator.comparingInt(SectorPhase::getPhaseNumber)));

        var sb = new StringBuilder();
        sb.append("Catàleg complet de fases per sector. Quan un prospecte explica el seu negoci, presenta les fases del seu sector.\n\n");

        for (var entry : bySector.entrySet()) {
            if (entry.getKey() == BusinessSector.AGENCIA_IA) continue;
            sb.append("SECTOR: ").append(sectorLabel(entry.getKey())).append("\n");
            for (var p : entry.getValue()) {
                String req = (p.getRequiredPhases() != null && !p.getRequiredPhases().isBlank())
                        ? " [req fase " + p.getRequiredPhases() + "]" : "";
                String dep = p.getDependencyType() == PhaseDepType.BASE ? " ★BASE" : "";
                sb.append("  F").append(p.getPhaseNumber()).append(dep).append(req)
                  .append(" — ").append(p.getName()).append(": ").append(p.getDescription())
                  .append(" (setup ").append(p.getSetupPrice().intValue()).append("€")
                  .append(", ").append(p.getMonthlyPrice().intValue()).append("€/mes)\n");
            }
            sb.append("  → Recomanació per estalviar temps: ").append(firstRecommendation(entry.getKey())).append("\n\n");
        }

        return sb.toString();
    }

    private static String sectorLabel(BusinessSector s) {
        return switch (s) {
            case PINTOR -> "Pintura";
            case ELECTRICISTA -> "Electricitat";
            case FONTANER -> "Lampisteria";
            case JARDINER -> "Jardineria";
            case NETEJA -> "Neteja";
            case TALLER_MECANIC -> "Taller mecànic";
            case FISIOTERAPEUTA -> "Fisioteràpia";
            case PSICOLEG -> "Psicologia";
            case NUTRICIONISTA -> "Nutrició";
            case RESTAURANTE -> "Restaurant";
            case ACADEMIA -> "Acadèmia / Centre de formació";
            case VETERINARI -> "Clínica veterinària";
            case PERRUQUERIA_CANINA -> "Perruqueria canina";
            case PERRUQUERIA -> "Perruqueria";
            case ESTETICA -> "Centre d'estètica";
            case GESTORIA -> "Gestoria / Assessoria";
            case INMOBILIARIA -> "Immobiliària";
            default -> s.name();
        };
    }

    private static String firstRecommendation(BusinessSector s) {
        return switch (s) {
            case PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA ->
                "F1+F2+F3 — generació i seguiment de pressupostos sense intervenció manual";
            case TALLER_MECANIC ->
                "F1+F2+F3 — pressupostos automàtics i agenda de deixada de vehicle";
            case FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA ->
                "F1+F2+F3 — agenda, historial de pacient i notes de sessió per veu";
            case RESTAURANTE ->
                "F1+F2 — reserves automàtiques i eliminació de no-shows";
            case ACADEMIA ->
                "F1+F2 — captació i matrícula sense trucades";
            case VETERINARI, PERRUQUERIA_CANINA ->
                "F1+F2 — agenda i historial de la mascota";
            case PERRUQUERIA, ESTETICA ->
                "F1+F2 — reserves i recordatoris, reducció de no-shows fins al 60%";
            case GESTORIA ->
                "F1+F2+F3 — captació, alta de client i recordatoris de terminis fiscals";
            case INMOBILIARIA ->
                "F1+F2+F3 — captació de propietats, cerca intel·ligent i agenda de visites coordinada";
            default -> "F1 — punt d'entrada per al sector";
        };
    }

    private void appendIfEnabled(StringBuilder sb, String json, Function<String, String> builder) {
        if (isEnabled(json)) sb.append(builder.apply(json));
    }

    private boolean isEnabled(String json) {
        if (json == null) return false;
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            return Boolean.TRUE.equals(c.get("enabled"));
        } catch (Exception e) {
            return false;
        }
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null && !v.toString().isBlank() ? v.toString() : def;
    }

    private String buildRagBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nBASE DE CONEIXEMENT (RAG):\n");
            sb.append("- Utilitza la base de coneixement del negoci per respondre preguntes sobre serveis, horaris, preus i informació del negoci\n");

            String customInstructions = str(c, "customInstructions", "");
            if (!customInstructions.isBlank()) {
                sb.append("- Instruccions addicionals: ").append(customInstructions).append("\n");
            }

            String sourcePriority = str(c, "sourcePriority", "");
            if (!sourcePriority.isBlank()) {
                sb.append("- Prioritza les fonts en aquest ordre: ").append(sourcePriority).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse RAG config: {}", e.getMessage());
            return "";
        }
    }

    private String buildHorariBlock(String json) {
        try {
            Map<String, Object> c = objectMapper.readValue(json, new TypeReference<>() {});
            var sb = new StringBuilder("\nFORA D'HORARI:\n");
            String message = str(c, "outOfHoursMessage", "");
            if (!message.isBlank()) {
                sb.append("- Missatge per a clients fora d'horari: \"").append(message).append("\"\n");
            }
            sb.append("- Informa els clients sobre l'horari del negoci i quan podran rebre resposta\n");
            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse HORARI config: {}", e.getMessage());
            return "";
        }
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
