package com.amg.digitalitzacio.content.api.dto;

import java.time.LocalDate;

/** Camps editables d'un item del pla (tots opcionals; només s'apliquen els no-null). */
public record UpdateItemRequest(
        String pillar,
        String briefText,
        String exampleText,
        String networks,
        String contentLanguage,
        LocalDate photoDeadline,
        LocalDate targetPublishDate,
        String status
) {}
