package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;

public record CreateServiceRequest(String name, String slug, String description,
                                   String type, BigDecimal cost, BigDecimal salePrice,
                                   Integer sortOrder) {}
