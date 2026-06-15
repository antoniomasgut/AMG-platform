package com.amg.digitalitzacio.documents.delivery.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptRequest(@NotBlank String signerName) {}
