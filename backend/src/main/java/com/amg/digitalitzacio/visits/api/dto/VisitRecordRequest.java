package com.amg.digitalitzacio.visits.api.dto;

import java.time.LocalDate;

public record VisitRecordRequest(
    String contactIdentifier,
    String contactName,
    LocalDate visitDate,
    String treatmentType,
    String notes,
    LocalDate nextVisitDue
) {}
