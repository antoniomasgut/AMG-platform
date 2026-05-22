package com.amg.digitalitzacio.agents.api.dto;

public record AIConfigRequest(String preferredModel, Integer maxTokens, Double temperature) {}
