package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nexe_service_configs")
@IdClass(NexeServiceConfig.PK.class)
@Getter @Setter @NoArgsConstructor
public class NexeServiceConfig {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "service_key", length = 30)
    private String serviceKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public static NexeServiceConfig of(UUID tenantId, String serviceKey, String configJson) {
        var c = new NexeServiceConfig();
        c.tenantId = tenantId;
        c.serviceKey = serviceKey;
        c.configJson = configJson;
        c.updatedAt = Instant.now();
        return c;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public static class PK implements Serializable {
        private UUID tenantId;
        private String serviceKey;

        public PK() {}
        public PK(UUID tenantId, String serviceKey) {
            this.tenantId = tenantId;
            this.serviceKey = serviceKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return java.util.Objects.equals(tenantId, pk.tenantId) &&
                   java.util.Objects.equals(serviceKey, pk.serviceKey);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, serviceKey);
        }
    }
}
