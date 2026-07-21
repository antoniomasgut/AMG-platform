package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialContentGeneratorService {

    private final AIProviderRouter aiRouter;

    private static final String MODEL = "claude-haiku-4-5-20251001";

    /**
     * Genera una idea breu de contingut per aquesta setmana (Mòdul 55, suggeriments proactius).
     * Retorna 1-2 frases amb una proposta concreta segons sector i època de l'any.
     */
    public String generateWeeklyIdea(String businessContext, String sector, String dateContext) {
        return generateWeeklyIdea(businessContext, sector, dateContext, null);
    }

    /**
     * Variant amb context de rendiment (P3): la idea prioritza els formats i temes
     * que han funcionat de veritat per a aquest negoci, no només sector i època.
     */
    public String generateWeeklyIdea(String businessContext, String sector, String dateContext,
                                     String performanceContext) {
        String systemPrompt = """
                Ets un estrateg de xarxes socials per a negocis locals a Mallorca.
                Proposa UNA idea de publicació concreta i accionable per a aquesta setmana.
                Té en compte el sector i l'època de l'any (temporada, festius, tendències locals).
                Si tens dades de rendiment, prioritza els formats i temes que han funcionat millor
                i esmenta-ho breument (p.ex. "com que les fotos del local funcionen bé...").
                Màxim 2 frases. To proper, en català. Comença directament amb la idea, sense preàmbuls.
                """;
        String userPrompt = "Negoci: " + businessContext + "\nSector: " + sector
                + "\nContext temporal: " + dateContext
                + (performanceContext != null && !performanceContext.isBlank()
                   ? "\n\n" + performanceContext : "")
                + "\n\nProposa la idea:";
        try {
            var provider = aiRouter.forModel(MODEL);
            return provider.chat(systemPrompt, List.of(), userPrompt).trim();
        } catch (Exception e) {
            log.warn("Error generant idea setmanal IA: {}", e.getMessage());
            return null;
        }
    }

    /**
     * P39: genera 3 opcions de caption en tons diferents (casual / professional / promocional).
     * Retorna entre 1 i 3 opcions; fa fallback a 1 sola opció si el parsing falla.
     */
    public List<String> generateCaptionOptions(String network, String businessContext,
                                               String userBrief, List<String> recentCaptions) {
        String networkName = switch (network.toUpperCase()) {
            case "INSTAGRAM"       -> "Instagram";
            case "FACEBOOK"        -> "Facebook";
            case "GOOGLE_BUSINESS" -> "Google Business Profile";
            case "LINKEDIN"        -> "LinkedIn";
            default -> "xarxes socials";
        };
        String recentHint = "";
        if (recentCaptions != null && !recentCaptions.isEmpty()) {
            recentHint = "Evita repetir temes i expressions de: "
                + recentCaptions.stream().limit(3)
                    .map(c -> "«" + (c.length() > 60 ? c.substring(0, 57) + "…" : c) + "»")
                    .collect(java.util.stream.Collectors.joining(", "))
                + "\n";
        }
        String systemPrompt = "Ets un copywriter expert en " + networkName + " per a negocis locals a Mallorca.\n"
            + recentHint
            + "Genera EXACTAMENT 3 versions de caption per al contingut descrit, en tons diferents:\n"
            + "1) To casual i proper (emojis, llenguatge informal)\n"
            + "2) To professional (directe, sense emojis excessius)\n"
            + "3) To promocional (CTA directa, benefici o oferta)\n"
            + "Format OBLIGATORI: separa les 3 opcions amb «---» en una línia sola. Cap text extra fora de les opcions.";
        String userPrompt = "Negoci: " + businessContext + "\nBreu: " + userBrief
            + "\n\nGenera les 3 opcions:";
        try {
            var provider = aiRouter.forModel(MODEL);
            String raw = provider.chat(systemPrompt, List.of(), userPrompt).trim();
            var parts = java.util.Arrays.stream(raw.split("(?m)^\\s*---\\s*$"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
            return parts.isEmpty() ? List.of(raw) : parts;
        } catch (Exception e) {
            log.warn("Error generant opcions de caption IA per {}: {}", network, e.getMessage());
            return List.of(generateCaption(network, businessContext, userBrief, recentCaptions));
        }
    }

    public String generateCaption(String network, String businessContext, String userBrief) {
        return generateCaption(network, businessContext, userBrief, List.of());
    }

    /**
     * P37: genera un caption adaptat a l'estil específic de cada xarxa.
     * P38: recentCaptions (últims 5 posts de la xarxa) per evitar repeticions.
     */
    public String generateCaption(String network, String businessContext, String userBrief,
                                  List<String> recentCaptions) {
        String styleGuide = switch (network.toUpperCase()) {
            case "INSTAGRAM" -> """
                    Ets un copywriter expert en Instagram per a negocis locals a Mallorca.
                    Escriu en català o castellà (adaptat al negoci).
                    Usa emojis (3-6 rellevants). Inclou 5-10 hashtags al final (#negoci #mallorca etc).
                    Màxim 300 caràcters de text principal + hashtags.
                    Torna NOMÉS el text del caption, res més.
                    """;
            case "FACEBOOK" -> """
                    Ets un copywriter expert en Facebook per a negocis locals a Mallorca.
                    Escriu en català o castellà (adaptat al negoci). To proper i conversacional.
                    Usa 1-2 emojis màxim. Sense hashtags (no funcionen bé a Facebook).
                    Màxim 250 caràcters. Acaba amb una crida a l'acció clara.
                    Torna NOMÉS el text del post, res més.
                    """;
            case "GOOGLE_BUSINESS" -> """
                    Ets un copywriter expert en Google Business Profile per a negocis locals a Mallorca.
                    Escriu en català o castellà (adaptat al negoci). To professional i local.
                    Sense emojis ni hashtags. Inclou el nom del negoci i la localitat si és rellevant.
                    Màxim 200 caràcters. Concís i informatiu.
                    Torna NOMÉS el text del post, res més.
                    """;
            case "LINKEDIN" -> """
                    Ets un copywriter expert en LinkedIn per a negocis locals a Mallorca.
                    Escriu en català o castellà (adaptat al negoci). To professional però proper.
                    Màxim 3 hashtags. 1-2 emojis. Màxim 300 caràcters.
                    Torna NOMÉS el text del post, res més.
                    """;
            default -> "Escriu un caption curt i professional per a xarxes socials. Màxim 200 caràcters.";
        };

        // P38: evitar repeticions de les publicacions recents
        String systemPrompt = styleGuide;
        if (recentCaptions != null && !recentCaptions.isEmpty()) {
            var samples = recentCaptions.stream()
                .map(c -> "«" + (c.length() > 80 ? c.substring(0, 77) + "…" : c) + "»")
                .collect(java.util.stream.Collectors.joining(", "));
            systemPrompt += "Evita repetir els mateixos temes, expressions o crida a l'acció de "
                + "publicacions recents: " + samples + "\n";
        }

        String userPrompt = "Negoci: " + businessContext + "\n\nBreu de l'usuari: " + userBrief
                + "\n\nGenera el caption per a " + network + ":";

        try {
            var provider = aiRouter.forModel(MODEL);
            return provider.chat(systemPrompt, List.of(), userPrompt).trim();
        } catch (Exception e) {
            log.warn("Error generant caption IA per {}: {}", network, e.getMessage());
            return userBrief;
        }
    }
}
