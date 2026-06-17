package com.amg.digitalitzacio.shared.sysconfig.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

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
            case "BREVO_API_KEY"           -> testBrevo();
            case "ANTHROPIC_API_KEY"       -> testAnthropic();
            case "TELEGRAM_BOT_TOKEN"      -> testTelegram();
            case "GOOGLE_PLACES_API_KEY"   -> testGooglePlaces();
            case "DEEPSEEK_API_KEY"        -> testDeepSeek();
            case "AMG_SALES_CHAT_ID"       -> testTelegramChat("AMG_SALES_CHAT_ID", "Vendes");
            case "TELEGRAM_CHAT_ID"        -> testTelegramChat("TELEGRAM_CHAT_ID", "InfraOps");
            case "GOOGLE_CALENDAR_SA_JSON" -> testGoogleCalendarSA();
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

    private TestResult testDeepSeek() {
        String key = configService.get("DEEPSEEK_API_KEY");
        if (key == null || key.isBlank()) return new TestResult(false,
                "La clau no està configurada. Afegeix-la des d'aquesta pàgina.");
        try {
            String body = """
                    {"model":"deepseek-chat","max_tokens":16,
                     "messages":[{"role":"user","content":"Di hola en una paraula."}]}""";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                return new TestResult(true,
                        "Clau vàlida. DeepSeek V3 respon correctament. Disponible com a model alternatiu als agents.");
            } else if (res.statusCode() == 401) {
                return new TestResult(false,
                        "Clau no vàlida. Comprova que has copiat la clau API des de platform.deepseek.com → API Keys.");
            } else if (res.statusCode() == 402) {
                return new TestResult(false,
                        "Saldo insuficient al compte DeepSeek. Recarrega crèdit a platform.deepseek.com → Top Up.");
            } else if (res.statusCode() == 429) {
                return new TestResult(true,
                        "Clau vàlida (límit de ràtio momentàniament assolit, però la clau funciona).");
            }
            return new TestResult(false, "Error inesperat de DeepSeek (HTTP " + res.statusCode() + ").");
        } catch (Exception e) {
            log.warn("DeepSeek test failed: {}", e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb DeepSeek. Comprova la connexió del servidor.");
        }
    }

    private TestResult testTelegramChat(String chatIdKey, String label) {
        String token = configService.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) return new TestResult(false,
                "TELEGRAM_BOT_TOKEN no configurat — configura'l primer per poder provar el chat.");
        String chatId = configService.get(chatIdKey);
        if (chatId == null || chatId.isBlank()) return new TestResult(false,
                "L'ID del chat no està configurat. Afegeix-lo des d'aquesta pàgina.");
        try {
            String body = "{\"chat_id\":\"" + chatId + "\",\"text\":\"\\u2705 Test AMG · Canal " + label + " operatiu.\",\"parse_mode\":\"HTML\"}";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(res.body());
            if (json.path("ok").asBoolean(false)) {
                return new TestResult(true,
                        "Missatge de prova enviat correctament al canal " + label + ". El bot té accés al grup/canal.");
            }
            String desc = json.path("description").asText("");
            if (desc.contains("chat not found")) return new TestResult(false,
                    "Chat no trobat. Comprova que l'ID és correcte i que el bot ha estat afegit al grup/canal " + label + ".");
            if (desc.contains("bot was kicked") || desc.contains("not a member")) return new TestResult(false,
                    "El bot no és membre del grup/canal " + label + ". Afegeix @" + label + " al grup i torna a provar.");
            return new TestResult(false, "Error de Telegram: " + desc);
        } catch (Exception e) {
            log.warn("Telegram {} chat test failed: {}", label, e.getMessage());
            return new TestResult(false, "No s'ha pogut connectar amb Telegram. Comprova la connexió del servidor.");
        }
    }

    private TestResult testGoogleCalendarSA() {
        String saJson = configService.get("GOOGLE_CALENDAR_SA_JSON");
        if (saJson == null || saJson.isBlank()) return new TestResult(false,
                "El JSON del Service Account no està configurat. Afegeix-lo des d'aquesta pàgina.");
        try {
            Map<String, Object> sa = objectMapper.readValue(saJson, new TypeReference<>() {});
            String clientEmail = (String) sa.get("client_email");
            String privateKeyPem = (String) sa.get("private_key");
            if (clientEmail == null || privateKeyPem == null) return new TestResult(false,
                    "El JSON no conté els camps 'client_email' o 'private_key'. Comprova que has enganxat el fitxer sencer.");
            if (!"service_account".equals(sa.get("type"))) return new TestResult(false,
                    "El JSON no és d'un Service Account (camp 'type' invàlid). Descarrega el JSON correcte des de Google Cloud Console.");

            // Genera JWT i intercanvia per access token
            String scope = "https://www.googleapis.com/auth/calendar";
            String tokenUrl = "https://oauth2.googleapis.com/token";
            long now = Instant.now().getEpochSecond();
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String claimsJson = objectMapper.writeValueAsString(Map.of(
                    "iss", clientEmail, "scope", scope,
                    "aud", tokenUrl, "exp", now + 3600, "iat", now));
            String claims = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));
            String toSign = header + "." + claims;

            String pem = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            PrivateKey pk = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(pk);
            sig.update(toSign.getBytes(StandardCharsets.UTF_8));
            String jwt = toSign + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

            String reqBody = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                    + "&assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody)).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});

            if (resp.containsKey("access_token")) {
                return new TestResult(true,
                        "Service Account vàlid. Compte: " + clientEmail + ". La creació i compartició de calendaris funcionarà correctament.");
            }
            String error = (String) resp.getOrDefault("error", "");
            String errorDesc = (String) resp.getOrDefault("error_description", "");
            if ("unauthorized_client".equals(error)) return new TestResult(false,
                    "Service Account no autoritzat. Activa l'API 'Google Calendar' al Google Cloud Console → APIs i serveis → Biblioteca.");
            if ("invalid_grant".equals(error)) return new TestResult(false,
                    "Credencials invàlides. Comprova que el Service Account no ha estat eliminat ni les seves claus revocades.");
            return new TestResult(false, "Error de Google (" + error + "): " + errorDesc);
        } catch (Exception e) {
            log.warn("Google Calendar SA test failed: {}", e.getMessage());
            return new TestResult(false, "Error validant el JSON: " + e.getMessage() + ". Comprova que has enganxat el fitxer JSON complet i sense modificar.");
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
