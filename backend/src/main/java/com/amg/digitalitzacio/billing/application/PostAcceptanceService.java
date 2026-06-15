package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntake;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
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

            notifyAmgTeam(budget, clientName, sector, total, intakeUrl);

            if (clientEmail != null && !clientEmail.isBlank()) {
                sendWelcomeEmail(clientEmail, clientName, intakeUrl);
            }

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

    private BudgetSetupIntake ensureIntake(Budget budget, String tenantName) {
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

    private void sendWelcomeEmail(String to, String name, String intakeUrl) {
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

                Si tens qualsevol pregunta, respon a aquest correu o escriu-nos
                directament al WhatsApp i t'atendrem de seguida.

                Fins aviat!
                L'equip d'AMG Digitalització
                """.formatted(name != null ? name : "client", intakeUrl);
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
