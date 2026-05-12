package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SetupResponse {
    private List<ProfileSetup> profiles;
    private List<AddonSetup> addons;

    @Data
    @Builder
    public static class ProfileSetup {
        private ProfileRef profile;
        private List<PhaseSetup> phases;

        @Data
        @Builder
        public static class ProfileRef {
            private UUID id;
            private String name;
            private String slug;
        }
    }

    @Data
    @Builder
    public static class PhaseSetup {
        private PhaseRef phase;
        private String approvalStatus;
        private String paymentStatus;
        private String implementationStatus;
        private List<ServiceSetup> services;

        @Data
        @Builder
        public static class PhaseRef {
            private UUID id;
            private String name;
            private int sortOrder;
        }
    }

    @Data
    @Builder
    public static class ServiceRef {
        private UUID id;
        private String name;
        private String type;
    }

    @Data
    @Builder
    public static class ServiceSetup {
        private ServiceRef service;
        private String status;
        private List<FieldSetup> fields;
    }

    @Data
    @Builder
    public static class FieldSetup {
        private UUID id;
        private String key;
        private String label;
        private boolean isSet;
        private String maskedValue;
        private String clearValue;
    }

    @Data
    @Builder
    public static class AddonSetup {
        private ServiceRef service;
        private boolean approvalRequired;
        private String approvalStatus;
    }
}
