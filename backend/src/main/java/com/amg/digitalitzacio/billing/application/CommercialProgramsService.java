package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class CommercialProgramsService {
    private final EarlyAdopterProgramRepository earlyAdopterProgramRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final DiscountRepository discountRepository;
    private final com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService systemConfigService;

    public EarlyAdopterProgram getEarlyAdopterStatus() {
        return earlyAdopterProgramRepository.findFirstByOrderByUpdatedAtDesc()
            .orElseGet(() -> {
                EarlyAdopterProgram program = EarlyAdopterProgram.builder()
                    .maxSlots(20)
                    .usedSlots(0)
                    .setupDiscountPct(BigDecimal.valueOf(100))
                    .monthlyDiscountPct(BigDecimal.valueOf(30))
                    .commitmentMonths(6)
                    .active(true)
                    .build();
                return earlyAdopterProgramRepository.save(program);
            });
    }

    public EarlyAdopterProgram updateEarlyAdopterConfig(Integer maxSlots, BigDecimal setupPct,
                                                        BigDecimal monthlyPct, Integer commitmentMonths,
                                                        Boolean active) {
        EarlyAdopterProgram program = getEarlyAdopterStatus();
        if (maxSlots != null) program.setMaxSlots(maxSlots);
        if (setupPct != null) program.setSetupDiscountPct(setupPct);
        if (monthlyPct != null) program.setMonthlyDiscountPct(monthlyPct);
        if (commitmentMonths != null) program.setCommitmentMonths(commitmentMonths);
        if (active != null) program.setActive(active);
        return earlyAdopterProgramRepository.save(program);
    }

    public List<Discount> applyEarlyAdopter(UUID tenantId, UUID createdByUserId) {
        EarlyAdopterProgram program = getEarlyAdopterStatus();

        if (!program.getActive() || program.getUsedSlots() >= program.getMaxSlots()) {
            throw new RuntimeException("No hi ha slots d'early adopter disponibles");
        }

        // Crear descompte per setup
        Discount setupDiscount = Discount.builder()
            .type(DiscountType.PERCENTAGE)
            .value(program.getSetupDiscountPct())
            .appliesTo(DiscountAppliesTo.BUDGET)
            .tenantId(tenantId)
            .label("Early Adopter - Setup gratuït")
            .program(DiscountProgram.EARLY_ADOPTER)
            .appliesToSetup(true)
            .appliesToMonthly(false)
            .isLifetime(false)
            .isActive(true)
            .createdBy(createdByUserId)
            .build();

        // Crear descompte per mensual
        Discount monthlyDiscount = Discount.builder()
            .type(DiscountType.PERCENTAGE)
            .value(program.getMonthlyDiscountPct())
            .appliesTo(DiscountAppliesTo.BUDGET)
            .tenantId(tenantId)
            .label("Early Adopter - " + program.getMonthlyDiscountPct() + "% mensual de per vida")
            .program(DiscountProgram.EARLY_ADOPTER)
            .appliesToSetup(false)
            .appliesToMonthly(true)
            .isLifetime(true)
            .commitmentMonths(program.getCommitmentMonths())
            .isActive(true)
            .createdBy(createdByUserId)
            .build();

        // Guardar descomptes
        discountRepository.save(setupDiscount);
        discountRepository.save(monthlyDiscount);

        // Incrementar slots usats i tancar si necessari
        program.setUsedSlots(program.getUsedSlots() + 1);
        if (program.getUsedSlots() >= program.getMaxSlots()) {
            program.setActive(false);
        }
        earlyAdopterProgramRepository.save(program);

        return List.of(setupDiscount, monthlyDiscount);
    }

    public Discount applyAnnualContract(UUID tenantId, UUID createdByUserId) {
        Discount discount = Discount.builder()
            .type(DiscountType.PERCENTAGE)
            .value(BigDecimal.valueOf(100))
            .appliesTo(DiscountAppliesTo.BUDGET)
            .tenantId(tenantId)
            .label("Contracte anual - Setup gratuït")
            .program(DiscountProgram.ANNUAL_CONTRACT)
            .appliesToSetup(true)
            .appliesToMonthly(false)
            .commitmentMonths(12)
            .isActive(true)
            .createdBy(createdByUserId)
            .build();

        return discountRepository.save(discount);
    }

    public ReferralCode getOrCreateReferralCode(UUID tenantId) {
        return referralCodeRepository.findByOwnerTenantId(tenantId)
            .orElseGet(() -> {
                String code;
                boolean isUnique;
                do {
                    code = generateReferralCode();
                    String finalCode = code;
                    isUnique = !referralCodeRepository.findByCode(finalCode).isPresent();
                } while (!isUnique);

                ReferralCode referralCode = ReferralCode.builder()
                    .ownerTenantId(tenantId)
                    .code(code)
                    .referrerCreditMonths(1)
                    .referredSetupFree(true)
                    .creditApplied(false)
                    .build();

                return referralCodeRepository.save(referralCode);
            });
    }

    public ReferralCode applyReferralCode(String code, UUID newTenantId, UUID createdByUserId) {
        ReferralCode referralCode = referralCodeRepository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("Codi referit no trobat"));

        if (referralCode.getUsedByTenantId() != null) {
            throw new RuntimeException("Codi ja usat");
        }

        // Marcar com usat
        referralCode.setUsedByTenantId(newTenantId);
        referralCode.setUsedAt(Instant.now());
        referralCodeRepository.save(referralCode);

        // Crear descompte per al referit — percentatge configurable (System Config, per defecte 50%)
        BigDecimal setupPct = referralSetupDiscountPct();
        Discount discountForReferred = Discount.builder()
            .type(DiscountType.PERCENTAGE)
            .value(setupPct)
            .appliesTo(DiscountAppliesTo.BUDGET)
            .tenantId(newTenantId)
            .label("Referit - " + setupPct.stripTrailingZeros().toPlainString() + "% setup")
            .program(DiscountProgram.REFERRAL)
            .appliesToSetup(true)
            .appliesToMonthly(false)
            .isActive(true)
            .createdBy(createdByUserId)
            .build();

        discountRepository.save(discountForReferred);

        return referralCode;
    }

    public List<ReferralCode> listAllReferrals() {
        return referralCodeRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<ReferralCode> getTenantReferral(UUID tenantId) {
        return referralCodeRepository.findByOwnerTenantId(tenantId);
    }

    private BigDecimal referralSetupDiscountPct() {
        try {
            String v = systemConfigService.get("REFERRAL_SETUP_DISCOUNT_PCT");
            if (v != null && !v.isBlank()) {
                var pct = new BigDecimal(v.trim());
                if (pct.signum() > 0 && pct.compareTo(BigDecimal.valueOf(100)) <= 0) return pct;
            }
        } catch (Exception ignored) {}
        return BigDecimal.valueOf(50);
    }

    private String generateReferralCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("AMG-");
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
