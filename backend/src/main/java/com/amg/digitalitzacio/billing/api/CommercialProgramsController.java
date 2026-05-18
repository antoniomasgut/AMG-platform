package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.application.CommercialProgramsService;
import com.amg.digitalitzacio.billing.domain.EarlyAdopterProgram;
import com.amg.digitalitzacio.billing.domain.ReferralCode;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing/programs")
@RequiredArgsConstructor
public class CommercialProgramsController {
    private final CommercialProgramsService commercialProgramsService;

    @GetMapping("/early-adopter")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EarlyAdopterStatusResponse> getEarlyAdopterStatus() {
        EarlyAdopterProgram program = commercialProgramsService.getEarlyAdopterStatus();
        Integer availableSlots = program.getMaxSlots() - program.getUsedSlots();
        EarlyAdopterStatusResponse response = new EarlyAdopterStatusResponse(
            program.getId(),
            program.getMaxSlots(),
            program.getUsedSlots(),
            availableSlots,
            program.getSetupDiscountPct(),
            program.getMonthlyDiscountPct(),
            program.getCommitmentMonths(),
            program.getActive(),
            program.getUpdatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/early-adopter")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EarlyAdopterStatusResponse> updateEarlyAdopter(
            @RequestBody UpdateEarlyAdopterRequest request) {
        EarlyAdopterProgram program = commercialProgramsService.updateEarlyAdopterConfig(
            request.maxSlots(),
            request.setupDiscountPct(),
            request.monthlyDiscountPct(),
            request.commitmentMonths(),
            request.active()
        );
        Integer availableSlots = program.getMaxSlots() - program.getUsedSlots();
        EarlyAdopterStatusResponse response = new EarlyAdopterStatusResponse(
            program.getId(),
            program.getMaxSlots(),
            program.getUsedSlots(),
            availableSlots,
            program.getSetupDiscountPct(),
            program.getMonthlyDiscountPct(),
            program.getCommitmentMonths(),
            program.getActive(),
            program.getUpdatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/early-adopter/apply/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> applyEarlyAdopter(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            commercialProgramsService.applyEarlyAdopter(tenantId, principal.id());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/annual-contract/apply/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> applyAnnualContract(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            commercialProgramsService.applyAnnualContract(tenantId, principal.id());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/referrals")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ReferralCodeResponse>> getAllReferrals() {
        List<ReferralCode> referrals = commercialProgramsService.listAllReferrals();
        List<ReferralCodeResponse> responses = referrals.stream()
            .map(rc -> new ReferralCodeResponse(
                rc.getId(),
                rc.getOwnerTenantId(),
                rc.getCode(),
                rc.getUsedByTenantId(),
                rc.getUsedAt(),
                rc.getCreditApplied(),
                rc.getReferrerCreditMonths(),
                rc.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/referrals/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyReferralCode(
            @AuthenticationPrincipal UserPrincipal principal) {
        var referral = commercialProgramsService.getTenantReferral(principal.tenantId());
        if (referral.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        ReferralCode rc = referral.get();
        ReferralCodeResponse response = new ReferralCodeResponse(
            rc.getId(),
            rc.getOwnerTenantId(),
            rc.getCode(),
            rc.getUsedByTenantId(),
            rc.getUsedAt(),
            rc.getCreditApplied(),
            rc.getReferrerCreditMonths(),
            rc.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/referrals/apply")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> applyReferralCode(
            @RequestBody ApplyReferralRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            ReferralCode referralCode = commercialProgramsService.applyReferralCode(
                request.code(),
                request.newTenantId(),
                principal.id()
            );
            return ResponseEntity.ok(new ReferralCodeResponse(
                referralCode.getId(),
                referralCode.getOwnerTenantId(),
                referralCode.getCode(),
                referralCode.getUsedByTenantId(),
                referralCode.getUsedAt(),
                referralCode.getCreditApplied(),
                referralCode.getReferrerCreditMonths(),
                referralCode.getCreatedAt()
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no trobat")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponse(e.getMessage())
                );
            } else if (e.getMessage().contains("ja usat")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponse(e.getMessage())
                );
            }
            throw e;
        }
    }

    record ErrorResponse(String message) {}
}
