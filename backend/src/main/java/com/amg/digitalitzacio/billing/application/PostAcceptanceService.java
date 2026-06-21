package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.SectorKbSeederService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntake;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.booking.application.BookingService;
import com.amg.digitalitzacio.documents.delivery.domain.TenantDocumentPreferencesRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Accions automàtiques post-acceptació d'un pressupost NexeLocal:
 * 1. Telegram a l'equip AMG (vendes)
 * 2. Email de benvinguda al client
 * 3. Creació automàtica de la fitxa de configuració (intake)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAcceptanceService {

    private final TenantRepository tenantRepository;
    private final LeadRepository leadRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final EmailService emailService;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService systemConfigService;
    private final BookingService bookingService;
    private final TenantDocumentPreferencesRepository tenantDocumentPreferencesRepository;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final TenantChatLinkRepository chatLinkRepository;
    private final SectorKbSeederService sectorKbSeederService;

    public void onSetupPaymentRequested(Budget budget, String paymentUrl) {
        try {
            var tenant = tenantRepository.findById(budget.getTenantId()).orElse(null);
            String clientName  = tenant != null && tenant.getName() != null ? tenant.getName() : (tenant != null ? tenant.getEmail() : "client");
            String clientEmail = tenant != null ? tenant.getEmail() : null;

            String tenantUrl = "https://amgdl.com/portal/admin/tenants/" + budget.getTenantId();
            String msg = """
                    💳 <b>Pressupost acceptat — pendent de pagament</b>
                    👤 %s
                    💰 %s €
                    🔗 <a href="%s">Enllaç de pagament →</a>
                    🔧 <a href="%s">Veure tenant →</a>
                    """.formatted(clientName,
                    budget.getTotal() != null ? budget.getTotal().toPlainString() : "—",
                    paymentUrl, tenantUrl);
            sendToSalesChat(msg);

            if (clientEmail != null && !clientEmail.isBlank()) {
                String subject = "Completa el pagament del teu servei AMG Digitalització";
                String body = """
                        Hola %s,

                        Has acceptat el pressupost. Per activar el teu servei, completa el pagament del setup:

                        %s

                        L'import és de %s €. Podràs pagar amb targeta de crèdit o Bizum.

                        Un cop completat el pagament, activarem el teu servei en les properes 24 hores.

                        Gràcies!
                        L'equip d'AMG Digitalització
                        """.formatted(clientName,
                        paymentUrl,
                        budget.getTotal() != null ? budget.getTotal().toPlainString() : "—");
                try {
                    emailService.sendEmail(clientEmail, subject, body);
                } catch (Exception e) {
                    log.warn("[PostAcceptance] No s'ha pogut enviar email de pagament a {}: {}", clientEmail, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[PostAcceptance] Error en onSetupPaymentRequested budget {}: {}", budget.getId(), e.getMessage());
        }
    }

    public void onBudgetAccepted(Budget budget) {
        if (!isEnabled("AMG_NOTIFY_BUDGET_ACCEPTED")) return;
        try {
            var tenant = tenantRepository.findById(budget.getTenantId()).orElse(null);
            if (tenant == null) return;

            String clientName  = tenant.getName() != null ? tenant.getName() : tenant.getEmail();
            String clientEmail = tenant.getEmail();
            String sector      = budget.getSector() != null ? budget.getSector() : "—";
            String total       = budget.getTotal() != null ? budget.getTotal() + " €" : "—";

            var intake = ensureIntake(budget, tenant.getName());
            String intakeUrl = "https://amgdl.com/setup-intake/" + intake.getToken();
            String statusUrl  = "https://amgdl.com/ca/status/" + intake.getToken();

            notifyAmgTeam(budget, clientName, sector, total, intakeUrl);

            if (clientEmail != null && !clientEmail.isBlank()) {
                sendWelcomeEmail(clientEmail, clientName, intakeUrl, statusUrl);
            }

            // F3→F2: si el tenant té F2 activa i autoBookingOnAccept, enviar invitació de reserva
            tryCreateBookingInvitation(budget, tenant, clientEmail, clientName);

        } catch (Exception e) {
            log.error("[PostAcceptance] Error processant post-acceptació budget {}: {}", budget.getId(), e.getMessage());
        }
    }

    public void onLeadCreated(UUID tenantId, String leadName, String contact, String source) {
        if (!isEnabled("AMG_NOTIFY_LEAD_CREATED")) return;
        try {
            String tenantName = resolveTenantName(tenantId);
            String msg = """
                    👤 <b>Nou lead</b> — %s
                    📛 %s
                    📞 %s
                    📡 Font: %s
                    🔗 <a href="https://amgdl.com/portal/admin/leads">Veure leads →</a>
                    """.formatted(tenantName, leadName, contact, source != null ? source : "—");
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant nou lead tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onBudgetSent(UUID tenantId, String clientName, BigDecimal total, UUID budgetId) {
        if (!isEnabled("AMG_NOTIFY_BUDGET_SENT")) return;
        try {
            String tenantName = resolveTenantName(tenantId);
            String msg = """
                    📋 <b>Pressupost enviat</b> — %s
                    👤 %s
                    💰 %s €
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(tenantName, clientName != null ? clientName : "—",
                    total != null ? total.toPlainString() : "—", tenantId);
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant pressupost enviat tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onPaymentReceived(UUID tenantId, BigDecimal amount, String reference, String method) {
        if (!isEnabled("AMG_NOTIFY_PAYMENT_RECEIVED")) return;
        try {
            String tenantName = resolveTenantName(tenantId);
            String msg = """
                    💳 <b>Pagament rebut</b> — %s
                    💰 %s €
                    🏦 Via: %s
                    🔖 Ref: %s
                    """.formatted(tenantName, amount != null ? amount.toPlainString() : "—",
                    method != null ? method : "—", reference != null ? reference : "—");
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant pagament tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onAgentError(UUID tenantId, String errorMsg) {
        if (!isEnabled("AMG_NOTIFY_AGENT_ERROR")) return;
        try {
            String tenantName = resolveTenantName(tenantId);
            String msg = """
                    🚨 <b>Error d'agent</b> — %s
                    ❌ %s
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(tenantName,
                    errorMsg != null ? errorMsg.substring(0, Math.min(errorMsg.length(), 200)) : "—",
                    tenantId);
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant error d'agent tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onContactForm(UUID tenantId, String landingTitle, String name, String phone, String message) {
        if (!isEnabled("AMG_NOTIFY_CONTACT_FORM")) return;
        try {
            String tenantName = resolveTenantName(tenantId);
            String msg = """
                    📬 <b>Formulari de contacte</b> — %s
                    🌐 Landing: %s
                    👤 %s — %s
                    💬 %s
                    """.formatted(tenantName,
                    landingTitle != null ? landingTitle : "—",
                    name != null ? name : "—",
                    phone != null ? phone : "—",
                    message != null ? message.substring(0, Math.min(message.length(), 150)) : "—");
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant formulari de contacte tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onSetupCompleted(UUID tenantId, String tenantName, String sector, String intakeUrl) {
        // Sembrar la KB amb contingut base del sector mentre l'equip prepara la implementació
        try {
            sectorKbSeederService.seedIfEmpty(tenantId, sector);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error sembrant KB sector={} tenant={}: {}", sector, tenantId, e.getMessage());
        }
        try {
            String tenantUrl = "https://amgdl.com/portal/admin/tenants/" + tenantId + "/wizard";
            String msg = """
                    🔔 <b>Setup completat</b> — %s
                    🏢 Sector: %s
                    📋 <a href="%s">Fitxa de configuració →</a>
                    🔧 <a href="%s">Iniciar implementació →</a>
                    ⚠️ Acció requerida en les pròximes 24h
                    """.formatted(
                    tenantName != null ? tenantName : "Desconegut",
                    sector != null ? sector : "—",
                    intakeUrl, tenantUrl);
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant setup completat tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onGoLive(UUID tenantId) {
        try {
            var tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return;

            String clientName  = tenant.getName() != null ? tenant.getName() : tenant.getEmail();
            String clientEmail = tenant.getEmail();
            String clientPhone = tenant.getPhone();
            String portalUrl   = "https://amgdl.com/ca/login";

            // Notificació interna a l'equip
            String internalMsg = """
                    🚀 <b>Tenant activat!</b> — %s
                    🔧 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(clientName, tenantId);
            sendToSalesChat(internalMsg);

            // Missatge d'onboarding al client
            String onboardingMsg = """
                    Hola %s! 🎉

                    El teu agent IA ja està actiu i funcionant.

                    Pots accedir al teu panel de gestió aquí:
                    %s

                    Durant els propers dies t'enviarem alguns consells per treure'n el màxim profit.

                    Si tens qualsevol dubte, escriu-nos per WhatsApp i t'ajudem de seguida.

                    L'equip d'AMG Digitalitzacions
                    """.formatted(clientName != null ? clientName : "client", portalUrl);

            var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
            boolean sent = false;

            if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                    && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()
                    && clientPhone != null && !clientPhone.isBlank()) {
                whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), clientPhone, onboardingMsg);
                sent = true;
            } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                    && !chatLink.getWhatsappPhoneNumber().isBlank()
                    && clientPhone != null && !clientPhone.isBlank()) {
                whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), clientPhone, onboardingMsg);
                sent = true;
            }

            if (!sent && clientEmail != null && !clientEmail.isBlank()) {
                emailService.sendEmail(clientEmail, "El teu agent ja està actiu! 🚀", onboardingMsg);
            }

        } catch (Exception e) {
            log.error("[PostAcceptance] Error en onGoLive tenant={}: {}", tenantId, e.getMessage());
        }
    }

    public void onSetupSlaExpired(com.amg.digitalitzacio.billing.domain.BudgetSetupIntake intake, long hoursWaiting) {
        try {
            String tenantUrl = "https://amgdl.com/portal/admin/tenants/" + intake.getTenantId() + "/wizard";
            String msg = """
                    ⚠️ <b>SLA expirat — Setup sense activar</b>
                    👤 %s · %s
                    ⏱ Fa %d hores esperant implementació
                    🔧 <a href="%s">Iniciar implementació →</a>
                    """.formatted(
                    intake.getTenantName() != null ? intake.getTenantName() : "Desconegut",
                    intake.getSector() != null ? intake.getSector() : "—",
                    hoursWaiting, tenantUrl);
            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant SLA expirat tenant={}: {}", intake.getTenantId(), e.getMessage());
        }
    }

    public void onBudgetRejected(Budget budget, String reason) {
        try {
            var tenant = tenantRepository.findById(budget.getTenantId()).orElse(null);
            String clientName = tenant != null ? tenant.getName() : "Desconegut";
            String total      = budget.getTotal() != null ? budget.getTotal() + " €" : "—";

            String msg = """
                    ❌ <b>Pressupost rebutjat</b>
                    👤 %s
                    💰 %s
                    📋 Motiu: %s
                    🔗 <a href="https://amgdl.com/portal/billing">Veure pressupostos →</a>
                    """.formatted(clientName, total, reason != null ? reason : "no especificat");

            sendToSalesChat(msg);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error notificant rebuig budget {}: {}", budget.getId(), e.getMessage());
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private void tryCreateBookingInvitation(Budget budget, Tenant tenant, String clientEmail, String clientName) {
        try {
            // Comprova si F2 (Agenda) està activa per al tenant
            if (!tenant.isPhaseActive("F2")) return;

            // Comprova les preferències: autoBookingOnAccept ha d'estar activat
            var prefs = tenantDocumentPreferencesRepository.findById(budget.getTenantId()).orElse(null);
            if (prefs != null && !prefs.isAutoBookingOnAccept()) return;

            // Crea el token de reserva vinculat al pressupost (sourceDocument)
            var bookingToken = bookingService.createTokenFromDocument(
                    budget.getTenantId(), clientEmail,
                    tenant.getPhone(), clientName,
                    budget.getId());

            String bookingUrl = "https://amgdl.com/ca/book/" + bookingToken.getToken();
            String missatge = "Hola " + (clientName != null ? clientName : "client") + "! "
                    + "Ja tenim el teu servei en preparació. El primer pas és reservar una reunió de configuració:\n"
                    + bookingUrl + "\n\nEscull el dia i hora que millor et vagi.";

            var chatLink = chatLinkRepository.findByTenantId(budget.getTenantId()).orElse(null);

            if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                    && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()
                    && tenant.getPhone() != null && !tenant.getPhone().isBlank()) {
                whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), tenant.getPhone(), missatge);
            } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                    && !chatLink.getWhatsappPhoneNumber().isBlank()
                    && tenant.getPhone() != null && !tenant.getPhone().isBlank()) {
                whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), tenant.getPhone(), missatge);
            } else if (clientEmail != null && !clientEmail.isBlank()) {
                emailService.sendEmail(clientEmail, "Reserva la teva primera reunió", missatge);
            }

        } catch (Exception e) {
            log.warn("[PostAcceptance] No s'ha pogut crear invitació de reserva per budget {}: {}", budget.getId(), e.getMessage());
        }
    }

    private BudgetSetupIntake ensureIntake(Budget budget, String tenantName) {
        // Re-check dins del mateix thread per evitar race condition de doble creació
        var existing = intakeRepository.findByBudgetId(budget.getId()).orElse(null);
        if (existing != null) return existing;
        return intakeRepository.findByBudgetId(budget.getId()).orElseGet(() -> {
            var intake = new BudgetSetupIntake();
            intake.setBudgetId(budget.getId());
            intake.setTenantId(budget.getTenantId());
            intake.setTenantName(tenantName);
            intake.setSector(budget.getSector());
            intake.setToken(UUID.randomUUID().toString().replace("-", ""));
            intake.setStatus("PENDING");
            intake.setCreatedAt(Instant.now());
            intake.setUpdatedAt(Instant.now());
            return intakeRepository.save(intake);
        });
    }

    private void notifyAmgTeam(Budget budget, String clientName, String sector, String total, String intakeUrl) {
        String tenantUrl = "https://amgdl.com/portal/admin/tenants/" + budget.getTenantId();
        String msg = """
                🎉 <b>Nou client!</b>
                👤 %s
                🏢 Sector: %s
                💰 %s
                📋 <a href="%s">Fitxa de configuració →</a>
                🔧 <a href="%s">Veure tenant →</a>
                """.formatted(clientName, sector, total, intakeUrl, tenantUrl);
        sendToSalesChat(msg);
    }

    private void sendWelcomeEmail(String to, String name, String intakeUrl, String statusUrl) {
        String subject = "Benvingut/da a AMG Digitalització 🎉";
        String body = """
                Hola %s,

                Hem rebut la confirmació del teu pressupost. Gràcies per confiar en nosaltres!

                En les properes 48 hores:
                ✅ Activarem el teu agent IA
                ✅ Configurarem els canals de comunicació
                ✅ T'enviarem accés al teu panel de gestió

                Per anar avançant, pots omplir ara la fitxa de configuració
                (triga uns 5 minuts i ens permet preparar-ho tot):

                %s

                Pots seguir l'estat del teu projecte en temps real aquí:

                %s

                Si tens qualsevol pregunta, respon a aquest correu o escriu-nos
                directament al WhatsApp i t'atendrem de seguida.

                Fins aviat!
                L'equip d'AMG Digitalització
                """.formatted(name != null ? name : "client", intakeUrl, statusUrl);
        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            log.warn("[PostAcceptance] No s'ha pogut enviar email de benvinguda a {}: {}", to, e.getMessage());
        }
    }

    private String resolveTenantName(UUID tenantId) {
        if (tenantId == null) return "Desconegut";
        return tenantRepository.findById(tenantId)
                .map(t -> t.getName() != null ? t.getName() : t.getEmail())
                .orElse("Desconegut");
    }

    private boolean isEnabled(String key) {
        try {
            String val = systemConfigService.get(key);
            return val == null || "true".equalsIgnoreCase(val.trim());
        } catch (Exception e) {
            return true;
        }
    }

    private void sendToSalesChat(String message) {
        try {
            String chatIdStr = systemConfigService.get("AMG_SALES_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) {
                log.warn("[PostAcceptance] AMG_SALES_CHAT_ID no configurat");
                return;
            }
            telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), message);
        } catch (Exception e) {
            log.warn("[PostAcceptance] Error enviant Telegram a vendes: {}", e.getMessage());
        }
    }
}
