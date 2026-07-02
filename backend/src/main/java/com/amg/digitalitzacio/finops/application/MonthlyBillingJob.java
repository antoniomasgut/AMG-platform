package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.finops.domain.HoldedConfigRepository;
import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessMandateRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessMandateStatus;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class MonthlyBillingJob {

    private final FinOpsService finOpsService;
    private final GoCardlessService goCardlessService;
    private final TenantRepository tenantRepository;
    private final HoldedConfigRepository holdedConfigRepository;
    private final GoCardlessMandateRepository goCardlessMandateRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService systemConfigService;

    // Dia 1 de cada mes a les 02:00
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyInvoices() {
        // Facturem el mes anterior
        var period = YearMonth.now().minusMonths(1).toString(); // ex: "2026-05"
        log.info("MonthlyBillingJob: generating invoices for period {}", period);

        int invoiceCount = 0;
        BigDecimal invoicedTotal = BigDecimal.ZERO;
        String invoiceError = null;
        try {
            var invoices = finOpsService.generateMonthlyInvoices(period);
            invoiceCount = invoices.size();
            invoicedTotal = invoices.stream()
                    .map(i -> i.amount() != null ? i.amount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("MonthlyBillingJob: generated {} invoices for period {}", invoiceCount, period);
        } catch (Exception e) {
            invoiceError = e.getMessage();
            log.error("MonthlyBillingJob: error generating invoices for period {}: {}", period, e.getMessage(), e);
        }

        // Cobrar via GoCardless els tenants que ho tenen actiu
        String chargeError = null;
        try {
            goCardlessService.chargeMonthlyInvoices(period);
            log.info("MonthlyBillingJob: GoCardless charges queued for period {}", period);
        } catch (Exception e) {
            chargeError = e.getMessage();
            log.error("MonthlyBillingJob: error charging GoCardless for period {}: {}", period, e.getMessage(), e);
        }

        sendSummary(period, invoiceCount, invoicedTotal, invoiceError, chargeError);
    }

    /**
     * Resum mensual a Telegram: factures generades + tenants actius que el job
     * NO pot facturar o cobrar (sense HoldedConfig o sense mandat SEPA actiu).
     * És la xarxa de seguretat contra tenants actius que ningú factura.
     */
    private void sendSummary(String period, int invoiceCount, BigDecimal invoicedTotal,
                             String invoiceError, String chargeError) {
        try {
            var missingHolded = new ArrayList<String>();
            var missingMandate = new ArrayList<String>();
            for (var tenant : tenantRepository.findAll()) {
                boolean billable = tenant.getBillingStartDate() != null
                        && !Boolean.TRUE.equals(tenant.getIsFree())
                        && !Boolean.TRUE.equals(tenant.getIsOwner())
                        && tenant.getContractedPhases() != null && !tenant.getContractedPhases().isBlank();
                if (!billable) continue;
                if (holdedConfigRepository.findByTenantId(tenant.getId()).isEmpty()) {
                    missingHolded.add(tenant.getName());
                }
                if (goCardlessMandateRepository.findByTenantIdAndStatus(
                        tenant.getId(), GoCardlessMandateStatus.ACTIVE).isEmpty()) {
                    missingMandate.add(tenant.getName());
                }
            }

            var sb = new StringBuilder();
            sb.append("🧾 <b>Facturació mensual — ").append(period).append("</b>\n");
            sb.append("Factures generades: ").append(invoiceCount)
              .append(" (").append(invoicedTotal.toPlainString()).append(" €)\n");
            if (invoiceError != null) sb.append("❌ Error factures: ").append(invoiceError).append("\n");
            if (chargeError != null)  sb.append("❌ Error cobraments SEPA: ").append(chargeError).append("\n");
            if (!missingHolded.isEmpty()) {
                sb.append("⚠️ Sense config Holded (no es facturen): ")
                  .append(String.join(", ", missingHolded)).append("\n");
            }
            if (!missingMandate.isEmpty()) {
                sb.append("⚠️ Sense mandat SEPA actiu (no es cobren): ")
                  .append(String.join(", ", missingMandate)).append("\n");
            }
            if (invoiceError == null && chargeError == null
                    && missingHolded.isEmpty() && missingMandate.isEmpty()) {
                sb.append("✅ Tot en ordre");
            }

            String chatIdStr = systemConfigService.get("AMG_SALES_CHAT_ID");
            if (chatIdStr != null && !chatIdStr.isBlank()) {
                telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), sb.toString());
            }
        } catch (Exception e) {
            log.warn("MonthlyBillingJob: error enviant resum Telegram: {}", e.getMessage());
        }
    }
}
