package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface DiscountService {
    DiscountResponse createDiscount(CreateDiscountRequest request);
    List<DiscountResponse> listDiscounts(UUID tenantId, Boolean isActive, String appliesTo);
    DiscountResponse updateDiscount(UUID discountId, UpdateDiscountRequest request);
    void deactivateDiscount(UUID discountId);
}
