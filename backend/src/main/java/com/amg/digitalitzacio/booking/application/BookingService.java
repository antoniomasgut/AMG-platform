package com.amg.digitalitzacio.booking.application;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.booking.domain.*;
import com.amg.digitalitzacio.leads.domain.*;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final MeetingSettingsRepository settingsRepo;
    private final BookingTokenRepository tokenRepo;
    private final LeadRepository leadRepo;
    private final ActivityRepository activityRepo;
    private final GoogleCalendarService calendarService;
    private final AvailabilityService availabilityService;
    private final EmailService emailService;
    private final SystemConfigService sysConfig;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final ObjectMapper objectMapper;

    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private static final SecureRandom RNG = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("ca"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    // Sentinel userId per a accions del sistema sense usuari real
    private static final UUID SYSTEM_USER = new UUID(0L, 0L);

    // ── Settings ────────────────────────────────────────────────

    public MeetingSettings getOrCreate(UUID tenantId) {
        return settingsRepo.findById(tenantId).orElseGet(() -> {
            var s = new MeetingSettings();
            s.setTenantId(tenantId);
            return settingsRepo.save(s);
        });
    }

    public MeetingSettings save(MeetingSettings s) {
        return settingsRepo.save(s);
    }

    // ── Agenda ───────────────────────────────────────────────────

    public List<BookingToken> getAppointments(UUID tenantId, Instant from, Instant to) {
        return tokenRepo.findByTenantIdAndMeetingAtBetween(tenantId, from, to);
    }

    public List<BookingToken> getPendingTokens(UUID tenantId) {
        return tokenRepo.findPendingByTenantId(tenantId, Instant.now());
    }

    // ── Token creation ───────────────────────────────────────────

    @Transactional
    public BookingToken createToken(UUID tenantId, UUID leadId) {
        Lead lead = leadRepo.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));

        // Invalidate previous unused tokens for this lead
        tokenRepo.findByLeadId(leadId).stream()
                .filter(t -> !t.isConfirmed() && t.getExpiresAt().isAfter(Instant.now()))
                .forEach(t -> { t.setExpiresAt(Instant.now()); tokenRepo.save(t); });

        var token = new BookingToken();
        token.setTenantId(tenantId);
        token.setLeadId(leadId);
        token.setToken(generateToken());
        token.setLeadName(lead.getName());
        token.setLeadEmail(lead.getEmail());
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        return tokenRepo.save(token);
    }

    // ── Public: token info + availability ───────────────────────

    public record TokenInfo(String leadName, MeetingSettings settings, String bookingLabel) {}

    public TokenInfo getTokenInfo(String token) {
        BookingToken bt = findValidToken(token);
        MeetingSettings s = getOrCreate(bt.getTenantId());
        return new TokenInfo(bt.getLeadName(), s, bt.getBookingLabel());
    }

    // ── Token creation from document ─────────────────────────────

    @Transactional
    public BookingToken createTokenFromDocument(UUID tenantId, String recipientEmail,
                                                String recipientPhone, String recipientName,
                                                UUID sourceDocumentId) {
        String label = resolveBookingLabel(tenantId);

        // Invalida tokens anteriors per al mateix document
        if (sourceDocumentId != null) {
            tokenRepo.findActiveBySourceDocumentId(sourceDocumentId, Instant.now())
                    .forEach(t -> { t.setExpiresAt(Instant.now()); tokenRepo.save(t); });
        }

        var bt = new BookingToken();
        bt.setTenantId(tenantId);
        bt.setToken(generateToken());
        bt.setLeadEmail(recipientEmail);
        bt.setRecipientPhone(recipientPhone);
        bt.setRecipientName(recipientName);
        bt.setLeadName(recipientName != null ? recipientName : "");
        bt.setSourceDocumentId(sourceDocumentId);
        bt.setBookingLabel(label);
        bt.setExpiresAt(Instant.now().plus(Duration.ofDays(14)));
        return tokenRepo.save(bt);
    }

    private String resolveBookingLabel(UUID tenantId) {
        return nexeServiceConfigService.get(tenantId, "AGENDA").map(cfg -> {
            try {
                var node = objectMapper.readTree(cfg.getConfigJson());
                String mode = node.path("mode").asText("");
                return switch (mode) {
                    case "vehicle"    -> "Programa l'entrega del vehicle";
                    case "inspection" -> "Concreta el dia d'inici";
                    case "meeting"    -> "Concerta una reunió";
                    default           -> "Reserva la teva cita";
                };
            } catch (Exception e) {
                return "Reserva la teva cita";
            }
        }).orElse("Reserva la teva cita");
    }

    public List<LocalDate> getAvailableDays(String token) {
        BookingToken bt = findValidToken(token);
        MeetingSettings s = getOrCreate(bt.getTenantId());
        return availabilityService.getAvailableDays(s);
    }

    public List<LocalTime> getSlotsForDay(String token, LocalDate date) {
        BookingToken bt = findValidToken(token);
        MeetingSettings s = getOrCreate(bt.getTenantId());
        return availabilityService.getSlotsForDay(s, date);
    }

    // ── Confirm booking ──────────────────────────────────────────

    public record BookingResult(String meetLink, LocalDateTime meetingAt) {}

    @Transactional
    public BookingResult confirmBooking(String token, LocalDateTime slotLocal) {
        BookingToken bt = findValidToken(token);
        if (bt.isConfirmed()) throw new IllegalStateException("Ja confirmat");

        MeetingSettings s = getOrCreate(bt.getTenantId());

        List<LocalTime> slots = availabilityService.getSlotsForDay(s, slotLocal.toLocalDate());
        if (!slots.contains(slotLocal.toLocalTime()))
            throw new IllegalStateException("La franja ja no està disponible");

        String meetLink = calendarService.createEventWithMeet(
                s.getCalendarId(),
                "Reunió AMG · " + bt.getLeadName(),
                slotLocal,
                s.getSlotDurationMinutes(),
                "Reunió de presentació de serveis AMG Digitalitzacions",
                bt.getLeadEmail()
        );

        bt.setConfirmed(true);
        bt.setMeetingAt(slotLocal.atZone(ZONE).toInstant());
        bt.setMeetLink(meetLink);
        tokenRepo.save(bt);

        if (bt.getLeadId() != null) {
            leadRepo.findById(bt.getLeadId()).ifPresent(lead -> {
                if (lead.getStage() == PipelineStage.NEW || lead.getStage() == PipelineStage.CONTACTED) {
                    lead.setStage(PipelineStage.QUALIFIED);
                    leadRepo.save(lead);
                }
                var act = new Activity();
                act.setLeadId(lead.getId());
                act.setUserId(SYSTEM_USER);
                act.setType(ActivityType.MEETING);
                act.setDescription("Reunió programada · " + formatDateTime(slotLocal)
                        + (meetLink != null ? "\nGoogle Meet: " + meetLink : ""));
                act.setDueDate(slotLocal.atZone(ZONE).toInstant());
                activityRepo.save(act);
            });
        }

        sendConfirmationEmails(bt, slotLocal, meetLink, s);

        return new BookingResult(meetLink, slotLocal);
    }

    // ── Tokens per lead (per mostrar al CRM) ────────────────────

    public List<BookingToken> getTokensForLead(UUID leadId) {
        return tokenRepo.findByLeadId(leadId);
    }

    // ── Token lookup ─────────────────────────────────────────────

    public BookingToken findValidToken(String token) {
        return tokenRepo.findByToken(token)
                .filter(bt -> bt.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Enllaç invàlid o expirat"));
    }

    // ── Emails ───────────────────────────────────────────────────

    private void sendConfirmationEmails(BookingToken bt, LocalDateTime slot, String meetLink, MeetingSettings s) {
        String dateStr = formatDateTime(slot);
        String meetLine = meetLink != null ? "\nEnllaç Google Meet: " + meetLink : "";

        if (bt.getLeadEmail() != null && !bt.getLeadEmail().isBlank()) {
            try {
                emailService.sendEmail(bt.getLeadEmail(),
                        "Confirmació de la teva reunió amb AMG Digitalitzacions",
                        "Hola " + bt.getLeadName() + ",\n\n"
                        + "La teva reunió ha quedat confirmada:\n"
                        + "Data: " + dateStr + "\n"
                        + "Durada: " + s.getSlotDurationMinutes() + " minuts" + meetLine + "\n\n"
                        + "No necessites instal·lar res — el link de Google Meet s'obre directament al navegador.\n\n"
                        + "Fins aviat!\nEquip AMG Digitalitzacions");
            } catch (Exception e) {
                log.warn("No s'ha pogut enviar confirmació al lead {}: {}", bt.getLeadEmail(), e.getMessage());
            }
        }

        String amgEmail = sysConfig.get("BREVO_SENDER_EMAIL");
        if (amgEmail != null && !amgEmail.isBlank()) {
            try {
                emailService.sendEmail(amgEmail,
                        "[Nova reserva] " + bt.getLeadName() + " · " + dateStr,
                        "S'ha confirmat una nova reunió:\n\n"
                        + "Lead: " + bt.getLeadName() + "\n"
                        + "Email: " + (bt.getLeadEmail() != null ? bt.getLeadEmail() : "—") + "\n"
                        + "Data: " + dateStr + meetLine);
            } catch (Exception e) {
                log.warn("No s'ha pogut enviar notificació interna de reserva: {}", e.getMessage());
            }
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt.format(DATE_FMT) + " a les " + dt.format(TIME_FMT) + "h";
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
