package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.application.BookingService;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.comm.application.CommunicationTemplateService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocumentRepository;
import com.amg.digitalitzacio.documents.delivery.api.dto.AcceptRequest;
import com.amg.digitalitzacio.documents.delivery.api.dto.DocumentViewResponse;
import com.amg.digitalitzacio.documents.delivery.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import com.amg.digitalitzacio.payments.application.TenantStripeCheckoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentViewService {

    private final SecureDocumentTokenRepository tokenRepo;
    private final SecureDocumentAuditRepository auditRepo;
    private final GeneratedDocumentRepository generatedDocumentRepo;
    private final TenantChatLinkRepository chatLinkRepo;
    private final TelegramBotClient telegramBotClient;
    private final TenantDocumentPreferencesService preferencesService;
    private final BookingService bookingService;
    private final TenantRepository tenantRepo;
    private final CommunicationTemplateService templateService;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppWabaConfigRepository wabaConfigRepo;
    private final EmailService emailService;
    private final SystemConfigService sysConfig;
    private final NexeServiceConfigService nexeConfigService;
    private final TenantStripeCheckoutService tenantStripeCheckoutService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.api.base-url:https://api.amgdl.com}")
    private String apiBaseUrl;

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

    /** @return URL de pagament de Stripe si el tenant té el cobrament online actiu; null si no */
    @Transactional
    public String accept(String tokenValue, AcceptRequest req, String ip) {
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

        if (token.getSourceEntityType() == SourceEntityType.GENERATED_DOCUMENT
                && token.getSourceEntityId() != null) {
            generatedDocumentRepo.findById(token.getSourceEntityId()).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.ACCEPTED);
                generatedDocumentRepo.save(doc);
            });
        }

        audit(token, AuditEventType.DOCUMENT_ACCEPTED, ip, null);
        log.info("[SecureDoc] Document {} acceptat per '{}' des de {}", token.getId(), req.signerName(), ip);

        boolean bookingSent = false;
        var prefs = preferencesService.getPreferences(token.getTenantId());
        if (prefs.isAutoBookingOnAccept() && agendaIsActive(token.getTenantId())) {
            try {
                var bookingToken = bookingService.createTokenFromDocument(
                    token.getTenantId(),
                    token.getRecipientEmail(),
                    token.getRecipientPhone(),
                    token.getRecipientName(),
                    token.getId()
                );
                sendBookingInvitation(token, bookingToken, prefs);
                bookingSent = true;
            } catch (Exception e) {
                log.warn("[SecureDoc] Error enviant invitació de reserva per document {}: {}",
                    token.getId(), e.getMessage());
            }
        }

        // Mòdul 53: cobrament online opcional amb l'Stripe del tenant
        String paymentUrl = tryCreatePaymentCheckout(token);

        notifyTenantAcceptance(token, req.signerName(), bookingSent);
        return paymentUrl;
    }

    /**
     * Crea el checkout de pagament si la config PRESSUPOSTOS del tenant ho demana.
     * Mai llança: si Stripe no està operatiu, l'acceptació es completa sense pagament.
     */
    private String tryCreatePaymentCheckout(SecureDocumentToken token) {
        try {
            var configOpt = nexeConfigService.get(token.getTenantId(), "PRESSUPOSTOS");
            if (configOpt.isEmpty()) return null;
            var config = objectMapper.readTree(configOpt.get().getConfigJson());
            String mode = config.path("online_payment_mode").asText("OFF");
            if (!"FULL".equals(mode) && !"DEPOSIT".equals(mode)) return null;

            java.math.BigDecimal total = resolveDocumentTotal(token);
            if (total == null || total.signum() <= 0) return null;

            java.math.BigDecimal amount = total;
            String concept;
            String docName = token.getFileName() != null
                ? token.getFileName().replaceFirst("\\.pdf$", "") : "Pressupost";
            String businessName = tenantRepo.findById(token.getTenantId())
                .map(t -> t.getName() != null ? t.getName() : "").orElse("");
            if ("DEPOSIT".equals(mode)) {
                int pct = config.path("deposit_percent").asInt(30);
                pct = Math.max(5, Math.min(90, pct));
                amount = total.multiply(java.math.BigDecimal.valueOf(pct))
                        .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                concept = "Paga i senyal (" + pct + "%) — " + docName
                        + (businessName.isBlank() ? "" : " · " + businessName);
            } else {
                concept = docName + (businessName.isBlank() ? "" : " · " + businessName);
            }

            String appBase = sysConfig.get("APP_BASE_URL");
            if (appBase == null || appBase.isBlank()) appBase = "https://amgdl.com";
            String successUrl = apiBaseUrl + "/api/v1/documents/view/" + token.getToken()
                    + "/payment-return?session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = appBase + "/ca/documents/view/" + token.getToken();

            var checkout = tenantStripeCheckoutService.createCheckout(
                    token.getTenantId(), amount, concept, token.getId().toString(), successUrl, cancelUrl);
            if (checkout.isEmpty()) return null;

            token.setPaymentStatus("PENDING");
            token.setPaymentSessionId(checkout.get().sessionId());
            token.setPaymentAmount(amount);
            tokenRepo.save(token);
            log.info("[SecureDoc] Checkout de {} € creat per document {} (tenant {})",
                    amount, token.getId(), token.getTenantId());
            return checkout.get().url();
        } catch (Exception e) {
            log.warn("[SecureDoc] No s'ha pogut crear el cobrament online per document {}: {}",
                    token.getId(), e.getMessage());
            return null;
        }
    }

    /** Total del document des del JSON `calculated` del GeneratedDocument. */
    private java.math.BigDecimal resolveDocumentTotal(SecureDocumentToken token) {
        if (token.getSourceEntityType() != SourceEntityType.GENERATED_DOCUMENT
                || token.getSourceEntityId() == null) return null;
        return generatedDocumentRepo.findById(token.getSourceEntityId())
            .map(doc -> {
                try {
                    var calc = objectMapper.readTree(doc.getCalculated());
                    if (calc.has("total") && calc.get("total").isNumber()) {
                        return java.math.BigDecimal.valueOf(calc.get("total").asDouble());
                    }
                } catch (Exception ignored) {}
                return null;
            }).orElse(null);
    }

    /**
     * Retorn del checkout: verifica la sessió amb la clau del tenant i marca el pagament.
     * @return URL del frontend on redirigir (?paid=1|0). Idempotent.
     */
    @Transactional
    public String handlePaymentReturn(String tokenValue, String sessionId) {
        String appBase = sysConfig.get("APP_BASE_URL");
        if (appBase == null || appBase.isBlank()) appBase = "https://amgdl.com";
        var token = tokenRepo.findByToken(tokenValue).orElse(null);
        if (token == null) return appBase + "/ca/documents/view/" + tokenValue + "?paid=0";

        String docUrl = appBase + "/ca/documents/view/" + token.getToken();
        if ("PAID".equals(token.getPaymentStatus())) {
            return docUrl + "?paid=1"; // idempotent — ja processat
        }
        if (token.getPaymentSessionId() == null
                || !token.getPaymentSessionId().equals(sessionId)) {
            return docUrl + "?paid=0";
        }
        boolean paid = tenantStripeCheckoutService.isSessionPaid(token.getTenantId(), sessionId);
        if (!paid) return docUrl + "?paid=0";

        token.setPaymentStatus("PAID");
        token.setPaymentPaidAt(Instant.now());
        tokenRepo.save(token);
        log.info("[SecureDoc] Pagament confirmat: {} € del document {} (tenant {})",
                token.getPaymentAmount(), token.getId(), token.getTenantId());
        notifyTenantPayment(token);
        return docUrl + "?paid=1";
    }

    private void notifyTenantPayment(SecureDocumentToken token) {
        try {
            var chatLink = chatLinkRepo.findByTenantId(token.getTenantId()).orElse(null);
            if (chatLink == null || chatLink.getTelegramChatId() == null) return;
            String msg = "💳 <b>Pagament rebut!</b>\n"
                    + "Import: " + (token.getPaymentAmount() != null ? token.getPaymentAmount().toPlainString() : "—") + " €\n"
                    + "Document: " + (token.getFileName() != null ? token.getFileName() : "—") + "\n"
                    + "Client: " + (token.getRecipientName() != null ? token.getRecipientName() : "—");
            telegramBotClient.sendMessage(chatLink.getTelegramChatId(), msg);
        } catch (Exception e) {
            log.warn("[SecureDoc] Error notificant pagament al tenant {}: {}",
                    token.getTenantId(), e.getMessage());
        }
    }

    private boolean agendaIsActive(UUID tenantId) {
        return tenantRepo.findById(tenantId).map(t -> t.isPhaseActive("F2")).orElse(false);
    }

    private void sendBookingInvitation(SecureDocumentToken docToken, BookingToken bookingToken,
                                       TenantDocumentPreferences prefs) {
        String baseUrl = sysConfig.get("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://app.amgdl.com";
        String bookingUrl = baseUrl + "/book/" + bookingToken.getToken();

        String businessName = tenantRepo.findById(docToken.getTenantId())
            .map(t -> t.getName() != null ? t.getName() : "")
            .orElse("");

        String lang = prefs.getLanguage();
        String channel = prefs.getStandardDocChannel();
        String phone = docToken.getRecipientPhone();

        boolean sendWa = phone != null && !phone.isBlank()
            && ("WHATSAPP".equals(channel) || "BOTH".equals(channel));
        boolean sendEmail = "EMAIL".equals(channel) || "BOTH".equals(channel) || !sendWa;

        var vars = Map.of(
            "RECIPIENT_NAME", bookingToken.getLeadName() != null ? bookingToken.getLeadName() : "",
            "BUSINESS_NAME",  businessName,
            "BOOKING_URL",    bookingUrl,
            "DOCUMENT_NAME",  docToken.getFileName().replaceFirst("\\.pdf$", ""),
            "BOOKING_LABEL",  bookingToken.getBookingLabel() != null ? bookingToken.getBookingLabel() : "Reserva la teva cita"
        );

        if (sendWa) {
            sendBookingWhatsApp(docToken.getTenantId(), phone, vars, lang);
        }
        if (sendEmail) {
            String email = docToken.getRecipientEmail();
            if (email != null && !email.isBlank()) {
                sendBookingEmail(email, vars, lang, docToken.getTenantId());
            } else {
                log.warn("[SecureDoc] No s'ha pogut enviar invitació per email: destinatari sense email (doc={})",
                    docToken.getId());
            }
        }
    }

    private void sendBookingWhatsApp(UUID tenantId, String phone,
                                     Map<String, String> vars, String lang) {
        var tpl = templateService.resolveForTenant(tenantId, "WHATSAPP", "BOOKING_INVITATION", lang);
        String msg = tpl != null
            ? templateService.render(tpl.getBody(), vars)
            : String.format("Hola %s,\n\nPots concretar la data aquí:\n%s",
                vars.get("RECIPIENT_NAME"), vars.get("BOOKING_URL"));

        var wabaOpt = wabaConfigRepo.findByTenantId(tenantId);
        if (wabaOpt.isPresent() && wabaOpt.get().getPhoneNumberId() != null) {
            try {
                whatsAppMetaChannel.sendMessage(wabaOpt.get().getPhoneNumberId(), phone, msg);
                log.info("[SecureDoc] Invitació booking WhatsApp Meta enviada a {} (tenant={})", phone, tenantId);
                return;
            } catch (Exception e) {
                log.warn("[SecureDoc] WhatsApp Meta fallat (booking) per {}: {}", phone, e.getMessage());
            }
        }

        var chatLink = chatLinkRepo.findByTenantId(tenantId).orElse(null);
        String fromPhone = chatLink != null ? chatLink.getWhatsappPhoneNumber() : null;
        if (fromPhone != null && !fromPhone.isBlank()) {
            try {
                whatsAppChannel.sendMessage(fromPhone, phone, msg);
                log.info("[SecureDoc] Invitació booking WhatsApp Twilio enviada a {} (tenant={})", phone, tenantId);
            } catch (Exception e) {
                log.warn("[SecureDoc] WhatsApp Twilio fallat (booking) per {}: {}", phone, e.getMessage());
            }
        }
    }

    private void sendBookingEmail(String email, Map<String, String> vars,
                                  String lang, UUID tenantId) {
        var tpl = templateService.resolveForTenant(tenantId, "EMAIL", "BOOKING_INVITATION", lang);
        String subject;
        String body;
        if (tpl != null) {
            subject = tpl.getSubject() != null
                ? templateService.render(tpl.getSubject(), vars)
                : "Reserva la teva cita — " + vars.get("DOCUMENT_NAME");
            body = templateService.render(tpl.getBody(), vars);
        } else {
            subject = "Reserva la teva cita — " + vars.get("DOCUMENT_NAME");
            body = String.format("Hola %s,\n\nPots concretar la data aquí:\n%s",
                vars.get("RECIPIENT_NAME"), vars.get("BOOKING_URL"));
        }
        try {
            emailService.sendEmail(email, subject, body);
            log.info("[SecureDoc] Invitació booking email enviada a {} (tenant={})", email, tenantId);
        } catch (Exception e) {
            log.warn("[SecureDoc] Error enviant booking email a {}: {}", email, e.getMessage());
        }
    }

    private void notifyTenantAcceptance(SecureDocumentToken token, String signerName, boolean bookingSent) {
        try {
            var chatLink = chatLinkRepo.findByTenantId(token.getTenantId()).orElse(null);
            if (chatLink == null || chatLink.getTelegramChatId() == null) return;

            String bookingLine = bookingSent
                ? "\n📅 S'ha enviat l'enllaç de reserva automàticament."
                : "\n👉 Envia l'enllaç de reserva des del CRM quan estiguis a punt.";

            String msg = String.format(
                "✅ <b>%s</b> ha acceptat el pressupost <b>%s</b>.%s",
                signerName,
                token.getFileName().replace(".pdf", ""),
                bookingLine
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
