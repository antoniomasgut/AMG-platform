package com.amg.digitalitzacio.shared.notification;

public record NotificationConfigResponse(
        boolean enabled,
        TelegramEvents telegram,
        EmailEvents email,
        QuietHours quietHours,
        int cooldownMinutes
) {
    public record TelegramEvents(
            boolean contactForm, boolean chatWidgetNew, boolean whatsappNew,
            boolean emailNew, boolean leadCreated, boolean booking) {}

    public record EmailEvents(boolean contactForm, boolean booking) {}

    public record QuietHours(Integer start, Integer end, String timezone) {}

    public static NotificationConfigResponse from(TenantNotificationConfig c) {
        return new NotificationConfigResponse(
                c.isEnabled(),
                new TelegramEvents(c.isTgContactForm(), c.isTgChatWidgetNew(), c.isTgWhatsappNew(),
                        c.isTgEmailNew(), c.isTgLeadCreated(), c.isTgBooking()),
                new EmailEvents(c.isEmContactForm(), c.isEmBooking()),
                new QuietHours(c.getQuietStart(), c.getQuietEnd(), c.getTimezone()),
                c.getCooldownMinutes()
        );
    }
}
