package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MonitoringResponse {

    @Data
    @Builder
    public static class InvoiceMonitoring {
        private List<PhaseInvoice> phases;
        private int pendingInvoices;
        private int overdueInvoices;
        private BigDecimal totalPaid;

        @Data
        @Builder
        public static class PhaseInvoice {
            private PhaseRef phase;
            private String invoiceId;
            private BigDecimal amount;
            private String invoiceStatus;
            private Instant paidAt;

            @Data
            @Builder
            public static class PhaseRef {
                private String name;
                private int sortOrder;
            }
        }
    }

    @Data
    @Builder
    public static class PaymentMonitoring {
        private List<PhasePayment> phases;
        private int pendingPayments;
        private int failedPayments;

        @Data
        @Builder
        public static class PhasePayment {
            private PhaseRef phase;
            private BigDecimal amount;
            private String paymentStatus;
            private Instant paidAt;
            private String paymentMethod;

            @Data
            @Builder
            public static class PhaseRef {
                private String name;
            }
        }
    }

    @Data
    @Builder
    public static class PhaseMonitoring {
        private int totalPhases;
        private int pendingApproval;
        private int inProgress;
        private int completed;
        private int rejected;
        private List<PhaseStatus> phases;

        @Data
        @Builder
        public static class PhaseStatus {
            private String name;
            private String approvalStatus;
            private String implementationStatus;
            private String progress;
        }
    }
}
