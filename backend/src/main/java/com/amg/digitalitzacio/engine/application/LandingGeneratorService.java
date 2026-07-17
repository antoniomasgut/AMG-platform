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
                Eres un experto copywriter en marketing digital para negocios locales, especializado en crear landing pages que convierten.
                Tu misión es crear contenido específico, emotivo y orientado a resultados — nunca genérico.
                Identifica siempre el principal punto de dolor del cliente objetivo y construye el mensaje desde ahí.
                Para negocios en Mallorca, usa referencias locales cuando sea relevante.
                Responde ÚNICAMENTE con un objeto JSON válido. Sin explicaciones, sin markdown, sin bloques de código.
                """;
            case "en" -> """
                You are an expert copywriter specialized in high-converting landing pages for local businesses.
                Your mission: create specific, emotionally-resonant, results-oriented content — never generic.
                Always identify the main pain point of the target customer and build the message from there.
                Respond ONLY with a valid JSON object. No explanations, no markdown, no code blocks.
                """;
            case "de" -> """
                Sie sind ein erfahrener Copywriter für hochkonvertierende Landingpages lokaler Unternehmen.
                Ihre Mission: spezifische, emotional ansprechende, ergebnisorientierte Inhalte — nie generisch.
                Identifizieren Sie stets den Hauptschmerzpunkt des Zielkunden.
                Antworten Sie NUR mit einem gültigen JSON-Objekt. Keine Erklärungen, kein Markdown.
                """;
            default -> """
                Ets un expert en copywriting per a landing pages d'alt rendiment de negocis locals.
                La teva missió: crear contingut específic, emotiu i orientat a resultats — mai genèric.
                Identifica sempre el principal punt de dolor del client objectiu i construeix el missatge des d'allà.
                Per a negocis de Mallorca, usa referències locals quan sigui rellevant.
                Respon ÚNICAMENT amb un objecte JSON vàlid. Sense explicacions, sense markdown, sense blocs de codi.
                """;
        };
    }

    private String buildUserMessage(String blockType, String business, String sector, String lang) {
        String ctx = sector.isBlank() ? business : business + " (" + sector + ")";
        return switch (blockType) {
            case "hero"         -> heroPrompt(ctx, lang);
            case "header"       -> headerPrompt(ctx, lang);
            case "text"         -> textPrompt(ctx, lang);
            case "services"     -> servicesPrompt(ctx, lang);
            case "faq"          -> faqPrompt(ctx, lang);
            case "cta"          -> ctaPrompt(ctx, lang);
            case "testimonials" -> testimonialsPrompt(ctx, lang);
            case "pricing"      -> pricingPrompt(ctx, lang);
            case "team"         -> teamPrompt(ctx, lang);
            case "reviews"      -> reviewsPrompt(ctx, lang);
            case "stats"        -> statsPrompt(ctx, lang);
            case "steps"        -> stepsPrompt(ctx, lang);
            case "trust-bar"    -> trustBarPrompt(ctx, lang);
            default -> """
                Genera contingut per al bloc "%s" per a "%s". Retorna un JSON amb les propietats adequades.
                """.formatted(blockType, ctx);
        };
    }

    // ── HERO ──────────────────────────────────────────────────────────────────

    private String heroPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Crea el bloque HERO para "%s".
                Identifica el principal punto de dolor del cliente de este negocio y construye el titular sobre esa solución.
                El titular debe ser específico, impactante y en máx 9 palabras (sin signos de interrogación ni exclamación al inicio).
                El subtítulo describe en 1-2 oraciones qué hace este negocio, para quién y qué resultado consiguen.
                badgeText: frase corta de confianza (ej: "15 años de experiencia · Mallorca", "Certificado · Respuesta en 2h", etc.)
                Retorna exactamente este JSON (sin ningún texto extra):
                {"title":"[titular específico máx 9 palabras]","subtitle":"[propuesta de valor 1-2 oraciones específicas]","ctaText":"[acción concreta]","ctaLink":"#contact","badgeText":"[frase de confianza corta]","secondaryCtaText":"Ver servicios","secondaryCtaLink":"#services"}
                """.formatted(ctx);
            case "en" -> """
                Create the HERO block for "%s".
                Identify the main pain point of this business's customer and build the headline around the solution.
                Headline: specific, impactful, max 9 words. Subtitle: 1-2 sentences about what they do, for whom, and what result.
                badgeText: short trust phrase (e.g. "15 years experience · Certified")
                Return exactly this JSON (no extra text):
                {"title":"[specific headline max 9 words]","subtitle":"[value proposition 1-2 specific sentences]","ctaText":"[concrete action]","ctaLink":"#contact","badgeText":"[short trust phrase]","secondaryCtaText":"See services","secondaryCtaLink":"#services"}
                """.formatted(ctx);
            default -> """
                Crea el bloc HERO per a "%s".
                Identifica el principal punt de dolor del client d'aquest negoci i construeix el titular sobre la solució.
                El titular ha de ser específic, impactant i en màx 9 paraules (sense signes d'interrogació ni exclamació a l'inici).
                El subtítol descriu en 1-2 oracions què fa aquest negoci, per a qui i quin resultat aconsegueixen.
                badgeText: frase curta de confiança (ex: "15 anys d'experiència · Mallorca", "Certificat · Resposta en 2h", etc.)
                Retorna exactament aquest JSON (sense cap text extra):
                {"title":"[titular específic màx 9 paraules]","subtitle":"[proposta de valor 1-2 oracions específiques]","ctaText":"[acció concreta]","ctaLink":"#contact","badgeText":"[frase de confiança curta]","secondaryCtaText":"Veure serveis","secondaryCtaLink":"#services"}
                """.formatted(ctx);
        };
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private String headerPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Crea la navegación del encabezado para "%s".
                Los links deben ser secciones reales de la landing (según el sector).
                Retorna exactamente este JSON:
                {"logoText":"[nombre corto del negocio]","links":[{"label":"Servicios","href":"#services"},{"label":"Nosotros","href":"#about"},{"label":"Opiniones","href":"#reviews"},{"label":"Contacto","href":"#contact"}],"ctaText":"[acción principal]","ctaLink":"#contact"}
                """.formatted(ctx);
            default -> """
                Crea la navegació de la capçalera per a "%s".
                Els links han de ser seccions reals de la landing (segons el sector).
                Retorna exactament aquest JSON:
                {"logoText":"[nom curt del negoci]","links":[{"label":"Serveis","href":"#services"},{"label":"Nosaltres","href":"#about"},{"label":"Opinions","href":"#reviews"},{"label":"Contacte","href":"#contact"}],"ctaText":"[acció principal]","ctaLink":"#contact"}
                """.formatted(ctx);
        };
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    private String statsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Crea 3 estadísticas de credibilidad realistas para "%s".
                Usa números específicos y plausibles para el sector (no inventes números imposibles).
                Retorna exactamente este JSON:
                {"items":[{"number":"[número+símbolo]","label":"[etiqueta 2-3 palabras]","icon":"[1 emoji relevante]"},{"number":"...","label":"...","icon":"..."},{"number":"...","label":"...","icon":"..."}]}
                """.formatted(ctx);
            default -> """
                Crea 3 estadístiques de credibilitat realistes per a "%s".
                Usa números específics i plausibles per al sector (no inventes números impossibles).
                Retorna exactament aquest JSON:
                {"items":[{"number":"[número+símbol]","label":"[etiqueta 2-3 paraules]","icon":"[1 emoji rellevant]"},{"number":"...","label":"...","icon":"..."},{"number":"...","label":"...","icon":"..."}]}
                """.formatted(ctx);
        };
    }

    // ── STEPS ─────────────────────────────────────────────────────────────────

    private String stepsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Crea 3-4 pasos del proceso de trabajo para "%s".
                Cada paso debe describir una acción real y específica del proceso de este sector.
                Retorna exactamente este JSON:
                {"title":"Cómo trabajamos","items":[{"number":"1","title":"[nombre paso]","description":"[descripción acción concreta 1-2 oraciones]"},{"number":"2","title":"...","description":"..."},{"number":"3","title":"...","description":"..."}]}
                """.formatted(ctx);
            default -> """
                Crea 3-4 passos del procés de treball per a "%s".
                Cada pas ha de descriure una acció real i específica del procés d'aquest sector.
                Retorna exactament aquest JSON:
                {"title":"Com treballem","items":[{"number":"1","title":"[nom pas]","description":"[descripció acció concreta 1-2 oracions]"},{"number":"2","title":"...","description":"..."},{"number":"3","title":"...","description":"..."}]}
                """.formatted(ctx);
        };
    }

    // ── TRUST BAR ─────────────────────────────────────────────────────────────

    private String trustBarPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Crea 4-5 indicadores de confianza para "%s".
                Usa certificaciones, asociaciones, premios o partners reales del sector (o plausibles).
                Retorna exactamente este JSON:
                {"title":"Por qué confiar en nosotros","items":[{"name":"[nombre indicador]","icon":"[emoji]"},{"name":"...","icon":"..."},{"name":"...","icon":"..."},{"name":"...","icon":"..."}]}
                """.formatted(ctx);
            default -> """
                Crea 4-5 indicadors de confiança per a "%s".
                Usa certificacions, associacions, premis o partners reals del sector (o plausibles).
                Retorna exactament aquest JSON:
                {"title":"Per què confiar en nosaltres","items":[{"name":"[nom indicador]","icon":"[emoji]"},{"name":"...","icon":"..."},{"name":"...","icon":"..."},{"name":"...","icon":"..."}]}
                """.formatted(ctx);
        };
    }

    // ── TEXT ──────────────────────────────────────────────────────────────────

    private String textPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera el bloque "Sobre nosotros" para "%s".
                Primer párrafo: historia o propósito del negocio, específico del sector y contexto local.
                Segundo párrafo: qué hace diferente a este negocio respecto a la competencia (propuesta diferencial real).
                Retorna exactamente este JSON:
                {"title":"[título atractivo, no 'Sobre nosotros' genérico]","body":"<p>[párrafo historia, 3-4 oraciones específicas]</p><p>[párrafo diferencial, 2-3 oraciones]</p>"}
                """.formatted(ctx);
            default -> """
                Genera el bloc "Sobre nosaltres" per a "%s".
                Primer paràgraf: història o propòsit del negoci, específic del sector i context local.
                Segon paràgraf: què fa diferent a aquest negoci respecte a la competència (proposta diferencial real).
                Retorna exactament aquest JSON:
                {"title":"[títol atractiu, no 'Sobre nosaltres' genèric]","body":"<p>[paràgraf història, 3-4 oracions específiques]</p><p>[paràgraf diferencial, 2-3 oracions]</p>"}
                """.formatted(ctx);
        };
    }

    // ── SERVICES ──────────────────────────────────────────────────────────────

    private String servicesPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera el bloque SERVICIOS para "%s" con 3-4 servicios reales y específicos del sector.
                Para cada servicio: nombre concreto (no genérico), descripción que menciona el resultado/beneficio para el cliente, y un emoji relevante como icono.
                Retorna exactamente este JSON:
                {"title":"Nuestros servicios","items":[{"name":"[nombre servicio específico]","desc":"[resultado que obtiene el cliente, 1-2 oraciones]","icon":"[emoji]"},{"name":"...","desc":"...","icon":"..."},{"name":"...","desc":"...","icon":"..."}]}
                """.formatted(ctx);
            default -> """
                Genera el bloc SERVEIS per a "%s" amb 3-4 serveis reals i específics del sector.
                Per a cada servei: nom concret (no genèric), descripció que menciona el resultat/benefici per al client, i un emoji rellevant com a icona.
                Retorna exactament aquest JSON:
                {"title":"Els nostres serveis","items":[{"name":"[nom servei específic]","desc":"[resultat que obté el client, 1-2 oracions]","icon":"[emoji]"},{"name":"...","desc":"...","icon":"..."},{"name":"...","desc":"...","icon":"..."}]}
                """.formatted(ctx);
        };
    }

    // ── FAQ ───────────────────────────────────────────────────────────────────

    private String faqPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 4-5 preguntas frecuentes reales que hacen los clientes de "%s".
                Las preguntas deben ser las dudas más comunes antes de contratar (precio, tiempo, proceso, garantías).
                Las respuestas: directas, concretas y tranquilizadoras (no evasivas).
                Retorna exactamente este JSON:
                {"title":"Preguntas frecuentes","items":[{"question":"[pregunta real del cliente]","answer":"[respuesta directa y específica]"},{"question":"...","answer":"..."},{"question":"...","answer":"..."},{"question":"...","answer":"..."}]}
                """.formatted(ctx);
            default -> """
                Genera 4-5 preguntes freqüents reals que fan els clients de "%s".
                Les preguntes han de ser els dubtes més comuns abans de contractar (preu, temps, procés, garanties).
                Les respostes: directes, concretes i tranquil·litzadores (no evasives).
                Retorna exactament aquest JSON:
                {"title":"Preguntes freqüents","items":[{"question":"[pregunta real del client]","answer":"[resposta directa i específica]"},{"question":"...","answer":"..."},{"question":"...","answer":"..."},{"question":"...","answer":"..."}]}
                """.formatted(ctx);
        };
    }

    // ── CTA ───────────────────────────────────────────────────────────────────

    private String ctaPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera un CTA poderoso para "%s".
                El titular debe crear urgencia o emoción positiva (no usar "¡Contáctanos!" genérico).
                El subtítulo debe eliminar la fricción de contactar (garantía, sin compromiso, rapidez).
                Retorna exactamente este JSON:
                {"title":"[frase motivadora específica, màx 12 palabras]","subtitle":"[elimina fricción, 1 oración]","ctaText":"[texto botón acción específica]","ctaLink":"#contact"}
                """.formatted(ctx);
            default -> """
                Genera un CTA poderós per a "%s".
                El titular ha de crear urgència o emoció positiva (no usar "Contacta'ns!" genèric).
                El subtítol ha d'eliminar la fricció de contactar (garantia, sense compromís, rapidesa).
                Retorna exactament aquest JSON:
                {"title":"[frase motivadora específica, màx 12 paraules]","subtitle":"[elimina fricció, 1 oració]","ctaText":"[text botó acció específica]","ctaLink":"#contact"}
                """.formatted(ctx);
        };
    }

    // ── TESTIMONIALS ──────────────────────────────────────────────────────────

    private String testimonialsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 3 testimonios realistas y específicos para "%s".
                Cada testimonio: menciona un resultado concreto (no "muy bueno" genérico), incluye el nombre mallorquín/catalán/español, y el rol/contexto (cliente habitual, propietaria, etc.).
                Retorna exactamente este JSON:
                {"title":"Lo que dicen nuestros clientes","items":[{"name":"[nombre realista]","role":"[contexto breve]","text":"[testimonio con resultado específico, 2-3 oraciones]","rating":5},{"name":"...","role":"...","text":"...","rating":5},{"name":"...","role":"...","text":"...","rating":5}]}
                """.formatted(ctx);
            default -> """
                Genera 3 testimonis realistes i específics per a "%s".
                Cada testimoni: menciona un resultat concret (no "molt bo" genèric), inclou el nom mallorquí/català/espanyol i el rol/context (client habitual, propietària, etc.).
                Retorna exactament aquest JSON:
                {"title":"Què diuen els nostres clients","items":[{"name":"[nom realista]","role":"[context breu]","text":"[testimoni amb resultat específic, 2-3 oracions]","rating":5},{"name":"...","role":"...","text":"...","rating":5},{"name":"...","role":"...","text":"...","rating":5}]}
                """.formatted(ctx);
        };
    }

    // ── PRICING ───────────────────────────────────────────────────────────────

    private String pricingPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 2-3 tarifas o paquetes reales para "%s" con precios plausibles del sector.
                El paquete destacado (highlighted:true) debe ser el de mejor relación calidad-precio.
                Las características: específicas del sector, no genéricas.
                Retorna exactamente este JSON:
                {"title":"Nuestras tarifas","items":[{"name":"[nombre]","description":"[a quién va dirigido]","price":"[número]","period":"mes","features":["característica específica 1","característica específica 2","característica específica 3"],"highlighted":false},{"name":"...","description":"...","price":"...","period":"mes","features":["...","...","...","..."],"highlighted":true}]}
                """.formatted(ctx);
            default -> """
                Genera 2-3 tarifes o paquets reals per a "%s" amb preus plausibles del sector.
                El paquet destacat (highlighted:true) ha de ser el de millor relació qualitat-preu.
                Les característiques: específiques del sector, no genèriques.
                Retorna exactament aquest JSON:
                {"title":"Les nostres tarifes","items":[{"name":"[nom]","description":"[a qui va dirigit]","price":"[número]","period":"mes","features":["característica específica 1","característica específica 2","característica específica 3"],"highlighted":false},{"name":"...","description":"...","price":"...","period":"mes","features":["...","...","...","..."],"highlighted":true}]}
                """.formatted(ctx);
        };
    }

    // ── TEAM ──────────────────────────────────────────────────────────────────

    private String teamPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 2-3 miembros de equipo plausibles para "%s".
                Nombres realistas del contexto balear/español. Roles específicos del sector. Bio: logro o especialización concreta.
                Retorna exactamente este JSON:
                {"title":"Nuestro equipo","items":[{"name":"[nombre]","role":"[cargo específico]","bio":"[especialización o logro concreto, 1-2 oraciones]","photo":""},{"name":"...","role":"...","bio":"...","photo":""}]}
                """.formatted(ctx);
            default -> """
                Genera 2-3 membres d'equip plausibles per a "%s".
                Noms realistes del context balear/espanyol. Rols específics del sector. Bio: assoliment o especialització concreta.
                Retorna exactament aquest JSON:
                {"title":"El nostre equip","items":[{"name":"[nom]","role":"[càrrec específic]","bio":"[especialització o assoliment concret, 1-2 oracions]","photo":""},{"name":"...","role":"...","bio":"...","photo":""}]}
                """.formatted(ctx);
        };
    }

    // ── REVIEWS ───────────────────────────────────────────────────────────────

    private String reviewsPrompt(String ctx, String lang) {
        return switch (lang) {
            case "es" -> """
                Genera 3 reseñas de Google realistas para "%s".
                Noms mallorquins/catalans/espanyols. Cada reseña: resultado específico, no genérico.
                Retorna exactamente este JSON:
                {"title":"Opiniones de nuestros clientes","googleMapsUrl":"","items":[{"name":"[nombre]","rating":5,"text":"[reseña con resultado concreto, 2-3 oraciones]","date":"2026-01-15"},{"name":"...","rating":5,"text":"...","date":"2026-02-10"},{"name":"...","rating":5,"text":"...","date":"2026-03-05"}]}
                """.formatted(ctx);
            default -> """
                Genera 3 ressenyes de Google realistes per a "%s".
                Noms mallorquins/catalans/espanyols. Cada ressenya: resultat específic, no genèric.
                Retorna exactament aquest JSON:
                {"title":"Opinions dels nostres clients","googleMapsUrl":"","items":[{"name":"[nom]","rating":5,"text":"[ressenya amb resultat concret, 2-3 oracions]","date":"2026-01-15"},{"name":"...","rating":5,"text":"...","date":"2026-02-10"},{"name":"...","rating":5,"text":"...","date":"2026-03-05"}]}
                """.formatted(ctx);
        };
    }

    // ── JSON PARSER ───────────────────────────────────────────────────────────

    private Map<String, Object> parseJson(String raw) {
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
