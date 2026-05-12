package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.application.BillingService;
import com.amg.digitalitzacio.billing.application.DiscountService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final DiscountService discountService;

    // ── 4.1 Pressupostos ──

    @PostMapping("/tenants/{tenantId}/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BudgetResponse> createBudget(@PathVariable UUID tenantId,
                                                        @RequestBody CreateBudgetRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createBudget(tenantId, request, principal.id()));
    }

    @GetMapping("/tenants/{tenantId}/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<BudgetListResponse> listBudgets(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(billingService.listBudgets(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable UUID tenantId, @PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getBudget(tenantId, id));
    }

    @PutMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable UUID id,
                                                        @RequestBody UpdateBudgetRequest request) {
        return ResponseEntity.ok(billingService.updateBudget(id, request));
    }

    @DeleteMapping("/budgets/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> cancelBudget(@PathVariable UUID id) {
        billingService.cancelBudget(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/budgets/{id}/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BudgetResponse> sendBudget(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.sendBudget(id));
    }

    // ── Acceptació / Rebuig públic (token-based, sense JWT) ──

    @PostMapping("/budgets/accept")
    public ResponseEntity<BudgetResponse> acceptBudget(@RequestParam String token) {
        return ResponseEntity.ok(billingService.acceptBudget(token));
    }

    @PostMapping("/budgets/reject")
    public ResponseEntity<BudgetResponse> rejectBudget(@RequestParam String token,
                                                        @RequestBody(required = false) RejectBudgetRequest request) {
        return ResponseEntity.ok(billingService.rejectBudget(token,
                request != null ? request.getReason() : null));
    }

    // ── 4.2 Descomptes ──

    @PostMapping("/discounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DiscountResponse> createDiscount(@RequestBody CreateDiscountRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discountService.createDiscount(request, principal.id()));
    }

    @GetMapping("/discounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<DiscountResponse>> listDiscounts(@RequestParam(required = false) UUID tenantId) {
        return ResponseEntity.ok(discountService.listDiscounts(tenantId));
    }

    @PutMapping("/discounts/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DiscountResponse> updateDiscount(@PathVariable UUID id,
                                                            @RequestBody UpdateDiscountRequest request) {
        return ResponseEntity.ok(discountService.updateDiscount(id, request));
    }

    @DeleteMapping("/discounts/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateDiscount(@PathVariable UUID id) {
        discountService.deactivateDiscount(id);
        return ResponseEntity.noContent().build();
    }

    // ── 4.3 Dashboard ──

    @GetMapping("/tenants/{tenantId}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(billingService.getDashboard(tenantId));
    }
}
