package com.amg.digitalitzacio.documents.api.dto;

public record ParsedDocumentResponse(
    String fileName,
    String rawText,
    String extractedName,
    String extractedEmail,
    String extractedPhone,
    String extractedNotes,
    String source,
    boolean leadCreated,
    String leadId
) {}
