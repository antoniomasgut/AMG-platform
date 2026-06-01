package com.amg.digitalitzacio.shared.sysconfig.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigTestService {

    private final SystemConfigService configService;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public record TestResult(boolean ok, String message) {}

    public TestResult test(String key) {
        return switch (key) {
            case "BREVO_API_KEY"        -> testBrevo();
            case "ANTHROPIC_API_KEY"    -> testAnthropic();
            case "TELEGRAM_BOT_TOKEN"   -> testTelegram();
            case "GOOGLE_PLACES_API_KEY"-> testGooglePlaces();
            default -> new TestResult(false, "No hi ha prova disponible per a aquesta clau.");
        };
    }

    private TestResult testBrevo() {
        String key = configService.get("BREVO_API_KEY");
        if (key == null || key.isBlank()) return new TestResult(false,
                "La clau no està configurada. Afegeix-la des d'aquesta pàgina.");
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/account"))
                    .header("api-key", key)
                    .GET().build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(res.body());
                String email = json.path("email").asText("desconegut");
                String plan  = json.path("plan").path(0).path("type").asText("Free");
                return new TestResult(true,
                        "Clau vàlida. Compte: " + email + " · Pla: " + plan +
                        ". El bot ja pot enviar emails als clients.");
            } else if (res.statusCode() == 401) {
                String body = res.body();
                if (body.contains("IP")) return new TestResult(false,
                        "IP del servidor no autoritzada a Brevo. Ves a Brevo → Configuració → IPs autoritzades i afegeix 65.108.148.62.");
                return new TestResult(false,
                        "Clau no vàlida. Comprova que has copiat la clau API v3 (xkeysib-...) des de Brevo → Configuració → Claves API.");
            }
            return new TestResult(false, "Error inesperat de Brevo (HTTP " + res.statusCode() + ").");
        } catch (Exception e) {
            log.warn("Brevo test failed: {}", e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb Brevo. Comprova la connexió del servidor.");
        }
    }

    private TestResult testAnthropic() {
        String key = configService.get("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) return new TestResult(false,
                "La clau no està configurada. Afegeix-la des d'aquesta pàgina.");
        try {
            String body = """
                    {"model":"claude-haiku-4-5-20251001","max_tokens":16,
                     "messages":[{"role":"user","content":"Di hola en una paraula."}]}""";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("x-api-key", key)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                return new TestResult(true,
                        "Clau vàlida. Claude respon correctament. Els agents IA ja estan operatius.");
            } else if (res.statusCode() == 401) {
                return new TestResult(false,
                        "Clau no vàlida. Comprova que has copiat la clau API completa des de console.anthropic.com → API Keys.");
            } else if (res.statusCode() == 429) {
                return new TestResult(true,
                        "Clau vàlida (límit de ràtio assolit momentàniament, però la clau funciona).");
            }
            return new TestResult(false, "Error inesperat d'Anthropic (HTTP " + res.statusCode() + ").");
        } catch (Exception e) {
            log.warn("Anthropic test failed: {}", e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb Anthropic. Comprova la connexió del servidor.");
        }
    }

    private TestResult testTelegram() {
        String token = configService.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) return new TestResult(false,
                "El token no està configurat. Afegeix-lo des d'aquesta pàgina.");
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token + "/getMe"))
                    .GET().build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(res.body());
            if (json.path("ok").asBoolean(false)) {
                String name = json.path("result").path("first_name").asText("Bot");
                String username = json.path("result").path("username").asText("");
                return new TestResult(true,
                        "Bot actiu: " + name + (username.isBlank() ? "" : " (@" + username + ")") +
                        ". Les notificacions i els agents de Telegram funcionaran correctament.");
            }
            return new TestResult(false,
                    "Token no vàlid. Crea un bot nou amb @BotFather a Telegram i copia el token complet.");
        } catch (Exception e) {
            log.warn("Telegram test failed: {}", e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb Telegram. Comprova la connexió del servidor.");
        }
    }

    private TestResult testGooglePlaces() {
        String key = configService.get("GOOGLE_PLACES_API_KEY");
        if (key == null || key.isBlank()) return new TestResult(false,
                "La clau no està configurada. Afegeix-la des d'aquesta pàgina.");
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://maps.googleapis.com/maps/api/place/textsearch/json?query=test&key=" + key))
                    .GET().build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(res.body());
            String status = json.path("status").asText("");
            if ("OK".equals(status) || "ZERO_RESULTS".equals(status)) {
                return new TestResult(true,
                        "Clau vàlida. Google Places API operativa. El mòdul de prospecció pot cercar negocis.");
            } else if ("REQUEST_DENIED".equals(status)) {
                return new TestResult(false,
                        "Clau denegada. Activa 'Places API' a Google Cloud Console → APIs i serveis → Biblioteca.");
            }
            return new TestResult(false, "Error de Google Places: " + status);
        } catch (Exception e) {
            log.warn("Google Places test failed: {}", e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb Google. Comprova la connexió del servidor.");
        }
    }
}
