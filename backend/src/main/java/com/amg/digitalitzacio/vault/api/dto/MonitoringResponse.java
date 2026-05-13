package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MonitoringResponse() {
    public record InvoiceMonitoring(List<PhaseInvoice> phases, int pendingInvoices, int overdueInvoices, BigDecimal totalPaid) {
        public record PhaseInvoice(String name, Integer sortOrder, String invoiceId, BigDecimal amount,
                                   String invoiceStatus, Instant paidAt) {}
    }
    public record PaymentMonitoring(List<PhasePayment> phases, int pendingPayments, int failedPayments) {
        public record PhasePayment(String name, BigDecimal amount, String paymentStatus,
                                   Instant paidAt, String paymentMethod) {}
    }
    public record PhaseMonitoring(int totalPhases, int pendingApproval, int inProgress, int completed, int rejected,
                                  List<PhaseProgress> phases) {
        public record PhaseProgress(String name, String approvalStatus, String implementationStatus, String progress) {}
    }
}
