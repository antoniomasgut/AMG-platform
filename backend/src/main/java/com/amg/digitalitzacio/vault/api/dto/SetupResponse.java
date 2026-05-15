package com.amg.digitalitzacio.vault.api.dto;

import java.util.List;
import java.util.UUID;

public record SetupResponse(
    List<ProfileSetup> profiles,
    List<AddonSetup> addons
) {
    public record ProfileSetup(
        ProfileRef profile,
        List<PhaseSetup> phases
    ) {
        public record ProfileRef(UUID id, String name, String slug) {}
        public record PhaseSetup(
            PhaseRef phase,
            String approvalStatus, String invoiceStatus, String paymentStatus, String implementationStatus,
            List<ServiceSetup> services
        ) {
            public record PhaseRef(UUID id, String name, Integer sortOrder) {}
            public record ServiceSetup(
                ServiceRef service,
                String status,
                List<FieldSetup> fields
            ) {
                public record ServiceRef(UUID id, String name, String slug, String type) {}
                public record FieldSetup(UUID id, String key, String label, Boolean isSet) {}
            }
        }
    }
    public record AddonSetup(
        ServiceRef service, Boolean approvalRequired, String approvalStatus
    ) {
        public record ServiceRef(UUID id, String name) {}
    }
}
