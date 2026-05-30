package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.GenerateBlockRequest;
import com.amg.digitalitzacio.engine.api.dto.GenerateBlockResponse;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LandingGeneratorService {

    private final AIProviderRouter aiProviderRouter;
    private final ObjectMapper objectMapper;

    public GenerateBlockResponse generate(GenerateBlockRequest req) {
        String model = (req.model() != null && !req.model().isBlank())
                ? req.model()
                : aiProviderRouter.defaultModel();

        String lang = (req.language() != null && !req.language().isBlank()) ? req.language() : "ca";
        String business = req.businessName() != null ? req.businessName() : "el negoci";
        String sector = req.sector() != null ? req.sector() : "";

        String systemPrompt = buildSystemPrompt(lang);
        String userMessage  = buildUserMessage(req.blockType(), business, sector, lang);

        String raw = aiProviderRouter.forModel(model)
                .chat(systemPrompt, List.of(), userMessage);

        Map<String, Object> props = parseJson(raw);
        return new GenerateBlockResponse(props, model);
    }

    private String buildSystemPrompt(String lang) {
        return switch (lang) {
            case "es" -> """
                Eres un experto en marketing digital para negocios locales.
                Cuando el usuario te pida contenido para un bloque de una landing page, responde ÚNICAMENTE con un objeto JSON válido con las propiedades del bloque.
                No incluyas explicaciones, ni markdown, ni bloques de código. Solo JSON puro.
                El contenido debe ser atractivo, profesional y en español.
                """;
            case "en" -> """
                You are a digital marketing expert for local businesses.
                When asked to generate content for a landing page block, respond ONLY with a valid JSON object containing the block properties.
                No explanations, no markdown, no code blocks. Pure JSON only.
                Content must be attractive, professional and in English.
                """;
            case "de" -> """
                Sie sind ein Digital-Marketing-Experte für lokale Unternehmen.
                Wenn Sie gebeten werden, Inhalte für einen Landing-Page-Block zu erstellen, antworten Sie NUR mit einem gültigen JSON-Objekt mit den Block-Eigenschaften.
                Keine Erklärungen, kein Markdown. Nur reines JSON.
                Der Inhalt muss ansprechend und professionell auf Deutsch sein.
                """;
            default -> """
                Ets un expert en màrqueting digital per a negocis locals de Mallorca.
                Quan et demanin contingut per a un bloc d'una landing page, respon ÚNICAMENT amb un objecte JSON vàlid amb les propietats del bloc.
                Sense explicacions, sense markdown, sense blocs de codi. Només JSON pur.
                El contingut ha de ser atractiu, professional i en català.
                """;
        };
    }

    private String buildUserMessage(String blockType, String business, String sector, String lang) {
        String ctx = sector.isBlank() ? business : business + " (" + sector + ")";
        return switch (blockType) {
            case "hero" -> heroPrompt(ctx, lang);
            case "text" -> textPrompt(ctx, lang);
            case "services" -> servicesPrompt(ctx, lang);
            case "faq" -> faqPrompt(ctx, lang);
            case "cta" -> ctaPrompt(ctx, lang);
            case "testimonials" -> testimonialsPrompt(ctx, lang);
            case "pricing" -> pricingPrompt(ctx, lang);
            case "team" -> teamPrompt(ctx, lang);
            case "reviews" -> reviewsPrompt(ctx, lang);
            default -> """
                Genera contingut per al bloc "%s" per a "%s". Retorna un JSON amb les propietats adequades.
                """.formatted(blockType, ctx);
        };
    }

    private String heroPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera el contenido del bloque HERO para "%s".
                Devuelve exactamente este JSON (sin ningún texto extra):
                {"title":"[titular impactante en máx 8 palabras]","subtitle":"[frase de valor en 1-2 oraciones]","ctaText":"[texto del botón de acción]","ctaLink":"#contact"}
                """.formatted(ctx);
            case "en" -> """
                Generate HERO block content for "%s".
                Return exactly this JSON (no extra text):
                {"title":"[impactful headline max 8 words]","subtitle":"[value proposition 1-2 sentences]","ctaText":"[CTA button text]","ctaLink":"#contact"}
                """.formatted(ctx);
            default -> """
                Genera el contingut del bloc HERO per a "%s".
                Retorna exactament aquest JSON (sense cap text extra):
                {"title":"[titular impactant en màx 8 paraules]","subtitle":"[frase de valor en 1-2 oracions]","ctaText":"[text del botó d'acció]","ctaLink":"#contact"}
                """.formatted(ctx);
        };
    }

    private String textPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera un bloque de texto "Sobre nosotros" para "%s".
                Retorna exactamente este JSON:
                {"title":"[título atractivo]","body":"<p>[párrafo 1 de presentación del negocio, 3-4 oraciones]</p><p>[párrafo 2 de valores o propuesta diferencial, 2-3 oraciones]</p>"}
                """.formatted(ctx);
            default -> """
                Genera un bloc de text "Sobre nosaltres" per a "%s".
                Retorna exactament aquest JSON:
                {"title":"[títol atractiu]","body":"<p>[paràgraf 1 de presentació del negoci, 3-4 oracions]</p><p>[paràgraf 2 de valors o proposta diferencial, 2-3 oracions]</p>"}
                """.formatted(ctx);
        };
    }

    private String servicesPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera el bloque SERVICIOS para "%s" con 3-4 servicios reales del sector.
                Retorna exactamente este JSON:
                {"title":"Nuestros servicios","items":[{"name":"[nombre servicio]","desc":"[descripción 1-2 oraciones]"},{"name":"...","desc":"..."},{"name":"...","desc":"..."}]}
                """.formatted(ctx);
            default -> """
                Genera el bloc SERVEIS per a "%s" amb 3-4 serveis reals del sector.
                Retorna exactament aquest JSON:
                {"title":"Els nostres serveis","items":[{"name":"[nom servei]","desc":"[descripció 1-2 oracions]"},{"name":"...","desc":"..."},{"name":"...","desc":"..."}]}
                """.formatted(ctx);
        };
    }

    private String faqPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 4 preguntas frecuentes reales para "%s".
                Retorna exactamente este JSON:
                {"title":"Preguntas frecuentes","items":[{"q":"[pregunta]","a":"[respuesta concisa]"},{"q":"...","a":"..."},{"q":"...","a":"..."},{"q":"...","a":"..."}]}
                """.formatted(ctx);
            default -> """
                Genera 4 preguntes freqüents reals per a "%s".
                Retorna exactament aquest JSON:
                {"title":"Preguntes freqüents","items":[{"q":"[pregunta]","a":"[resposta concisa]"},{"q":"...","a":"..."},{"q":"...","a":"..."},{"q":"...","a":"..."}]}
                """.formatted(ctx);
        };
    }

    private String ctaPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera un CTA convincente para "%s".
                Retorna exactamente este JSON:
                {"title":"[frase motivadora de máx 12 palabras]","ctaText":"[texto botón acción]","ctaLink":"#contact"}
                """.formatted(ctx);
            default -> """
                Genera un CTA convincent per a "%s".
                Retorna exactament aquest JSON:
                {"title":"[frase motivadora de màx 12 paraules]","ctaText":"[text botó acció]","ctaLink":"#contact"}
                """.formatted(ctx);
        };
    }

    private String testimonialsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 3 testimonios realistas (no genéricos) para "%s".
                Retorna exactamente este JSON:
                {"title":"Lo que dicen nuestros clientes","items":[{"name":"[nombre]","text":"[testimonio personal 2-3 oraciones]","rating":5},{"name":"...","text":"...","rating":5},{"name":"...","text":"...","rating":5}]}
                """.formatted(ctx);
            default -> """
                Genera 3 testimonis realistes (no genèrics) per a "%s".
                Retorna exactament aquest JSON:
                {"title":"Què diuen els nostres clients","items":[{"name":"[nom]","text":"[testimoni personal 2-3 oracions]","rating":5},{"name":"...","text":"...","rating":5},{"name":"...","text":"...","rating":5}]}
                """.formatted(ctx);
        };
    }

    private String pricingPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 2-3 tarifas reales para "%s".
                Retorna exactamente este JSON:
                {"title":"Nuestras tarifas","items":[{"name":"[nombre]","description":"[descripción corta]","price":"[número sin símbolo]","period":"mes","features":["característica 1","característica 2","característica 3"],"highlighted":false},{"name":"...","description":"...","price":"...","period":"mes","features":["...","...","...","..."],"highlighted":true}]}
                """.formatted(ctx);
            default -> """
                Genera 2-3 tarifes reals per a "%s".
                Retorna exactament aquest JSON:
                {"title":"Les nostres tarifes","items":[{"name":"[nom]","description":"[descripció curta]","price":"[número sense símbol]","period":"mes","features":["característica 1","característica 2","característica 3"],"highlighted":false},{"name":"...","description":"...","price":"...","period":"mes","features":["...","...","...","..."],"highlighted":true}]}
                """.formatted(ctx);
        };
    }

    private String teamPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 2-3 miembros de equipo plausibles para "%s".
                Retorna exactamente este JSON:
                {"title":"Nuestro equipo","items":[{"name":"[nombre]","role":"[cargo]","bio":"[descripción profesional 1-2 oraciones]","photo":""},{"name":"...","role":"...","bio":"...","photo":""}]}
                """.formatted(ctx);
            default -> """
                Genera 2-3 membres d'equip plausibles per a "%s".
                Retorna exactament aquest JSON:
                {"title":"El nostre equip","items":[{"name":"[nom]","role":"[càrrec]","bio":"[descripció professional 1-2 oracions]","photo":""},{"name":"...","role":"...","bio":"...","photo":""}]}
                """.formatted(ctx);
        };
    }

    private String reviewsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 3 reseñas de Google realistas para "%s".
                Retorna exactamente este JSON:
                {"title":"Opiniones de clientes","googleMapsUrl":"","items":[{"name":"[nombre]","rating":5,"text":"[reseña personal 2-3 oraciones]","date":"2026-01-15"},{"name":"...","rating":5,"text":"...","date":"2026-02-10"},{"name":"...","rating":5,"text":"...","date":"2026-03-05"}]}
                """.formatted(ctx);
            default -> """
                Genera 3 ressenyes de Google realistes per a "%s".
                Retorna exactament aquest JSON:
                {"title":"Opinions de clients","googleMapsUrl":"","items":[{"name":"[nom]","rating":5,"text":"[ressenya personal 2-3 oracions]","date":"2026-01-15"},{"name":"...","rating":5,"text":"...","date":"2026-02-10"},{"name":"...","rating":5,"text":"...","date":"2026-03-05"}]}
                """.formatted(ctx);
        };
    }

    private Map<String, Object> parseJson(String raw) {
        // Extreu JSON del text (el model podria afegir text extra)
        String json = raw.trim();
        int start = json.indexOf('{');
        int end   = json.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse AI response as JSON: {}", raw);
            return Map.of("title", raw.trim());
        }
    }
}
