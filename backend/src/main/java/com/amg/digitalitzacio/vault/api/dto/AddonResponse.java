package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddonResponse(Boolean approvalRequired, BigDecimal salePrice, String status) {}
