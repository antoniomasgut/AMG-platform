package com.amg.digitalitzacio.google.application;

public record CalendarEventItem(
    String id,
    String summary,
    String description,
    String start,
    String end,
    String htmlLink
) {}
