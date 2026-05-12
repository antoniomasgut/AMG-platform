package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.DiscountResponse;
import com.amg.digitalitzacio.billing.api.dto.CreateDiscountRequest;
import com.amg.digitalitzacio.billing.api.dto.UpdateDiscountRequest;

import java.util.List;
import java.util.UUID;

public interface DiscountService {

    DiscountResponse createDiscount(CreateDiscountRequest request, UUID createdBy);

    List<DiscountResponse> listDiscounts(UUID tenantId);

    DiscountResponse updateDiscount(UUID id, UpdateDiscountRequest request);

    void deactivateDiscount(UUID id);
}
