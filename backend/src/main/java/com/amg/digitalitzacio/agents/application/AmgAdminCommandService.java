package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmgAdminCommandService {

    private final SystemConfigService sysConfig;
    private final LeadRepository leadRepository;
    private final TenantRepository tenantRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final TelegramBotClient telegramBotClient;

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Europe/Madrid"));

    /** Retorna true si el chatId és un dels xats AMG admin configurats */
    public boolean isAdminChat(Long chatId) {
        if (chatId == null) return false;
        String salesChatId = sysConfig.get("AMG_SALES_CHAT_ID");
        String infraChatId = sysConfig.get("TELEGRAM_CHAT_ID");
        String chatStr = String.valueOf(chatId);
        return chatStr.equals(salesChatId) || chatStr.equals(infraChatId);
    }

    /** Processa la comanda i envia la resposta al xat */
    public void handle(Long chatId, String text) {
        String cmd = text.trim().toLowerCase();
        // Elimina el sufix @nombot si present
        if (cmd.contains("@")) cmd = cmd.substring(0, cmd.indexOf("@"));

        String response;
        if (cmd.equals("/ajuda") || cmd.equals("/help") || cmd.equals("/start")) {
            response = helpText();
        } else if (cmd.equals("/pipeline") || cmd.equals("/kanban")) {
            response = pipelineStats();
        } else if (cmd.equals("/nous") || cmd.equals("/new")) {
            response = newLeads();
        } else if (cmd.equals("/leads")) {
            response = recentLeads();
        } else if (cmd.equals("/pressupostos") || cmd.equals("/budgets")) {
            response = pendingBudgets();
        } else if (cmd.equals("/tenants") || cmd.equals("/clients")) {
            response = activeTenantsText();
        } else if (cmd.equals("/stats")) {
            response = statsText();
        } else {
            response = null;
        }

        if (response != null) {
            telegramBotClient.sendMessage(chatId, response);
        }
    }

    private String helpText() {
        return """
                🤖 <b>Comandes AMG Admin</b>

                /stats — Resum general de la plataforma
                /pipeline — Kanban de vendes per etapes
                /nous — Leads nous i contactats
                /leads — Últims 10 leads
                /pressupostos — Pressupostos pendents de resposta
                /tenants — Clients actius
                /ajuda — Aquesta ajuda""";
    }

    private String pipelineStats() {
        var sb = new StringBuilder("📊 <b>Pipeline de vendes</b>\n\n");
        long total = 0;
        for (PipelineStage stage : PipelineStage.values()) {
            long count = leadRepository.countByStage(stage);
            if (count > 0) {
                sb.append(stageEmoji(stage)).append(" <b>").append(stageLabel(stage))
                  .append(":</b> ").append(count).append("\n");
                total += count;
            }
        }
        sb.append("\nTotal actius: <b>").append(total).append("</b>");
        return sb.toString();
    }

    private String newLeads() {
        UUID ownerTenantId = tenantRepository.findByIsOwnerTrue().map(Tenant::getId).orElse(null);
        List<Lead> leads = ownerTenantId != null
                ? leadRepository.findByTenantId(ownerTenantId).stream()
                        .filter(l -> Boolean.TRUE.equals(l.getIsActive())
                                && (l.getStage() == PipelineStage.NEW || l.getStage() == PipelineStage.CONTACTED))
                        .limit(10).toList()
                : List.of();

        if (leads.isEmpty()) return "✅ Sense leads nous ni contactats ara mateix.";

        var sb = new StringBuilder("🆕 <b>Leads nous i contactats (").append(leads.size()).append(")</b>\n\n");
        for (int i = 0; i < leads.size(); i++) {
            var l = leads.get(i);
            String contact = l.getPhone() != null ? l.getPhone()
                    : (l.getEmail() != null ? l.getEmail() : "—");
            sb.append(i + 1).append(". <b>").append(escape(l.getName())).append("</b>")
              .append(" — ").append(escape(contact))
              .append(" — <i>").append(stageLabel(l.getStage())).append("</i>");
            if (l.getCreatedAt() != null) {
                sb.append(" (").append(FMT.format(l.getCreatedAt())).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String recentLeads() {
        UUID ownerTenantId = tenantRepository.findByIsOwnerTrue().map(Tenant::getId).orElse(null);
        List<Lead> leads = ownerTenantId != null
                ? leadRepository.findByTenantId(ownerTenantId, PageRequest.of(0, 10)).getContent()
                : List.of();

        if (leads.isEmpty()) return "📭 Sense leads registrats.";

        var sb = new StringBuilder("📋 <b>Últims ").append(leads.size()).append(" leads</b>\n\n");
        for (int i = 0; i < leads.size(); i++) {
            var l = leads.get(i);
            String contact = l.getPhone() != null ? l.getPhone()
                    : (l.getEmail() != null ? l.getEmail() : "—");
            sb.append(i + 1).append(". <b>").append(escape(l.getName())).append("</b>")
              .append(" — ").append(escape(contact))
              .append(" — <i>").append(l.getStage() != null ? stageLabel(l.getStage()) : "?").append("</i>")
              .append("\n");
        }
        return sb.toString();
    }

    private String pendingBudgets() {
        var budgets = budgetRepository.findByStatusOrderByCreatedAtDesc(
                BudgetStatus.SENT, PageRequest.of(0, 10)).getContent();

        if (budgets.isEmpty()) return "✅ Sense pressupostos pendents de resposta.";

        var sb = new StringBuilder("💸 <b>Pressupostos pendents (").append(budgets.size()).append(")</b>\n\n");
        Instant now = Instant.now();
        for (var b : budgets) {
            var tenant = b.getTenantId() != null
                    ? tenantRepository.findById(b.getTenantId()).orElse(null) : null;
            String name = tenant != null
                    ? (tenant.getName() != null ? tenant.getName() : tenant.getEmail()) : "—";
            long dies = b.getSentAt() != null
                    ? ChronoUnit.DAYS.between(b.getSentAt(), now) : 0;
            String total = b.getTotal() != null ? b.getTotal().toPlainString() + " €" : "—";
            sb.append("• <b>").append(escape(name)).append("</b>")
              .append(" — ").append(total)
              .append(" — fa ").append(dies).append(" ").append(dies == 1 ? "dia" : "dies")
              .append("\n");
        }
        return sb.toString();
    }

    private String activeTenantsText() {
        var actius = tenantRepository.findAll().stream()
                .filter(t -> t.getActivePhases() != null && !t.getActivePhases().isBlank()
                        && !Boolean.TRUE.equals(t.getIsOwner()))
                .sorted((a, b) -> {
                    if (a.getName() == null) return 1;
                    if (b.getName() == null) return -1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .toList();

        if (actius.isEmpty()) return "📭 Sense clients actius.";

        var sb = new StringBuilder("✅ <b>Clients actius (").append(actius.size()).append(")</b>\n\n");
        for (var t : actius) {
            String name = t.getName() != null ? t.getName() : t.getEmail();
            sb.append("• <b>").append(escape(name)).append("</b>");
            if (t.getActivePhases() != null && !t.getActivePhases().isBlank()) {
                sb.append(" — <i>").append(t.getActivePhases()).append("</i>");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String statsText() {
        UUID ownerTenantId = tenantRepository.findByIsOwnerTrue().map(Tenant::getId).orElse(null);

        long totalLeads = ownerTenantId != null
                ? leadRepository.findByTenantId(ownerTenantId).size() : 0;
        long leadsNous = leadRepository.countByStage(PipelineStage.NEW)
                + leadRepository.countByStage(PipelineStage.CONTACTED);
        long leadsQualificats = leadRepository.countByStage(PipelineStage.QUALIFIED)
                + leadRepository.countByStage(PipelineStage.PROPOSAL)
                + leadRepository.countByStage(PipelineStage.NEGOTIATION);

        long pressupostsPendents = budgetRepository.findByStatusOrderByCreatedAtDesc(
                BudgetStatus.SENT, PageRequest.of(0, 100)).getTotalElements();

        long tenantsActius = tenantRepository.findAll().stream()
                .filter(t -> t.getActivePhases() != null && !t.getActivePhases().isBlank()
                        && !Boolean.TRUE.equals(t.getIsOwner()))
                .count();

        long setupPendents = intakeRepository.findAll().stream()
                .filter(i -> "PENDING".equals(i.getStatus()) || "IN_PROGRESS".equals(i.getStatus()))
                .count();

        return "📈 <b>Estadístiques generals</b>\n\n" +
                "👥 <b>Leads totals:</b> " + totalLeads + "\n" +
                "   🆕 Nous/Contactats: " + leadsNous + "\n" +
                "   🎯 Qualificats/Proposta: " + leadsQualificats + "\n\n" +
                "💸 <b>Pressupostos pendents:</b> " + pressupostsPendents + "\n" +
                "✅ <b>Clients actius:</b> " + tenantsActius + "\n" +
                "⏳ <b>Setup pendent:</b> " + setupPendents;
    }

    private String stageEmoji(PipelineStage stage) {
        return switch (stage) {
            case NEW         -> "🆕";
            case CONTACTED   -> "📞";
            case QUALIFIED   -> "🎯";
            case PROPOSAL    -> "📋";
            case NEGOTIATION -> "🤝";
            case WON         -> "🏆";
            case NURTURING   -> "❄️";
            case LOST        -> "❌";
        };
    }

    private String stageLabel(PipelineStage stage) {
        if (stage == null) return "?";
        return switch (stage) {
            case NEW         -> "Nou";
            case CONTACTED   -> "Contactat";
            case QUALIFIED   -> "Qualificat";
            case PROPOSAL    -> "Proposta";
            case NEGOTIATION -> "Negociació";
            case WON         -> "Guanyat";
            case NURTURING   -> "Nurturing";
            case LOST        -> "Perdut";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
