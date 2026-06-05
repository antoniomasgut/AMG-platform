package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillingCalculator {

    private final TenantServiceRepository tenantServiceRepository;

    public BigDecimal calculateMonthlyAmount(UUID tenantId, String period, LocalDate billingStartDate) {
        if (billingStartDate == null) {
            return BigDecimal.ZERO;
        }
        // period = "2026-05"
        var parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        var periodStart = LocalDate.of(year, month, 1);
        var periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        int daysInMonth = periodStart.lengthOfMonth();

        // Serveis actius: VERIFIED o CONFIGURED (aproximació — idealment IMPLEMENTATION_ACCEPTED)
        var services = tenantServiceRepository.findByTenantId(tenantId);

        BigDecimal total = BigDecimal.ZERO;
        for (var ts : services) {
            if (ts.getMonthlyPriceLocked() == null || ts.getMonthlyPriceLocked().compareTo(BigDecimal.ZERO) == 0)
                continue;

            // Use billingStartDate for pro-rata calculation
            if (billingStartDate.isAfter(periodEnd)) {
                return BigDecimal.ZERO; // No billing yet
            }
            if (billingStartDate.isAfter(periodStart)) {
                // Pro-rata: days from billingStartDate to end of month
                int activeDays = periodEnd.getDayOfMonth() - billingStartDate.getDayOfMonth() + 1;
                BigDecimal prorata = ts.getMonthlyPriceLocked()
                        .multiply(BigDecimal.valueOf(activeDays))
                        .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
                total = total.add(prorata);
                continue;
            }
            total = total.add(ts.getMonthlyPriceLocked());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
