package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.comm.application.CommunicationTemplateService;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import com.amg.digitalitzacio.documents.delivery.domain.*;
import com.amg.digitalitzacio.documents.delivery.domain.DocumentViewType;
import com.amg.digitalitzacio.documents.delivery.domain.SourceEntityType;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.shared.storage.StorageProviderRouter;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecureDocumentService {

    private final SecureDocumentTokenRepository tokenRepo;
    private final SecureDocumentAuditRepository auditRepo;
    private final StorageProviderRouter storageRouter;
    private final EmailService emailService;
    private final SystemConfigService sysConfig;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppWabaConfigRepository wabaConfigRepo;
    private final TenantChatLinkRepository chatLinkRepo;
    private final TenantDocumentPreferencesService preferencesService;
    private final CommunicationTemplateService templateService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a les' HH:mm")
            .withZone(ZoneId.of("Europe/Madrid"))
            .withLocale(new java.util.Locale("ca"));

    @Transactional
    public SecureDocumentToken issue(UUID tenantId, UUID createdBy,
                                     String storageFileId, String fileName, String mimeType,
                                     String recipientEmail, String recipientPhone, String recipientName,
                                     String description, DocumentSensitivity sensitivity,
                                     DocumentViewType documentType, UUID sourceEntityId,
                                     SourceEntityType sourceEntityType) {
        var token = new SecureDocumentToken();
        token.setToken(generateToken());
        token.setTenantId(tenantId);
        token.setCreatedBy(createdBy);
        token.setStorageFileId(storageFileId);
        token.setFileName(fileName);
        token.setMimeType(mimeType);
        token.setSensitivity(sensitivity);
        token.setRecipientEmail(recipientEmail);
        token.setRecipientPhone(recipientPhone);
        token.setRecipientName(recipientName);
        token.setDescription(description);
        token.setExpiresAt(Instant.now().plus(sensitivity.ttl));
        token.setMaxDownloads(sensitivity.maxDownloads);
        token.setDocumentType(documentType != null ? documentType : DocumentViewType.GENERIC);
        token.setSourceEntityId(sourceEntityId);
        token.setSourceEntityType(sourceEntityType);

        tokenRepo.save(token);

        String baseUrl = resolveBaseUrl();
        String viewUrl = baseUrl + "/documents/view/" + token.getToken();

        // Carrega preferències del tenant
        var prefs = preferencesService.getPreferences(tenantId);
        String lang = prefs.getLanguage();
        String action = sensitivity == DocumentSensitivity.SENSITIVE
            ? "DOCUMENT_SENSITIVE_DELIVERY"
            : "DOCUMENT_STANDARD_DELIVERY";
        String effectiveChannel = sensitivity == DocumentSensitivity.SENSITIVE
            ? prefs.getSensitiveDocChannel()
            : prefs.getStandardDocChannel();

        boolean sendWa = recipientPhone != null && !recipientPhone.isBlank()
            && ("WHATSAPP".equals(effectiveChannel) || "BOTH".equals(effectiveChannel));
        boolean sendEmail = "EMAIL".equals(effectiveChannel) || "BOTH".equals(effectiveChannel)
            || (!sendWa);  // email sempre si no s'envia per WhatsApp

        if (sendWa) {
            sendViaWhatsApp(tenantId, recipientPhone, token, viewUrl, action, lang);
        }
        if (sendEmail) {
            sendNotificationEmail(token, viewUrl, action, lang);
            token.setEmailSentAt(Instant.now());
        }
        tokenRepo.save(token);

        log.info("[SecureDoc] Token emès per tenant={} destinatari={} telèfon={} sensibilitat={}",
            tenantId, recipientEmail, recipientPhone, sensitivity);
        return token;
    }

    /** Valida el token i retorna l'InputStream del fitxer. Llança excepcions si no és accessible. */
    @Transactional
    public DownloadResult validateAndStream(String tokenValue, String ipAddress, String userAgent) {
        var tokenOpt = tokenRepo.findByToken(tokenValue);
        if (tokenOpt.isEmpty()) {
            log.warn("[SecureDoc] Token no trobat: {}", tokenValue);
            return null;
        }
        var token = tokenOpt.get();

        if (token.isRevoked()) {
            audit(token, AuditEventType.DOWNLOAD_REVOKED, ipAddress, userAgent);
            return null;
        }
        if (token.isExpired()) {
            audit(token, AuditEventType.DOWNLOAD_EXPIRED, ipAddress, userAgent);
            return null;
        }
        if (token.isDownloadsExhausted()) {
            audit(token, AuditEventType.DOWNLOAD_EXHAUSTED, ipAddress, userAgent);
            return null;
        }

        try {
            InputStream stream = storageRouter.getProvider(token.getTenantId())
                .download(token.getStorageFileId());

            token.setDownloadCount(token.getDownloadCount() + 1);
            Instant now = Instant.now();
            if (token.getFirstDownloadedAt() == null) token.setFirstDownloadedAt(now);
            token.setLastDownloadedAt(now);
            tokenRepo.save(token);

            audit(token, AuditEventType.DOWNLOAD_OK, ipAddress, userAgent);

            return new DownloadResult(stream, token.getFileName(), token.getMimeType());
        } catch (Exception e) {
            log.error("[SecureDoc] Error recuperant fitxer per token {}: {}", tokenValue, e.getMessage());
            audit(token, AuditEventType.DOWNLOAD_ERROR, ipAddress, userAgent);
            throw new RuntimeException("Error retrieving document", e);
        }
    }

    @Transactional
    public void revoke(UUID tenantId, UUID tokenId) {
        var token = tokenRepo.findById(tokenId)
            .filter(t -> t.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        tokenRepo.save(token);
        log.info("[SecureDoc] Token {} revocat per tenant={}", tokenId, tenantId);
    }

    @Transactional(readOnly = true)
    public Page<SecureDocumentToken> list(UUID tenantId, Pageable pageable) {
        return tokenRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public SecureDocumentToken getToken(UUID tenantId, UUID tokenId) {
        return tokenRepo.findById(tokenId)
            .filter(t -> t.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
    }

    @Transactional(readOnly = true)
    public List<SecureDocumentAudit> getAudit(UUID tenantId, UUID tokenId) {
        tokenRepo.findById(tokenId)
            .filter(t -> t.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        return auditRepo.findByTokenIdOrderByOccurredAtDesc(tokenId);
    }

    private void sendViaWhatsApp(UUID tenantId, String phone, SecureDocumentToken token,
                                 String viewUrl, String action, String lang) {
        String msg = renderTemplate(tenantId, "WHATSAPP", action, lang, token, viewUrl);
        if (msg == null) {
            // Fallback inline si no hi ha plantilla
            String docLabel = token.getDescription() != null ? token.getDescription() : token.getFileName();
            String recipientDisplay = token.getRecipientName() != null ? token.getRecipientName() : phone;
            msg = String.format("Hola %s,\n\nel teu document *%s* ja està disponible:\n%s",
                recipientDisplay, docLabel, viewUrl);
        }

        // Prova Meta WhatsApp (per-tenant) primer
        var wabaOpt = wabaConfigRepo.findByTenantId(tenantId);
        if (wabaOpt.isPresent() && wabaOpt.get().getPhoneNumberId() != null) {
            try {
                whatsAppMetaChannel.sendMessage(wabaOpt.get().getPhoneNumberId(), phone, msg);
                log.info("[SecureDoc] WhatsApp Meta enviat a {} per tenant={}", phone, tenantId);
                return;
            } catch (Exception e) {
                log.warn("[SecureDoc] WhatsApp Meta fallat per a {}: {}", phone, e.getMessage());
            }
        }

        // Fallback: Twilio WhatsApp global
        var chatLink = chatLinkRepo.findByTenantId(tenantId).orElse(null);
        String fromPhone = chatLink != null ? chatLink.getWhatsappPhoneNumber() : null;
        if (fromPhone != null && !fromPhone.isBlank()) {
            try {
                whatsAppChannel.sendMessage(fromPhone, phone, msg);
                log.info("[SecureDoc] WhatsApp Twilio enviat a {} per tenant={}", phone, tenantId);
            } catch (Exception e) {
                log.warn("[SecureDoc] WhatsApp Twilio fallat per a {}: {}", phone, e.getMessage());
            }
        }
    }

    private void sendNotificationEmail(SecureDocumentToken token, String viewUrl,
                                       String action, String lang) {
        String body = renderTemplate(token.getTenantId(), "EMAIL", action, lang, token, viewUrl);
        String subject = resolveSubject(token.getTenantId(), action, lang, token);
        if (body == null) {
            String recipientDisplay = token.getRecipientName() != null ? token.getRecipientName() : token.getRecipientEmail();
            body = String.format("Hola %s,\n\nEl teu document \"%s\" ja és disponible:\n%s",
                recipientDisplay,
                token.getDescription() != null ? token.getDescription() : token.getFileName(),
                viewUrl);
            subject = "El teu document està disponible";
        }
        try {
            emailService.sendEmail(token.getRecipientEmail(), subject, body);
            audit(token, AuditEventType.EMAIL_SENT, null, null);
        } catch (Exception e) {
            log.error("[SecureDoc] Error enviant email a {}: {}", token.getRecipientEmail(), e.getMessage());
        }
    }

    private String renderTemplate(UUID tenantId, String channel, String action, String lang,
                                  SecureDocumentToken token, String viewUrl) {
        var tpl = templateService.resolveForTenant(tenantId, channel, action, lang);
        if (tpl == null) return null;
        String recipientDisplay = token.getRecipientName() != null ? token.getRecipientName() : token.getRecipientEmail();
        var vars = java.util.Map.of(
            "RECIPIENT_NAME",  recipientDisplay,
            "DOCUMENT_NAME",   token.getDescription() != null ? token.getDescription() : token.getFileName(),
            "VIEW_URL",        viewUrl,
            "EXPIRES_AT",      DATE_FMT.format(token.getExpiresAt()),
            "DOCUMENT_NUMBER", token.getFileName().replaceFirst("\\.pdf$", "")
        );
        return templateService.render(tpl.getBody(), vars);
    }

    private String resolveSubject(UUID tenantId, String action, String lang, SecureDocumentToken token) {
        var tpl = templateService.resolveForTenant(tenantId, "EMAIL", action, lang);
        if (tpl == null || tpl.getSubject() == null) return "El teu document està disponible";
        String recipientDisplay = token.getRecipientName() != null ? token.getRecipientName() : token.getRecipientEmail();
        return templateService.render(tpl.getSubject(), java.util.Map.of(
            "RECIPIENT_NAME", recipientDisplay,
            "DOCUMENT_NAME",  token.getDescription() != null ? token.getDescription() : token.getFileName()
        ));
    }

    private String resolveBaseUrl() {
        String base = sysConfig.get("APP_BASE_URL");
        return (base != null && !base.isBlank()) ? base : "https://app.amgdl.com";
    }

    private void audit(SecureDocumentToken token, AuditEventType eventType,
                       String ipAddress, String userAgent) {
        var audit = new SecureDocumentAudit();
        audit.setTokenId(token.getId());
        audit.setTenantId(token.getTenantId());
        audit.setEventType(eventType);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent != null && userAgent.length() > 500
            ? userAgent.substring(0, 500) : userAgent);
        auditRepo.save(audit);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record DownloadResult(InputStream stream, String fileName, String mimeType) {}
}
