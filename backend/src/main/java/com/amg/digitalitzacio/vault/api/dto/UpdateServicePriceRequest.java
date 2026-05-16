package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;

public record UpdateServicePriceRequest(BigDecimal salePrice, BigDecimal monthlyPrice, BigDecimal cost) {}
