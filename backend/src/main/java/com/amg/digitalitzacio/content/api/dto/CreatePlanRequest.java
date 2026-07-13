package com.amg.digitalitzacio.content.api.dto;

/** period: YYYY-MM · contentLanguage opcional (default = idioma del tenant) · generate: auto-crear items. */
public record CreatePlanRequest(String period, String contentLanguage, Boolean generate, String notes) {}
