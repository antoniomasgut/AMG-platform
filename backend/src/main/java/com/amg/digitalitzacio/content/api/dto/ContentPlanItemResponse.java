package com.amg.digitalitzacio.content.api.dto;

import com.amg.digitalitzacio.content.domain.ContentPlanItem;

import java.time.LocalDate;
import java.util.UUID;

public record ContentPlanItemResponse(
        UUID id,
        Integer weekNumber,
        String pillar,
        String briefText,
        String exampleText,
        String networks,
        String contentLanguage,
        LocalDate photoDeadline,
        LocalDate targetPublishDate,
        String status,
        String mediaUrl,
        String caption,
        String error
) {
    public static ContentPlanItemResponse from(ContentPlanItem i) {
        return new ContentPlanItemResponse(
                i.getId(), i.getWeekNumber(),
                i.getPillar() != null ? i.getPillar().name() : null,
                i.getBriefText(), i.getExampleText(), i.getNetworks(), i.getContentLanguage(),
                i.getPhotoDeadline(), i.getTargetPublishDate(),
                i.getStatus() != null ? i.getStatus().name() : null,
                i.getMediaUrl(), i.getCaption(), i.getError());
    }
}
