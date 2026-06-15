package com.amg.digitalitzacio.documents.delivery.api.dto;

import com.amg.digitalitzacio.documents.delivery.domain.DocumentSensitivity;
import com.amg.digitalitzacio.documents.delivery.domain.DocumentViewType;
import com.amg.digitalitzacio.documents.delivery.domain.SourceEntityType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IssueTokenRequest(
    @NotBlank String storageFileId,
    @NotBlank String fileName,
    @NotBlank String mimeType,
    @NotNull  DocumentSensitivity sensitivity,
    @Email @NotBlank String recipientEmail,
    String recipientPhone,
    String recipientName,
    String description,
    DocumentViewType documentType,
    UUID sourceEntityId,
    SourceEntityType sourceEntityType
) {}
