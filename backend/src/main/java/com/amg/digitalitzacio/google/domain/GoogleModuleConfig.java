package com.amg.digitalitzacio.google.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_module_configs")
@Getter @Setter @NoArgsConstructor
public class GoogleModuleConfig {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "drive_enabled", nullable = false)
    private boolean driveEnabled;

    @Column(name = "gmail_enabled", nullable = false)
    private boolean gmailEnabled;

    @Column(name = "calendar_enabled", nullable = false)
    private boolean calendarEnabled;

    @Column(name = "sheets_enabled", nullable = false)
    private boolean sheetsEnabled;

    @Column(name = "drive_folder_id")
    private String driveFolderId;

    @Column(name = "business_enabled", nullable = false)
    private boolean businessEnabled;

    @Column(name = "business_location_id")
    private String businessLocationId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public static GoogleModuleConfig defaults(UUID tenantId) {
        var c = new GoogleModuleConfig();
        c.tenantId = tenantId;
        c.driveEnabled = false;
        c.gmailEnabled = false;
        c.calendarEnabled = false;
        c.sheetsEnabled = false;
        return c;
    }
}
