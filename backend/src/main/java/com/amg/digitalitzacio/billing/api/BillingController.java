package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.application.BillingService;
import com.amg.digitalitzacio.billing.application.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final DiscountService discountService;

    // --- Budgets ---

    @PostMapping("/tenants/{tenantId}/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(@PathVariable UUID tenantId, @Valid @RequestBody CreateBudgetRequest request) {
        return billingService.createBudget(tenantId, request);
    }

    @GetMapping("/tenants/{tenantId}/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public List<BudgetResponse> listBudgets(@PathVariable UUID tenantId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return billingService.listBudgets(tenantId, status, page, size);
    }

    @GetMapping("/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<BudgetResponse> listAllBudgets(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return billingService.listAllBudgets(status, page, size);
    }

    @GetMapping("/tenants/{tenantId}/budgets/{id}")
    @PreAuthorize("isAuthenticated()")
    public BudgetResponse getBudget(@PathVariable UUID id) {
        return billingService.getBudget(id, false);
    }

    @PutMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public BudgetResponse updateBudget(@PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest request) {
        return billingService.updateBudget(id, request);
    }

    @DeleteMapping("/budgets/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBudget(@PathVariable UUID id) {
        billingService.cancelBudget(id);
    }

    @PostMapping("/budgets/{id}/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public BudgetSendResponse sendBudget(@PathVariable UUID id) {
        return billingService.sendBudget(id);
    }

    @GetMapping("/budgets/preview")
    @PreAuthorize("permitAll()")
    public BudgetResponse previewBudget(@RequestParam String token) {
        return billingService.previewBudget(token);
    }

    @PostMapping("/budgets/accept")
    public AcceptRejectResponse acceptBudget(@RequestParam String token) {
        return billingService.acceptBudget(token);
    }

    @PostMapping("/budgets/accept-phases")
    public AcceptRejectResponse acceptBudgetPhases(@RequestParam String token,
                                                    @RequestBody AcceptPhasesRequest request) {
        return billingService.acceptBudgetPhases(token, request.phaseKeys());
    }

    @PostMapping("/budgets/reject")
    public AcceptRejectResponse rejectBudget(@RequestParam String token,
                                              @RequestBody(required = false) RejectBudgetRequest request) {
        return billingService.rejectBudget(token, request != null ? request.reason() : null);
    }

    // --- Discounts ---

    @PostMapping("/discounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse createDiscount(@Valid @RequestBody CreateDiscountRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return discountService.createDiscount(request, principal.id());
    }

    @GetMapping("/discounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<DiscountResponse> listDiscounts(@RequestParam(required = false) UUID tenantId,
                                                 @RequestParam(required = false) Boolean isActive,
                                                 @RequestParam(required = false) String appliesTo) {
        return discountService.listDiscounts(tenantId, isActive, appliesTo);
    }

    @PutMapping("/discounts/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DiscountResponse updateDiscount(@PathVariable UUID id, @Valid @RequestBody UpdateDiscountRequest request) {
        return discountService.updateDiscount(id, request);
    }

    @DeleteMapping("/discounts/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiscount(@PathVariable UUID id) {
        discountService.deactivateDiscount(id);
    }

    // --- Dashboard ---

    @GetMapping("/tenants/{tenantId}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public DashboardResponse getDashboard(@PathVariable UUID tenantId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return billingService.getDashboard(tenantId);
    }
}
