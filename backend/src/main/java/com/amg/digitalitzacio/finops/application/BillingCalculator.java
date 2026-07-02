package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.auth.domain.SectorPricingRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.Discount;
import com.amg.digitalitzacio.billing.domain.DiscountRepository;
import com.amg.digitalitzacio.billing.domain.DiscountType;
import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Càlcul de la quota mensual (model de preus NexeLocal, mòdul 22):
 *
 *   quota = tiers de fases contractades (sector_pricing per sector + mida)
 *         + serveis independents de fases (landings, dominis — phaseId null)
 *         − descomptes actius amb appliesToMonthly (taula discounts)
 *
 * Els serveis lligats a fases (phaseId != null) NO sumen preu propi:
 * la seva quota ja està coberta pel tier de la fase.
 *
 * Descomptes mensuals suportats:
 *   - permanents (isLifetime)
 *   - temporals (validFrom / validUntil)
 *   - per N mesos (maxApplications: mesos naturals des de validFrom/creació,
 *     p. ex. crèdit de referit "2 mesos gratis")
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingCalculator {

    private final TenantServiceRepository tenantServiceRepository;
    private final TenantRepository tenantRepository;
    private final SectorPricingRepository sectorPricingRepository;
    private final DiscountRepository discountRepository;

    public BigDecimal calculateMonthlyAmount(UUID tenantId, String period, LocalDate billingStartDate) {
        if (billingStartDate == null) {
            return BigDecimal.ZERO;
        }
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || Boolean.TRUE.equals(tenant.getIsFree())) {
            return BigDecimal.ZERO;
        }

        // period = "2026-05"
        var parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        var periodStart = LocalDate.of(year, month, 1);
        var periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        int daysInMonth = periodStart.lengthOfMonth();

        if (billingStartDate.isAfter(periodEnd)) {
            return BigDecimal.ZERO; // encara no factura
        }

        // 1. Quota de fases: suma de tiers segons fases contractades
        BigDecimal phaseTotal = BigDecimal.ZERO;
        List<String> phases = tenant.getContractedPhases() != null && !tenant.getContractedPhases().isBlank()
                ? Arrays.stream(tenant.getContractedPhases().split(",")).map(String::trim).toList()
                : List.of();
        if (!phases.isEmpty() && tenant.getSector() != null && tenant.getBusinessSize() != null) {
            var pricing = sectorPricingRepository
                    .findBySectorAndBusinessSize(tenant.getSector(), tenant.getBusinessSize())
                    .orElse(null);
            if (pricing != null) {
                phaseTotal = pricing.totalMonthly(phases);
            } else {
                log.warn("[Billing] Tenant {} sense sector_pricing per {}/{} — quota de fases 0",
                        tenantId, tenant.getSector(), tenant.getBusinessSize());
            }
        } else if (!phases.isEmpty()) {
            log.warn("[Billing] Tenant {} amb fases {} però sense sector o businessSize — quota de fases 0",
                    tenantId, phases);
        }

        // 2. Serveis independents de fases (landings, dominis...): preu propi
        BigDecimal servicesTotal = tenantServiceRepository.findByTenantId(tenantId).stream()
                .filter(ts -> ts.getPhaseId() == null)
                .filter(ts -> Boolean.TRUE.equals(ts.getIsEnabled()))
                .map(ts -> ts.getMonthlyPriceLocked() != null ? ts.getMonthlyPriceLocked() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal base = phaseTotal.add(servicesTotal);

        // 3. Descomptes mensuals vigents (percentatge primer, després fixos)
        BigDecimal total = applyMonthlyDiscounts(tenantId, base, periodStart, periodEnd);

        // 4. Prorrata del primer mes
        if (billingStartDate.isAfter(periodStart)) {
            int activeDays = periodEnd.getDayOfMonth() - billingStartDate.getDayOfMonth() + 1;
            total = total.multiply(BigDecimal.valueOf(activeDays))
                    .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyMonthlyDiscounts(UUID tenantId, BigDecimal base,
                                             LocalDate periodStart, LocalDate periodEnd) {
        var discounts = discountRepository.findByTenantIdAndIsActive(tenantId, true).stream()
                .filter(d -> Boolean.TRUE.equals(d.getAppliesToMonthly()))
                .filter(d -> appliesToPeriod(d, periodStart, periodEnd))
                .toList();

        BigDecimal total = base;
        for (var d : discounts) {
            if (d.getType() == DiscountType.PERCENTAGE) {
                total = total.subtract(base.multiply(d.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            } else {
                total = total.subtract(d.getValue());
            }
        }
        return total.max(BigDecimal.ZERO);
    }

    private boolean appliesToPeriod(Discount d, LocalDate periodStart, LocalDate periodEnd) {
        if (d.getValidFrom() != null && d.getValidFrom().isAfter(periodEnd)) return false;
        if (Boolean.TRUE.equals(d.getIsLifetime())) return true;
        if (d.getValidUntil() != null && d.getValidUntil().isBefore(periodStart)) return false;
        // maxApplications = nombre de mesos naturals que s'aplica (crèdits de referit, promos "N mesos")
        if (d.getMaxApplications() != null && d.getMaxApplications() > 0) {
            LocalDate start = d.getValidFrom() != null ? d.getValidFrom()
                    : d.getCreatedAt() != null
                        ? LocalDate.ofInstant(d.getCreatedAt(), ZoneId.of("Europe/Madrid"))
                        : periodStart;
            long monthsElapsed = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(periodStart));
            return monthsElapsed >= 0 && monthsElapsed < d.getMaxApplications();
        }
        return true;
    }
}
