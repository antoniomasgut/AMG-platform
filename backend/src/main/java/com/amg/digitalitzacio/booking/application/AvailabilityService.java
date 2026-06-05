package com.amg.digitalitzacio.booking.application;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.booking.domain.MeetingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final GoogleCalendarService calendarService;

    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

    /**
     * Returns dates (within the next maxAdvanceDays) that have at least one free slot.
     */
    public List<LocalDate> getAvailableDays(MeetingSettings s) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate limit = today.plusDays(s.getMaxAdvanceDays());
        Set<DayOfWeek> allowed = parseWorkingDays(s.getWorkingDays());

        List<LocalDate> result = new ArrayList<>();
        for (LocalDate d = today; !d.isAfter(limit); d = d.plusDays(1)) {
            if (!allowed.contains(d.getDayOfWeek())) continue;
            if (!getSlotsForDay(s, d).isEmpty()) result.add(d);
        }
        return result;
    }

    /**
     * Returns all free time slots for a given day.
     */
    public List<LocalTime> getSlotsForDay(MeetingSettings s, LocalDate date) {
        ZoneId zone = ZONE;
        List<LocalTime> candidates = generateCandidates(s, date);
        if (candidates.isEmpty()) return List.of();

        Instant dayStart = date.atTime(s.getStartTime()).atZone(zone).toInstant();
        Instant dayEnd   = date.atTime(s.getEndTime()).atZone(zone).toInstant();
        List<Instant[]> busy = fetchBusy(s.getCalendarId(), dayStart, dayEnd);

        Instant minStart = Instant.now().plus(Duration.ofHours(s.getMinNoticeHours()));

        List<LocalTime> free = new ArrayList<>();
        for (LocalTime t : candidates) {
            Instant slotStart = date.atTime(t).atZone(zone).toInstant();
            Instant slotEnd   = slotStart.plus(Duration.ofMinutes(s.getSlotDurationMinutes()));
            if (slotStart.isBefore(minStart)) continue;
            if (!overlapsAny(slotStart, slotEnd, busy, s.getBufferMinutes())) {
                free.add(t);
            }
        }
        return free;
    }

    private List<LocalTime> generateCandidates(MeetingSettings s, LocalDate date) {
        if (!parseWorkingDays(s.getWorkingDays()).contains(date.getDayOfWeek())) return List.of();
        int step = s.getSlotDurationMinutes() + s.getBufferMinutes();
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cur = s.getStartTime();
        LocalTime lastStart = s.getEndTime().minusMinutes(s.getSlotDurationMinutes());
        while (!cur.isAfter(lastStart)) {
            slots.add(cur);
            cur = cur.plusMinutes(step);
        }
        return slots;
    }

    private boolean overlapsAny(Instant start, Instant end, List<Instant[]> busy, int bufferMin) {
        Duration buf = Duration.ofMinutes(bufferMin);
        for (Instant[] b : busy) {
            Instant bStart = b[0].minus(buf);
            Instant bEnd   = b[1].plus(buf);
            if (start.isBefore(bEnd) && end.isAfter(bStart)) return true;
        }
        return false;
    }

    /** Calls Google Calendar Freebusy API — returns list of [start, end] busy intervals. */
    private List<Instant[]> fetchBusy(String calendarId, Instant from, Instant to) {
        if (calendarId == null || calendarId.isBlank()) return List.of();
        try {
            return calendarService.getFreeBusySlots(calendarId, from, to);
        } catch (Exception e) {
            log.warn("Could not fetch freebusy for calendar {}: {}", calendarId, e.getMessage());
            return List.of();
        }
    }

    private Set<DayOfWeek> parseWorkingDays(String csv) {
        Set<DayOfWeek> days = new HashSet<>();
        for (String d : csv.split(",")) {
            try { days.add(DayOfWeek.valueOf(d.trim())); } catch (Exception ignored) {}
        }
        return days;
    }
}
