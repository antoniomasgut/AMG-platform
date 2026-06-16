package com.amg.digitalitzacio.booking.api;

import com.amg.digitalitzacio.booking.api.dto.*;
import com.amg.digitalitzacio.booking.application.BookingService;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.MeetingSettings;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ── Admin: settings ──────────────────────────────────────────

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<MeetingSettings> getSettings(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.getOrCreate(p.tenantId()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<MeetingSettings> updateSettings(
            @Valid @RequestBody MeetingSettingsRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        MeetingSettings s = bookingService.getOrCreate(p.tenantId());
        if (req.workingDays()         != null) s.setWorkingDays(req.workingDays());
        if (req.startTime()           != null) s.setStartTime(java.time.LocalTime.parse(req.startTime()));
        if (req.endTime()             != null) s.setEndTime(java.time.LocalTime.parse(req.endTime()));
        if (req.slotDurationMinutes() != null) s.setSlotDurationMinutes(req.slotDurationMinutes());
        if (req.bufferMinutes()       != null) s.setBufferMinutes(req.bufferMinutes());
        if (req.minNoticeHours()      != null) s.setMinNoticeHours(req.minNoticeHours());
        if (req.maxAdvanceDays()      != null) s.setMaxAdvanceDays(req.maxAdvanceDays());
        if (req.calendarId()          != null) s.setCalendarId(req.calendarId());
        return ResponseEntity.ok(bookingService.save(s));
    }

    // ── Admin: create booking token for a lead ───────────────────

    @PostMapping("/tokens")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> createToken(
            @RequestParam UUID leadId,
            @AuthenticationPrincipal UserPrincipal p) {
        BookingToken bt = bookingService.createToken(p.tenantId(), leadId);
        return ResponseEntity.ok(Map.of("token", bt.getToken(), "expiresAt", bt.getExpiresAt().toString()));
    }

    @GetMapping("/tokens/lead/{leadId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<BookingToken>> getTokensForLead(@PathVariable UUID leadId) {
        return ResponseEntity.ok(bookingService.getTokensForLead(leadId));
    }

    // ── Agenda: llistat de cites per al tenant ───────────────────

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserPrincipal p) {
        ZoneId zone = ZoneId.of("Europe/Madrid");
        Instant fromInstant = (from != null)
                ? LocalDate.parse(from).atStartOfDay(zone).toInstant()
                : Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        Instant toInstant = (to != null)
                ? LocalDate.parse(to).plusDays(1).atStartOfDay(zone).toInstant()
                : Instant.now().plus(60, java.time.temporal.ChronoUnit.DAYS);

        List<BookingToken> confirmed = bookingService.getAppointments(p.tenantId(), fromInstant, toInstant);
        List<BookingToken> pending   = bookingService.getPendingTokens(p.tenantId());
        return ResponseEntity.ok(Map.of("confirmed", confirmed, "pending", pending));
    }

    // ── Public: booking flow ─────────────────────────────────────

    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getTokenInfo(@PathVariable String token) {
        BookingService.TokenInfo info = bookingService.getTokenInfo(token);
        MeetingSettings s = info.settings();
        var resp = new java.util.HashMap<String, Object>();
        resp.put("leadName",            info.leadName());
        resp.put("slotDurationMinutes", s.getSlotDurationMinutes());
        resp.put("workingDays",         s.getWorkingDays());
        if (info.bookingLabel() != null) resp.put("bookingLabel", info.bookingLabel());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{token}/days")
    public ResponseEntity<List<String>> getAvailableDays(@PathVariable String token) {
        return ResponseEntity.ok(
                bookingService.getAvailableDays(token).stream()
                        .map(LocalDate::toString)
                        .toList());
    }

    @GetMapping("/{token}/slots")
    public ResponseEntity<List<String>> getSlotsForDay(
            @PathVariable String token,
            @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return ResponseEntity.ok(
                bookingService.getSlotsForDay(token, d).stream()
                        .map(t -> t.toString().substring(0, 5))
                        .toList());
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(
            @PathVariable String token,
            @Valid @RequestBody BookingConfirmRequest req) {
        LocalDateTime slot = LocalDateTime.parse(req.slotDatetime());
        BookingService.BookingResult result = bookingService.confirmBooking(token, slot);
        var resp = new java.util.HashMap<String, Object>();
        resp.put("meetingAt", result.meetingAt().toString());
        if (result.meetLink() != null) resp.put("meetLink", result.meetLink());
        return ResponseEntity.ok(resp);
    }
}
