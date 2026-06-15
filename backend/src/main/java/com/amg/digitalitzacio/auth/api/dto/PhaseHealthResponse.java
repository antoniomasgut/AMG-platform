package com.amg.digitalitzacio.auth.api.dto;

import java.util.List;

// DTO de resposta per a l'estat de salut d'una fase individual
public record PhaseHealthResponse(
    String phase,
    boolean healthy,
    List<String> issues,    // problemes detectats (llista buida si healthy)
    List<String> warnings   // avisos no bloquejants
) {}
