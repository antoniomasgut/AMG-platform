package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.content.domain.ContentPillar;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Genera briefs de contingut adaptats al sector (Spec 58 §6.2), amb IA.
 * Una sola crida retorna un brief (què fotografiar) + exemple per a cada pilar.
 * Si la IA falla, es retorna un mapa buit i el servei usa les plantilles per defecte.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentBriefGenerator {

    private static final String MODEL = "claude-haiku-4-5-20251001";

    private final AIProviderRouter aiRouter;
    private final ObjectMapper objectMapper;

    /** brief = què ha de fotografiar el tenant; example = frase aclaridora. */
    public record Brief(String brief, String example) {}

    /**
     * Genera els briefs per als 4 pilars, adaptats al negoci i sector, en l'idioma indicat
     * (per defecte català; és l'idioma de la INSTRUCCIÓ al tenant, no el de la publicació).
     */
    public Map<ContentPillar, Brief> generate(String businessName, String sector, String briefLanguage) {
        Map<ContentPillar, Brief> result = new EnumMap<>(ContentPillar.class);
        String lang = (briefLanguage == null || briefLanguage.isBlank()) ? "ca" : briefLanguage;

        String systemPrompt = """
                Ets un estrateg de xarxes socials per a negocis locals a Mallorca.
                Per a cada pilar de contingut, escriu una instrucció BREU i concreta al propietari
                sobre QUÈ ha de fotografiar aquesta setmana, adaptada al seu sector.
                Pilars:
                - NOVELTY: una novetat o producte/servei nou
                - COMBINE: com usar o combinar el producte/servei
                - SHOP: el local, l'ambient o la persona del negoci
                - SOCIAL_PROOF: un client content (amb permís) o una selecció de temporada
                Retorna NOMÉS un array JSON, sense text addicional, amb objectes:
                {"pillar":"NOVELTY|COMBINE|SHOP|SOCIAL_PROOF","brief":"...","example":"..."}
                'brief' = què fotografiar (1 frase). 'example' = exemple concret (1 frase curta).
                Idioma de la resposta: %s.
                """.formatted(lang);
        String userPrompt = "Negoci: " + businessName + "\nSector: " + sector + "\n\nGenera els 4 briefs:";

        try {
            var provider = aiRouter.forModel(MODEL);
            String raw = provider.chat(systemPrompt, List.of(), userPrompt).trim();
            String json = extractJsonArray(raw);
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String pillarName = node.path("pillar").asText(null);
                    String brief = node.path("brief").asText(null);
                    String example = node.path("example").asText("");
                    if (pillarName == null || brief == null || brief.isBlank()) continue;
                    try {
                        result.put(ContentPillar.valueOf(pillarName.trim()), new Brief(brief.trim(), example.trim()));
                    } catch (IllegalArgumentException ignore) {
                        // pilar desconegut → s'ignora, s'usarà la plantilla per defecte
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Content brief IA fallida ({}), s'usaran plantilles per defecte: {}", sector, e.getMessage());
        }
        return result;
    }

    /** Extreu l'array JSON encara que vingui embolcallat amb ```json ... ``` o text. */
    private String extractJsonArray(String raw) {
        String s = raw.replaceAll("(?s)```(json)?", "").trim();
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }
}
