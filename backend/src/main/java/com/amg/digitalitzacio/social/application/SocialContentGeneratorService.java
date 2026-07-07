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
        String systemPrompt = """
                Ets un estrateg de xarxes socials per a negocis locals a Mallorca.
                Proposa UNA idea de publicació concreta i accionable per a aquesta setmana.
                Té en compte el sector i l'època de l'any (temporada, festius, tendències locals).
                Màxim 2 frases. To proper, en català. Comença directament amb la idea, sense preàmbuls.
                """;
        String userPrompt = "Negoci: " + businessContext + "\nSector: " + sector
                + "\nContext temporal: " + dateContext + "\n\nProposa la idea:";
        try {
            var provider = aiRouter.forModel(MODEL);
            return provider.chat(systemPrompt, List.of(), userPrompt).trim();
        } catch (Exception e) {
            log.warn("Error generant idea setmanal IA: {}", e.getMessage());
            return null;
        }
    }

    public String generateCaption(String network, String businessContext, String userBrief) {
        String systemPrompt = switch (network.toUpperCase()) {
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
                    Sense emojis. Inclou el nom del negoci i la localitat si és rellevant.
                    Màxim 200 caràcters. Concís i informatiu.
                    Torna NOMÉS el text del post, res més.
                    """;
            default -> "Escriu un caption curt i professional per a xarxes socials. Màxim 200 caràcters.";
        };

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
