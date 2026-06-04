package com.amg.digitalitzacio.shared.notification;

public record NotificationConfigRequest(
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
}
