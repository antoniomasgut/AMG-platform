package com.amg.digitalitzacio.shared.storage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "storage_provider_configs")
@IdClass(StorageProviderConfig.PK.class)
@Getter @Setter @NoArgsConstructor
public class StorageProviderConfig {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "provider_key", length = 30)
    private String providerKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    @PrePersist
    void onCreate() { updatedAt = Instant.now(); }

    public static StorageProviderConfig of(UUID tenantId, String providerKey, String configJson) {
        var c = new StorageProviderConfig();
        c.tenantId = tenantId;
        c.providerKey = providerKey;
        c.configJson = configJson;
        c.active = true;
        return c;
    }

    public static class PK implements Serializable {
        private UUID tenantId;
        private String providerKey;

        public PK() {}

        public PK(UUID tenantId, String providerKey) {
            this.tenantId = tenantId;
            this.providerKey = providerKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(tenantId, pk.tenantId) && Objects.equals(providerKey, pk.providerKey);
        }

        @Override
        public int hashCode() { return Objects.hash(tenantId, providerKey); }
    }
}
