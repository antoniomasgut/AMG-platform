package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocument;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocumentRepository;
import com.amg.digitalitzacio.google.application.GoogleBusinessReviewSyncService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Comandes Telegram per a tenants (no AMG admin).
 * Registrades al TelegramWebhookController.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantTelegramCommandService {

    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("d/MM HH:mm");

    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final BookingTokenRepository bookingTokenRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final GoogleBusinessReviewSyncService reviewSyncService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final ObjectMapper objectMapper;

    // ── /ajuda ────────────────────────────────────────────────────────────────

    public String handleAjuda(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        var link = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        boolean hasF1 = tenant != null && tenant.isPhaseActive("F1");
        boolean hasF2 = tenant != null && tenant.isPhaseActive("F2");
        boolean hasF3 = tenant != null && tenant.isPhaseActive("F3");
        boolean hasF4 = tenant != null && tenant.isPhaseActive("F4");

        String mode = link != null ? modeLabel(link.getAgentMode()) : "—";

        var sb = new StringBuilder();
        sb.append("📋 <b>Comandes disponibles</b>\n\n");

        sb.append("<b>Generals</b>\n");
        sb.append("  /ajuda — aquesta llista\n\n");

        if (hasF1) {
            sb.append("<b>Agent IA</b> · Mode actual: ").append(mode).append("\n");
            sb.append("  /mode auto — respon automàticament\n");
            sb.append("  /mode manual — t'avisa però no respon\n");
            sb.append("  /mode hybrid — guarda per a la teva revisió\n");
            sb.append("  /stats — resum d'activitat d'avui\n\n");
        }

        if (hasF2) {
            sb.append("<b>Agenda</b>\n");
            sb.append("  /agenda — cites d'avui\n");
            sb.append("  /agenda demà\n");
            sb.append("  /agenda setmana\n");
            sb.append("  /agenda 2026-07-10 — dia concret\n");
            sb.append("  /absencia [data] — marcar absència\n");
            sb.append("  /festiu [data] — marcar festiu\n");
            sb.append("  /cancel 10:30 — cancel·lar cita\n\n");
        }

        if (hasF3) {
            sb.append("<b>Documents</b>\n");
            sb.append("  /pressupost email Nom [notes]\n");
            sb.append("  /pendents — documents sense resposta\n\n");
        }

        if (hasF4) {
            sb.append("<b>Ressenyes</b>\n");
            sb.append("  /reviews — darreres ressenyes de Google\n\n");
        }

        boolean hasSocialPublisher = nexeServiceConfigService.get(tenantId, "SOCIAL_PUBLISHER").isPresent();
        if (hasSocialPublisher) {
            sb.append("<b>Xarxes socials</b>\n");
            sb.append("  /publica — publicar a Instagram, Facebook i Google Business\n\n");
        }

        if (!hasF1 && !hasF2 && !hasF3 && !hasF4 && !hasSocialPublisher) {
            return "ℹ️ Encara no tens cap servei actiu. Contacta l'administrador per activar els teus serveis.";
        }

        return sb.toString().trim();
    }

    // ── /mode ─────────────────────────────────────────────────────────────────

    @Transactional
    public String handleMode(UUID tenantId, String commandText) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isPhaseActive("F1")) {
            return "ℹ️ L'agent IA no està activat per al teu compte.";
        }

        var linkOpt = chatLinkRepository.findByTenantId(tenantId);
        if (linkOpt.isEmpty()) return "⚠️ No s'ha trobat la configuració del teu compte.";

        var arg = commandText.replaceFirst("(?i)/mode\\s*", "").trim().toLowerCase();

        AgentMode newMode = switch (arg) {
            case "auto"   -> AgentMode.AUTO;
            case "manual", "off" -> AgentMode.MANUAL;
            case "hybrid" -> AgentMode.HYBRID;
            default -> null;
        };

        if (newMode == null) {
            var link = linkOpt.get();
            return "Mode actual: <b>" + modeLabel(link.getAgentMode()) + "</b>\n\n"
                 + "Opcions:\n"
                 + "  /mode auto — respon automàticament\n"
                 + "  /mode hybrid — guarda per a la teva revisió\n"
                 + "  /mode manual — no respon, t'avisa\n";
        }

        var link = linkOpt.get();
        link.setAgentMode(newMode);
        chatLinkRepository.save(link);

        return "✅ Mode canviat a <b>" + modeLabel(newMode) + "</b>.";
    }

    // ── /stats ────────────────────────────────────────────────────────────────

    public String handleStats(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isPhaseActive("F1")) {
            return "ℹ️ L'agent IA no està activat per al teu compte.";
        }

        Instant startOfDay = LocalDate.now(TZ).atStartOfDay(TZ).toInstant();
        Instant startOfWeek = LocalDate.now(TZ).with(java.time.DayOfWeek.MONDAY).atStartOfDay(TZ).toInstant();

        long convsToday   = conversationRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfDay);
        long convsWeek    = conversationRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfWeek);
        long pendingApproval = conversationRepository.countByTenantIdAndPendingApprovalTrue(tenantId);
        long totalContacts = contactRepository.findByTenantId(tenantId).size();

        var link = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        String mode = link != null ? modeLabel(link.getAgentMode()) : "—";

        return "📊 <b>Activitat del teu agent</b>\n\n"
             + "Avui\n"
             + "  💬 Converses noves: <b>" + convsToday + "</b>\n"
             + (pendingApproval > 0
                ? "  ⏳ Pendents de revisió: <b>" + pendingApproval + "</b>\n"
                : "  ✅ Cap missatge pendent de revisió\n")
             + "\nAquesta setmana\n"
             + "  💬 Converses: <b>" + convsWeek + "</b>\n\n"
             + "Acumulat\n"
             + "  👥 Contactes totals: <b>" + totalContacts + "</b>\n"
             + "  🤖 Mode agent: <b>" + mode + "</b>";
    }

    // ── /pendents ─────────────────────────────────────────────────────────────

    public String handlePendents(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isPhaseActive("F3")) {
            return "ℹ️ La gestió de documents no està activada per al teu compte.";
        }

        var docs = documentRepository.findByTenantIdAndStatus(tenantId, DocumentStatus.SENT);
        if (docs.isEmpty()) {
            return "✅ Cap document pendent de resposta.";
        }

        var sb = new StringBuilder();
        sb.append("📄 <b>Documents sense resposta</b> · ").append(docs.size()).append("\n\n");

        for (var doc : docs) {
            String clientName = extractCustomerName(doc);
            sb.append("• <b>").append(doc.getNumber()).append("</b>");
            if (clientName != null) sb.append(" — ").append(escapeHtml(clientName));
            long daysAgo = Duration.between(doc.getCreatedAt(), Instant.now()).toDays();
            if (daysAgo > 0) sb.append(" · fa ").append(daysAgo).append("d");
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    // ── /cancel ───────────────────────────────────────────────────────────────

    @Transactional
    public String handleCancel(UUID tenantId, String commandText) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isPhaseActive("F2")) {
            return "ℹ️ La gestió d'agenda no està activada per al teu compte.";
        }

        var arg = commandText.replaceFirst("(?i)/cancel\\s*", "").trim();
        if (arg.isBlank()) {
            return "Indica l'hora de la cita a cancel·lar.\nExemples:\n  /cancel 10:30\n  /cancel demà 10:30\n  /cancel 2026-07-10 10:30";
        }

        LocalDateTime targetDt = parseDateTime(arg);
        if (targetDt == null) {
            return "No entenc el format. Exemples:\n  /cancel 10:30\n  /cancel demà 10:30\n  /cancel 2026-07-10 10:30";
        }

        // Busquem cites en una finestra de ±30 min al voltant de l'hora indicada
        Instant targetInst = targetDt.atZone(TZ).toInstant();
        Instant from = targetInst.minus(Duration.ofMinutes(30));
        Instant to   = targetInst.plus(Duration.ofMinutes(30));

        var candidates = bookingTokenRepository
                .findByTenantIdAndMeetingAtBetween(tenantId, from, to)
                .stream()
                .filter(BookingToken::isConfirmed)
                .sorted(Comparator.comparingLong(b ->
                        Math.abs(Duration.between(b.getMeetingAt(), targetInst).toMinutes())))
                .toList();

        if (candidates.isEmpty()) {
            return "No s'ha trobat cap cita confirmada al voltant de les "
                 + targetDt.toLocalTime().format(TIME_FMT)
                 + " del " + targetDt.toLocalDate().format(DateTimeFormatter.ofPattern("d/MM/yyyy")) + ".";
        }

        var booking = candidates.get(0);
        String clientName = booking.getRecipientName() != null ? booking.getRecipientName() : booking.getLeadName();
        String slotStr = booking.getMeetingAt().atZone(TZ).format(DT_FMT);

        // Cancel·lem
        booking.setConfirmed(false);
        booking.setMeetingAt(null);
        bookingTokenRepository.save(booking);

        // Intentem notificar el client
        notifyClientCancellation(tenantId, booking, clientName, slotStr);

        return "✅ Cita cancel·lada: <b>" + escapeHtml(clientName != null ? clientName : "Client") + "</b>"
             + " · " + slotStr
             + (hasContactInfo(booking) ? "\nS'ha enviat una notificació al client." : "");
    }

    // ── /reviews ──────────────────────────────────────────────────────────────

    public String handleReviews(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isPhaseActive("F4")) {
            return "ℹ️ La fidelització no està activada per al teu compte.";
        }

        var reviews = reviewSyncService.getReviews(tenantId, 1, 5);
        if (reviews.isEmpty()) {
            return "📝 Encara no hi ha ressenyes sincronitzades.\nAssegura't que tens el compte de Google Business configurat.";
        }

        var sb = new StringBuilder();
        sb.append("⭐ <b>Darreres ressenyes de Google</b>\n\n");

        for (var r : reviews) {
            sb.append(stars(r.getRating())).append(" <b>").append(r.getRating()).append("/5</b>");
            if (r.getAuthorName() != null) sb.append(" · ").append(escapeHtml(r.getAuthorName()));
            sb.append("\n");
            if (r.getComment() != null && !r.getComment().isBlank()) {
                String snippet = r.getComment().length() > 160
                        ? r.getComment().substring(0, 157) + "…"
                        : r.getComment();
                sb.append("<i>").append(escapeHtml(snippet)).append("</i>\n");
            }
            sb.append("\n");
        }

        double avg = reviews.stream().mapToInt(r -> r.getRating()).average().orElse(0);
        sb.append("Mitjana: <b>").append(String.format("%.1f", avg)).append("/5</b>");

        return sb.toString().trim();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDateTime parseDateTime(String arg) {
        LocalDate today = LocalDate.now(TZ);
        arg = arg.trim().toLowerCase();

        // "demà 10:30" o "dema 10:30"
        if (arg.startsWith("demà ") || arg.startsWith("dema ")) {
            String timePart = arg.replaceFirst("(?i)dem[àa]\\s+", "").trim();
            return parseTime(timePart).map(t -> today.plusDays(1).atTime(t)).orElse(null);
        }

        // "2026-07-10 10:30"
        if (arg.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}")) {
            try {
                var parts = arg.split("\\s+", 2);
                var date = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return parseTime(parts[1]).map(t -> date.atTime(t)).orElse(null);
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        // "10:30" → avui
        return parseTime(arg).map(t -> today.atTime(t)).orElse(null);
    }

    private Optional<LocalTime> parseTime(String s) {
        try {
            return Optional.of(LocalTime.parse(s.trim(),
                    DateTimeFormatter.ofPattern(s.contains(":") ? "H:mm" : "HH")));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private void notifyClientCancellation(UUID tenantId, BookingToken b,
                                          String clientName, String slotStr) {
        String msg = "La teva cita del " + slotStr + " ha estat cancel·lada. "
                   + "Disculpa les molèsties — pots tornar a reservar quan vulguis.";
        try {
            var link = chatLinkRepository.findByTenantId(tenantId).orElse(null);
            if (b.getRecipientPhone() != null && !b.getRecipientPhone().isBlank() && link != null) {
                if (link.getWhatsappMetaPhoneNumberId() != null && !link.getWhatsappMetaPhoneNumberId().isBlank()) {
                    whatsAppMetaChannel.sendMessage(link.getWhatsappMetaPhoneNumberId(), b.getRecipientPhone(), msg);
                } else if (link.getWhatsappPhoneNumber() != null && !link.getWhatsappPhoneNumber().isBlank()) {
                    whatsAppChannel.sendMessage(link.getWhatsappPhoneNumber(), b.getRecipientPhone(), msg);
                }
            } else if (b.getLeadEmail() != null && !b.getLeadEmail().isBlank()) {
                emailChannel.sendMessage(b.getLeadEmail(), "Cita cancel·lada", msg);
            }
        } catch (Exception e) {
            log.warn("[Cancel] No s'ha pogut notificar el client {}: {}", clientName, e.getMessage());
        }
    }

    private boolean hasContactInfo(BookingToken b) {
        return (b.getRecipientPhone() != null && !b.getRecipientPhone().isBlank())
            || (b.getLeadEmail() != null && !b.getLeadEmail().isBlank());
    }

    private String extractCustomerName(GeneratedDocument doc) {
        try {
            Map<String, Object> data = objectMapper.readValue(
                    doc.getCustomerData(), new TypeReference<>() {});
            Object name = data.get("name");
            return name instanceof String s && !s.isBlank() ? s : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String modeLabel(AgentMode mode) {
        return switch (mode) {
            case AUTO   -> "Auto (respon sol)";
            case HYBRID -> "Hybrid (revisió prèvia)";
            case MANUAL -> "Manual (no respon)";
        };
    }

    private String stars(int rating) {
        return "⭐".repeat(Math.max(0, Math.min(5, rating)));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
