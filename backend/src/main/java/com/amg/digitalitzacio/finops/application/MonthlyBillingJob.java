package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class MonthlyBillingJob {

    private final FinOpsService finOpsService;
    private final GoCardlessService goCardlessService;

    // Dia 1 de cada mes a les 02:00
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyInvoices() {
        // Facturem el mes anterior
        var period = YearMonth.now().minusMonths(1).toString(); // ex: "2026-05"
        log.info("MonthlyBillingJob: generating invoices for period {}", period);
        try {
            var invoices = finOpsService.generateMonthlyInvoices(period);
            log.info("MonthlyBillingJob: generated {} invoices for period {}", invoices.size(), period);
        } catch (Exception e) {
            log.error("MonthlyBillingJob: error generating invoices for period {}: {}", period, e.getMessage(), e);
        }

        // Cobrar via GoCardless els tenants que ho tenen actiu
        log.info("MonthlyBillingJob: charging GoCardless mandates for period {}", period);
        try {
            goCardlessService.chargeMonthlyInvoices(period);
            log.info("MonthlyBillingJob: GoCardless charges queued for period {}", period);
        } catch (Exception e) {
            log.error("MonthlyBillingJob: error charging GoCardless for period {}: {}", period, e.getMessage(), e);
        }
    }
}
