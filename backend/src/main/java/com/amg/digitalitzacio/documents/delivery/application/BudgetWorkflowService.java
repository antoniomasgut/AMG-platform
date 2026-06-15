package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.documents.builder.api.dto.GenerateRequest;
import com.amg.digitalitzacio.documents.builder.application.DocumentBuilderService;
import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;
import com.amg.digitalitzacio.documents.builder.domain.DocumentTemplateRepository;
import com.amg.digitalitzacio.documents.builder.domain.DocumentType;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocumentRepository;
import com.amg.digitalitzacio.documents.delivery.domain.DocumentSensitivity;
import com.amg.digitalitzacio.documents.delivery.domain.DocumentViewType;
import com.amg.digitalitzacio.documents.delivery.domain.SourceEntityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetWorkflowService {

    private static final String REDIS_PREFIX = "budget:approval:";
    private static final Duration APPROVAL_TTL = Duration.ofHours(24);
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(?:^|\\s)(\\+?[0-9][0-9\\s\\-]{7,14})(?:\\s|$)");

    private final DocumentBuilderService documentBuilderService;
    private final DocumentTemplateRepository templateRepo;
    private final GeneratedDocumentRepository documentRepo;
    private final SecureDocumentService secureDocumentService;
    private final TelegramBotClient telegramBotClient;
    private final TenantChatLinkRepository chatLinkRepo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * Processa la comanda /pressupost.
     * Format: /pressupost <email> <nom complet> [notes opcionals]
     * Retorna el missatge a enviar al tenant per Telegram.
     */
    @Transactional
    public String handleCommand(UUID tenantId, Long chatId, String commandText) {
        var parts = parseCommand(commandText);
        if (parts == null) {
            return "Format incorrecte.\nUsa: <code>/pressupost email@client.com Nom Cognom [notes]</code>";
        }

        String email = parts.email();
        String name = parts.name();
        String notes = parts.notes();

        // Cerca la primera plantilla de pressupost (quote) activa del tenant
        var templates = templateRepo.findByTenantIdAndDocumentTypeAndActiveTrue(tenantId, DocumentType.quote);
        if (templates.isEmpty()) {
            return "❌ No hi ha cap plantilla de pressupost activa. Crea'n una primer al portal.";
        }
        var template = templates.get(0);

        // Genera el document HTML (ràpid, sense PDF encara)
        var req = new GenerateRequest(
            template.getId(),
            null,
            Map.of("name", name, "email", email, "notes", notes != null ? notes : ""),
            Map.of(),
            List.of()
        );

        DocumentBuilderService.GeneratedDocumentSummary summary;
        try {
            summary = documentBuilderService.generateDocumentSummary(tenantId, req);
        } catch (Exception e) {
            log.error("[BudgetWorkflow] Error generant document per tenant={}: {}", tenantId, e.getMessage());
            return "❌ Error generant el pressupost: " + e.getMessage();
        }

        // Guarda estat d'aprovació a Redis
        var state = new ApprovalState(
            summary.documentId(), summary.documentNumber(),
            email, parts.phone(), name, notes, template.getId()
        );
        storeState(tenantId, chatId, state);

        String preview = summary.totalAmount() != null
            ? "\n💰 Total: <b>" + summary.totalAmount() + " €</b>" : "";

        return String.format(
            "📄 Pressupost <b>%s</b> generat per a <b>%s</b>.%s\n\n" +
            "Enviar al correu <code>%s</code>?\n\n" +
            "Respon <b>SI</b> per confirmar o <b>NO</b> per cancel·lar.",
            summary.documentNumber(), name, preview, email
        );
    }

    /**
     * Aprovació del tenant. Genera el PDF, crea el token segur i envia l'email al client.
     * Retorna el missatge de confirmació per Telegram.
     */
    @Transactional
    public String approve(UUID tenantId, Long chatId) {
        var state = loadState(tenantId, chatId);
        if (state == null) return null;

        try {
            // Genera PDF i guarda a storage
            String storageFileId = documentBuilderService.generateAndStorePdf(
                tenantId, state.documentId());

            String fileName = state.documentNumber() + ".pdf";

            // Crea token segur i envia per WhatsApp (si hi ha telèfon) + email
            secureDocumentService.issue(
                tenantId, null,
                storageFileId, fileName, "application/pdf",
                state.recipientEmail(), state.recipientPhone(), state.recipientName(),
                "Pressupost " + state.documentNumber(),
                DocumentSensitivity.STANDARD,
                DocumentViewType.BUDGET,
                state.documentId(),
                SourceEntityType.GENERATED_DOCUMENT
            );

            clearState(tenantId, chatId);

            log.info("[BudgetWorkflow] Pressupost {} enviat a {} per tenant={}",
                state.documentNumber(), state.recipientEmail(), tenantId);

            return String.format(
                "✅ Pressupost <b>%s</b> enviat a <b>%s</b>.\n\n" +
                "El client rebrà un email amb l'enllaç per veure'l i acceptar-lo.",
                state.documentNumber(), state.recipientEmail()
            );
        } catch (Exception e) {
            log.error("[BudgetWorkflow] Error aprovant pressupost per tenant={}: {}", tenantId, e.getMessage());
            return "❌ Error enviant el pressupost: " + e.getMessage();
        }
    }

    /**
     * Rebuig del tenant. Cancel·la el document generat.
     * Retorna el missatge de confirmació per Telegram.
     */
    @Transactional
    public String reject(UUID tenantId, Long chatId) {
        var state = loadState(tenantId, chatId);
        if (state == null) return null;

        documentRepo.findById(state.documentId()).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.CANCELLED);
            documentRepo.save(doc);
        });

        clearState(tenantId, chatId);
        log.info("[BudgetWorkflow] Pressupost {} cancel·lat per tenant={}", state.documentNumber(), tenantId);
        return "❌ Pressupost " + state.documentNumber() + " cancel·lat.";
    }

    public boolean hasPendingApproval(UUID tenantId, Long chatId) {
        return redis.hasKey(redisKey(tenantId, chatId));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void storeState(UUID tenantId, Long chatId, ApprovalState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(redisKey(tenantId, chatId), json,
                APPROVAL_TTL.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Error guardant estat d'aprovació", e);
        }
    }

    private ApprovalState loadState(UUID tenantId, Long chatId) {
        try {
            String json = redis.opsForValue().get(redisKey(tenantId, chatId));
            if (json == null) return null;
            return objectMapper.readValue(json, ApprovalState.class);
        } catch (Exception e) {
            log.error("[BudgetWorkflow] Error llegint estat Redis: {}", e.getMessage());
            return null;
        }
    }

    private void clearState(UUID tenantId, Long chatId) {
        redis.delete(redisKey(tenantId, chatId));
    }

    private String redisKey(UUID tenantId, Long chatId) {
        return REDIS_PREFIX + tenantId + ":" + chatId;
    }

    /**
     * Format: /pressupost email [+34phone] nom complet [notes opcionals]
     * El telèfon és opcional — permet enviar per WhatsApp si es proporciona.
     */
    private ParsedCommand parseCommand(String text) {
        String body = text.replaceFirst("(?i)/pressupost\\s*", "").trim();
        if (body.isBlank()) return null;

        var emailMatcher = EMAIL_PATTERN.matcher(body);
        if (!emailMatcher.find()) return null;

        String email = emailMatcher.group();
        String rest = body.substring(emailMatcher.end()).trim();
        if (rest.isBlank()) return null;

        // Telèfon opcional al principi de la resta
        String phone = null;
        var phoneMatcher = PHONE_PATTERN.matcher(" " + rest + " ");
        if (phoneMatcher.find()) {
            String candidate = phoneMatcher.group(1).replaceAll("[\\s\\-]", "");
            if (candidate.length() >= 9) {
                phone = candidate;
                rest = rest.replace(phoneMatcher.group(1).trim(), "").trim();
            }
        }

        if (rest.isBlank()) return null;

        String[] parts = rest.split("\\s{2,}|,", 2);
        String name = parts[0].trim();
        String notes = parts.length > 1 ? parts[1].trim() : null;

        if (name.isBlank()) return null;
        return new ParsedCommand(email, phone, name, notes);
    }

    public record ApprovalState(
        UUID documentId,
        String documentNumber,
        String recipientEmail,
        String recipientPhone,
        String recipientName,
        String notes,
        UUID templateId
    ) {}

    private record ParsedCommand(String email, String phone, String name, String notes) {}
}
