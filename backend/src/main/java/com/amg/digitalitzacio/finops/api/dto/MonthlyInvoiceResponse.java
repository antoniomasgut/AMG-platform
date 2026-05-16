package com.amg.digitalitzacio.finops.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MonthlyInvoiceResponse(UUID id, UUID tenantId, String period, String invoiceNumber,
                                      BigDecimal amount, String status, LocalDate sepaCollectionDate,
                                      Boolean sepaCollected, String invoicePdfUrl, Instant createdAt) {}
