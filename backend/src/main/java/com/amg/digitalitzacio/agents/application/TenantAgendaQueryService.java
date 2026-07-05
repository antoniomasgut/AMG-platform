package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * F2 — Consulta de l'agenda via Telegram.
 * Comandes suportades:
 *   /agenda            → cites d'avui
 *   /agenda avui       → cites d'avui
 *   /agenda demà       → cites de demà
 *   /agenda setmana    → cites dels propers 7 dies
 *   /agenda 2026-07-10 → cites d'un dia concret (format YYYY-MM-DD)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAgendaQueryService {

    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("ca"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BookingTokenRepository bookingTokenRepository;
    private final TenantRepository tenantRepository;

    /**
     * Processa la comanda /agenda del Telegram del tenant.
     * Retorna el text de resposta formatat per enviar-lo de tornada.
     */
    public String handleCommand(UUID tenantId, String commandText) {
        // Comprova que el tenant té F2 activa
        var tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty() || !tenantOpt.get().isPhaseActive("F2")) {
            return "ℹ️ La gestió d'agenda no està activada per al teu compte. Contacta amb l'administrador per activar-la.";
        }

        var arg = commandText.replaceFirst("(?i)/agenda\\s*", "").trim().toLowerCase();

        LocalDate today = LocalDate.now(TZ);

        if (arg.isBlank() || arg.equals("avui")) {
            return buildDayResponse(tenantId, today, "Avui");
        }

        if (arg.equals("demà") || arg.equals("dema")) {
            return buildDayResponse(tenantId, today.plusDays(1), "Demà");
        }

        if (arg.equals("setmana")) {
            return buildRangeResponse(tenantId, today, today.plusDays(6), "Pròxims 7 dies");
        }

        // Intent de data concreta YYYY-MM-DD
        try {
            var date = LocalDate.parse(arg, ISO_DATE);
            String label = date.format(DateTimeFormatter.ofPattern("d/MM/yyyy"));
            return buildDayResponse(tenantId, date, label);
        } catch (DateTimeParseException ignored) {
            // no és una data vàlida
        }

        return """
                No entenc el format. Exemples:
                • /agenda — cites d'avui
                • /agenda demà
                • /agenda setmana
                • /agenda 2026-07-10
                """;
    }

    private String buildDayResponse(UUID tenantId, LocalDate date, String label) {
        Instant from = date.atStartOfDay(TZ).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(TZ).toInstant();

        var bookings = bookingTokenRepository
                .findByTenantIdAndMeetingAtBetween(tenantId, from, to)
                .stream()
                .filter(BookingToken::isConfirmed)
                .sorted((a, b) -> a.getMeetingAt().compareTo(b.getMeetingAt()))
                .toList();

        if (bookings.isEmpty()) {
            return "📅 <b>" + label + "</b> — cap cita agendada.";
        }

        var sb = new StringBuilder();
        sb.append("📅 <b>").append(label).append("</b>");
        sb.append(" · ").append(bookings.size())
          .append(bookings.size() == 1 ? " cita" : " cites").append("\n\n");

        for (var b : bookings) {
            var localTime = b.getMeetingAt().atZone(TZ).toLocalTime();
            sb.append("🕐 <b>").append(localTime.format(TIME_FMT)).append("</b>  ");
            sb.append(escapeHtml(displayName(b))).append("\n");
            if (b.getMeetLink() != null && !b.getMeetLink().isBlank()) {
                sb.append("   🔗 ").append(b.getMeetLink()).append("\n");
            }
            if (b.getBookingLabel() != null && !b.getBookingLabel().isBlank()) {
                sb.append("   📌 ").append(escapeHtml(b.getBookingLabel())).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private String buildRangeResponse(UUID tenantId, LocalDate from, LocalDate to, String label) {
        Instant fromInst = from.atStartOfDay(TZ).toInstant();
        Instant toInst = to.plusDays(1).atStartOfDay(TZ).toInstant();

        var bookings = bookingTokenRepository
                .findByTenantIdAndMeetingAtBetween(tenantId, fromInst, toInst)
                .stream()
                .filter(BookingToken::isConfirmed)
                .sorted((a, b) -> a.getMeetingAt().compareTo(b.getMeetingAt()))
                .toList();

        if (bookings.isEmpty()) {
            return "📅 <b>" + label + "</b> — cap cita agendada.";
        }

        var sb = new StringBuilder();
        sb.append("📅 <b>").append(label).append("</b>");
        sb.append(" · ").append(bookings.size())
          .append(bookings.size() == 1 ? " cita" : " cites").append("\n\n");

        LocalDate current = null;
        for (var b : bookings) {
            var zdt = b.getMeetingAt().atZone(TZ);
            var day = zdt.toLocalDate();
            if (!day.equals(current)) {
                if (current != null) sb.append("\n");
                sb.append("<b>").append(capitalize(day.format(DATE_FMT))).append("</b>\n");
                current = day;
            }
            sb.append("  🕐 ").append(zdt.toLocalTime().format(TIME_FMT))
              .append("  ").append(escapeHtml(displayName(b))).append("\n");
        }

        return sb.toString().trim();
    }

    private String displayName(BookingToken b) {
        if (b.getRecipientName() != null && !b.getRecipientName().isBlank()) {
            return b.getRecipientName();
        }
        if (b.getLeadName() != null && !b.getLeadName().isBlank()) {
            return b.getLeadName();
        }
        return "Client";
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
