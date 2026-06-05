package com.amg.digitalitzacio.booking.api.dto;

public record MeetingSettingsRequest(
        String workingDays,
        String startTime,
        String endTime,
        Integer slotDurationMinutes,
        Integer bufferMinutes,
        Integer minNoticeHours,
        Integer maxAdvanceDays,
        String calendarId
) {}
