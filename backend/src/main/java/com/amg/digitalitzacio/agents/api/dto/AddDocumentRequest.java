package com.amg.digitalitzacio.agents.api.dto;

public record AddDocumentRequest(
    String filename,
    String content
) {}
