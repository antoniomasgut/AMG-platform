package com.amg.digitalitzacio.documents.application;

import com.amg.digitalitzacio.documents.api.dto.ParsedDocumentResponse;
import com.amg.digitalitzacio.leads.application.LeadService;
import com.amg.digitalitzacio.leads.api.dto.LeadRequest;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final String ANTHROPIC_BASE    = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL             = "claude-haiku-4-5-20251001";

    private final SystemConfigService sysConfig;
    private final LeadService leadService;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public ParsedDocumentResponse parseDocument(UUID tenantId, MultipartFile file,
                                                boolean autoCreateLead, UserPrincipal principal) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String rawText;

        try (InputStream is = file.getInputStream()) {
            String contentType = file.getContentType();
            if (contentType != null && contentType.contains("pdf")) {
                rawText = extractPdfText(is);
            } else {
                // txt / plain text
                rawText = new String(file.getBytes());
            }
        } catch (Exception e) {
            log.error("[Documents] Error llegint fitxer {}: {}", fileName, e.getMessage());
            return new ParsedDocumentResponse(fileName, "", null, null, null, null, null, false, null);
        }

        if (rawText.isBlank()) {
            return new ParsedDocumentResponse(fileName, rawText, null, null, null, null, null, false, null);
        }

        // Limitem a 3000 caràcters per no enviar massa tokens
        String textForAI = rawText.length() > 3000 ? rawText.substring(0, 3000) + "…" : rawText;

        String[] extracted = extractWithAI(textForAI);
        String name  = extracted[0];
        String email = extracted[1];
        String phone = extracted[2];
        String notes = extracted[3];

        // Fallback regex si la IA no respon
        if (email == null || email.isBlank()) email = extractEmail(rawText);
        if (phone == null || phone.isBlank()) phone = extractPhone(rawText);

        String leadId = null;
        boolean created = false;

        if (autoCreateLead && (name != null || email != null || phone != null)) {
            try {
                String safeName = (name != null && !name.isBlank()) ? name : "Lead des de document";
                var req = new LeadRequest(safeName, email, phone, LeadSource.OTHER,
                        null, null, notes, null, null);
                var resp = leadService.createLead(req, principal);
                leadId  = resp.id().toString();
                created = true;
            } catch (Exception e) {
                log.warn("[Documents] No s'ha pogut crear lead des del document: {}", e.getMessage());
            }
        }

        return new ParsedDocumentResponse(
            fileName, rawText, name, email, phone, notes,
            "DOCUMENT", created, leadId
        );
    }

    private String extractPdfText(InputStream is) throws Exception {
        byte[] bytes = is.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String[] extractWithAI(String text) {
        String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return new String[4];

        try {
            String prompt = """
                Extreu del text següent: nom complet, email, telèfon i una nota breu (màx 100 car.).
                Respon ONLY en format JSON: {"name":"...","email":"...","phone":"...","notes":"..."}
                Si no trobes algun camp deixa'l "".
                Text:
                """ + text;

            var body = Map.of(
                "model", MODEL,
                "max_tokens", 300,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            var rc  = restClientBuilder.baseUrl(ANTHROPIC_BASE).build();
            var raw = rc.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            String content = objectMapper.readTree(raw).path("content").path(0).path("text").asText("");
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start < 0 || end < 0) return new String[4];

            JsonNode json = objectMapper.readTree(content.substring(start, end + 1));
            return new String[]{
                json.path("name").asText(null),
                json.path("email").asText(null),
                json.path("phone").asText(null),
                json.path("notes").asText(null)
            };
        } catch (Exception e) {
            log.warn("[Documents] AI extraction failed: {}", e.getMessage());
            return new String[4];
        }
    }

    private String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractPhone(String text) {
        Matcher m = Pattern.compile("(?:\\+34|0034)?[\\s.-]?[6789]\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{3}").matcher(text);
        return m.find() ? m.group().replaceAll("[\\s.-]", "") : null;
    }
}
