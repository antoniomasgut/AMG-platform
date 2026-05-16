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

    public BigDecimal calculateMonthlyAmount(UUID tenantId, String period) {
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

            // Si activatedAt és dins el periode, calcular pro-rata
            if (ts.getActivatedAt() != null) {
                var activatedDate = ts.getActivatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                if (activatedDate.isAfter(periodEnd)) continue; // Activat el mes que ve, no compta
                if (activatedDate.isAfter(periodStart)) {
                    // Pro-rata: dies des d'activació fins a final de mes
                    int activedays = periodEnd.getDayOfMonth() - activatedDate.getDayOfMonth() + 1;
                    BigDecimal prorata = ts.getMonthlyPriceLocked()
                            .multiply(BigDecimal.valueOf(activedays))
                            .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
                    total = total.add(prorata);
                    continue;
                }
            }
            total = total.add(ts.getMonthlyPriceLocked());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
