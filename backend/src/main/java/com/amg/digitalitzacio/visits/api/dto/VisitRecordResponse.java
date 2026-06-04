package com.amg.digitalitzacio.visits.api.dto;

import com.amg.digitalitzacio.visits.domain.VisitRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VisitRecordResponse(
    UUID id,
    String contactIdentifier,
    String contactName,
    LocalDate visitDate,
    String treatmentType,
    String notes,
    LocalDate nextVisitDue,
    Instant createdAt
) {
    public static VisitRecordResponse from(VisitRecord v) {
        return new VisitRecordResponse(
            v.getId(), v.getContactIdentifier(), v.getContactName(),
            v.getVisitDate(), v.getTreatmentType(), v.getNotes(),
            v.getNextVisitDue(), v.getCreatedAt()
        );
    }
}
