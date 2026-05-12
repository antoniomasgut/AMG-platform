package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.CreateDiscountRequest;
import com.amg.digitalitzacio.billing.api.dto.DiscountResponse;
import com.amg.digitalitzacio.billing.api.dto.UpdateDiscountRequest;
import com.amg.digitalitzacio.billing.domain.Discount;
import com.amg.digitalitzacio.billing.domain.DiscountRepository;
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
                .tenantId(request.getTenantId())
                .type(request.getType())
                .value(request.getValue())
                .appliesTo(request.getAppliesTo())
                .referenceId(request.getReferenceId())
                .label(request.getLabel())
                .isActive(true)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .maxApplications(request.getMaxApplications())
                .appliedCount(0)
                .createdBy(createdBy)
                .build();
        discount = discountRepository.save(discount);
        return toDiscountResponse(discount);
    }

    @Override
    public List<DiscountResponse> listDiscounts(UUID tenantId) {
        return discountRepository.findByTenantId(tenantId).stream()
                .map(this::toDiscountResponse)
                .toList();
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(UUID id, UpdateDiscountRequest request) {
        var discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Descompte no trobat"));
        if (request.getValue() != null) discount.setValue(request.getValue());
        if (request.getLabel() != null) discount.setLabel(request.getLabel());
        if (request.getValidUntil() != null) discount.setValidUntil(request.getValidUntil());
        if (request.getMaxApplications() != null) discount.setMaxApplications(request.getMaxApplications());
        discount = discountRepository.save(discount);
        return toDiscountResponse(discount);
    }

    @Override
    @Transactional
    public void deactivateDiscount(UUID id) {
        var discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Descompte no trobat"));
        discount.setIsActive(false);
        discountRepository.save(discount);
    }

    private DiscountResponse toDiscountResponse(Discount discount) {
        return DiscountResponse.builder()
                .id(discount.getId())
                .tenantId(discount.getTenantId())
                .type(discount.getType())
                .value(discount.getValue())
                .appliesTo(discount.getAppliesTo())
                .referenceId(discount.getReferenceId())
                .label(discount.getLabel())
                .isActive(discount.getIsActive())
                .validFrom(discount.getValidFrom())
                .validUntil(discount.getValidUntil())
                .maxApplications(discount.getMaxApplications())
                .appliedCount(discount.getAppliedCount())
                .createdBy(discount.getCreatedBy())
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .build();
    }
}
