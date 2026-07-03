package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountManager implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    @Transactional
    public DiscountResponse createDiscount(CreateDiscountRequest request, UUID createdBy) {
        var discount = Discount.builder()
                .tenantId(request.tenantId())
                .type(DiscountType.valueOf(request.type()))
                .value(request.value())
                .appliesTo(DiscountAppliesTo.valueOf(request.appliesTo()))
                .referenceId(request.referenceId())
                .label(request.label())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .appliesToSetup(request.appliesToSetup() == null || request.appliesToSetup())
                .appliesToMonthly(Boolean.TRUE.equals(request.appliesToMonthly()))
                .isLifetime(Boolean.TRUE.equals(request.isLifetime()))
                .maxApplications(request.maxApplications())
                .createdBy(createdBy)
                .build();
        discount = discountRepository.save(discount);
        return toDiscountResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountResponse> listDiscounts(UUID tenantId, Boolean isActive, String appliesTo) {
        if (isActive != null) {
            return discountRepository.findByTenantIdAndIsActive(tenantId, isActive)
                    .stream().map(this::toDiscountResponse).toList();
        }
        return discountRepository.findByTenantId(tenantId)
                .stream().map(this::toDiscountResponse).toList();
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(UUID discountId, UpdateDiscountRequest request) {
        var discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + discountId));
        if (request.type() != null) discount.setType(DiscountType.valueOf(request.type()));
        if (request.value() != null) discount.setValue(request.value());
        if (request.appliesTo() != null) discount.setAppliesTo(DiscountAppliesTo.valueOf(request.appliesTo()));
        if (request.referenceId() != null) discount.setReferenceId(request.referenceId());
        if (request.label() != null) discount.setLabel(request.label());
        if (request.isActive() != null) discount.setIsActive(request.isActive());
        if (request.validFrom() != null) discount.setValidFrom(request.validFrom());
        if (request.validUntil() != null) discount.setValidUntil(request.validUntil());
        discount = discountRepository.save(discount);
        return toDiscountResponse(discount);
    }

    @Override
    @Transactional
    public void deactivateDiscount(UUID discountId) {
        var discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + discountId));
        discount.setIsActive(false);
        discountRepository.save(discount);
    }

    private DiscountResponse toDiscountResponse(Discount d) {
        return new DiscountResponse(d.getId(), d.getTenantId(), d.getType().name(), d.getValue(),
                d.getAppliesTo().name(), d.getReferenceId(), d.getLabel(), d.getIsActive(),
                d.getValidFrom(), d.getValidUntil(), d.getMaxApplications(), d.getAppliedCount(),
                d.getCreatedAt(), d.getAppliesToSetup(), d.getAppliesToMonthly(), d.getIsLifetime(),
                d.getProgram() != null ? d.getProgram().name() : null);
    }
}
