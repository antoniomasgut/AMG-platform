package com.amg.digitalitzacio.vault.api.dto;

public record CreatePhaseRequest(String name, String description, Integer sortOrder, Integer sectorPhaseNumber) {}
