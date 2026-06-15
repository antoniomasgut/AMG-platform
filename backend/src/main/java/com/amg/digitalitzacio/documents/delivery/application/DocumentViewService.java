package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocumentRepository;
import com.amg.digitalitzacio.documents.delivery.api.dto.AcceptRequest;
import com.amg.digitalitzacio.documents.delivery.api.dto.DocumentViewResponse;
import com.amg.digitalitzacio.documents.delivery.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentViewService {

    private final SecureDocumentTokenRepository tokenRepo;
    private final SecureDocumentAuditRepository auditRepo;
    private final GeneratedDocumentRepository generatedDocumentRepo;
    private final TenantChatLinkRepository chatLinkRepo;
    private final TelegramBotClient telegramBotClient;

    @Transactional
    public DocumentViewResponse view(String tokenValue, String ip, String ua) {
        var token = resolveToken(tokenValue);
        if (token == null) return null;

        String htmlContent = resolveHtml(token);
        audit(token, AuditEventType.DOCUMENT_VIEWED, ip, ua);

        return new DocumentViewResponse(
            token.getFileName(),
            token.getDescription(),
            token.getDocumentType().name(),
            htmlContent,
            token.getDocumentType().hasAcceptAction(),
            token.getAcceptedAt() != null,
            token.getAcceptedAt(),
            token.getSignerName()
        );
    }

    @Transactional
    public void accept(String tokenValue, AcceptRequest req, String ip) {
        var token = resolveToken(tokenValue);
        if (token == null) throw new ResourceNotFoundException("Document no accessible");

        if (!token.getDocumentType().hasAcceptAction()) {
            throw new IllegalStateException("Aquest tipus de document no admet acceptació");
        }
        if (token.getAcceptedAt() != null) {
            throw new IllegalStateException("El document ja ha estat acceptat");
        }

        token.setAcceptedAt(Instant.now());
        token.setSignerName(req.signerName());
        token.setSignerIp(ip);
        tokenRepo.save(token);

        // Actualitza l'estat del GeneratedDocument font si existeix
        if (token.getSourceEntityType() == SourceEntityType.GENERATED_DOCUMENT
                && token.getSourceEntityId() != null) {
            generatedDocumentRepo.findById(token.getSourceEntityId()).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.ACCEPTED);
                generatedDocumentRepo.save(doc);
            });
        }

        audit(token, AuditEventType.DOCUMENT_ACCEPTED, ip, null);
        log.info("[SecureDoc] Document {} acceptat per '{}' des de {}", token.getId(), req.signerName(), ip);

        notifyTenantAcceptance(token, req.signerName());
    }

    private void notifyTenantAcceptance(SecureDocumentToken token, String signerName) {
        try {
            var chatLink = chatLinkRepo.findByTenantId(token.getTenantId()).orElse(null);
            if (chatLink == null || chatLink.getTelegramChatId() == null) return;

            String msg = String.format(
                "✅ <b>%s</b> ha acceptat el pressupost <b>%s</b>.",
                signerName, token.getFileName().replace(".pdf", "")
            );
            telegramBotClient.sendMessageForTenant(token.getTenantId(), chatLink.getTelegramChatId(), msg);
        } catch (Exception e) {
            log.warn("[SecureDoc] No s'ha pogut enviar notificació Telegram al tenant {}: {}",
                token.getTenantId(), e.getMessage());
        }
    }

    private SecureDocumentToken resolveToken(String tokenValue) {
        var opt = tokenRepo.findByToken(tokenValue);
        if (opt.isEmpty()) return null;
        var token = opt.get();
        if (!token.isAccessible()) return null;
        return token;
    }

    private String resolveHtml(SecureDocumentToken token) {
        if (token.getSourceEntityType() == SourceEntityType.GENERATED_DOCUMENT
                && token.getSourceEntityId() != null) {
            return generatedDocumentRepo.findById(token.getSourceEntityId())
                .map(doc -> doc.getHtmlContent())
                .orElse(null);
        }
        return null;
    }

    private void audit(SecureDocumentToken token, AuditEventType eventType, String ip, String ua) {
        var audit = new SecureDocumentAudit();
        audit.setTokenId(token.getId());
        audit.setTenantId(token.getTenantId());
        audit.setEventType(eventType);
        audit.setIpAddress(ip);
        audit.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
        auditRepo.save(audit);
    }
}
