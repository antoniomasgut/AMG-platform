package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;

public record UpdateEarlyAdopterRequest(
    Integer maxSlots,
    BigDecimal setupDiscountPct,
    BigDecimal monthlyDiscountPct,
    Integer commitmentMonths,
    Boolean active
) {}
