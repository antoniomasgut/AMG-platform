package com.amg.digitalitzacio.shared.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_notification_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantNotificationConfig {

    @Id
    private UUID tenantId;

    @Builder.Default @Column(nullable = false) private boolean enabled = true;

    // Telegram per event
    @Builder.Default @Column(name = "tg_contact_form",    nullable = false) private boolean tgContactForm    = true;
    @Builder.Default @Column(name = "tg_chat_widget_new", nullable = false) private boolean tgChatWidgetNew  = true;
    @Builder.Default @Column(name = "tg_whatsapp_new",    nullable = false) private boolean tgWhatsappNew    = true;
    @Builder.Default @Column(name = "tg_email_new",       nullable = false) private boolean tgEmailNew       = true;
    @Builder.Default @Column(name = "tg_lead_created",    nullable = false) private boolean tgLeadCreated    = false;
    @Builder.Default @Column(name = "tg_booking",         nullable = false) private boolean tgBooking        = true;

    // Email per event
    @Builder.Default @Column(name = "em_contact_form", nullable = false) private boolean emContactForm = true;
    @Builder.Default @Column(name = "em_booking",      nullable = false) private boolean emBooking     = true;

    // Quiet hours
    @Column(name = "quiet_start") private Integer quietStart;
    @Column(name = "quiet_end")   private Integer quietEnd;
    @Builder.Default @Column(length = 50, nullable = false) private String timezone = "Europe/Madrid";

    @Builder.Default @Column(name = "cooldown_minutes", nullable = false) private int cooldownMinutes = 0;

    @Column(name = "updated_at") private Instant updatedAt;

    public static TenantNotificationConfig defaultFor(UUID tenantId) {
        return TenantNotificationConfig.builder().tenantId(tenantId).build();
    }

    public boolean isTelegramEnabledFor(NotificationEvent event) {
        if (!enabled) return false;
        return switch (event) {
            case CONTACT_FORM    -> tgContactForm;
            case CHAT_WIDGET_NEW -> tgChatWidgetNew;
            case WHATSAPP_NEW    -> tgWhatsappNew;
            case EMAIL_NEW       -> tgEmailNew;
            case LEAD_CREATED    -> tgLeadCreated;
            case BOOKING_CONFIRMED -> tgBooking;
        };
    }

    public boolean isEmailEnabledFor(NotificationEvent event) {
        if (!enabled) return false;
        return switch (event) {
            case CONTACT_FORM      -> emContactForm;
            case BOOKING_CONFIRMED -> emBooking;
            default -> false;
        };
    }
}
