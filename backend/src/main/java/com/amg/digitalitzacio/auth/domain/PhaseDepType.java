package com.amg.digitalitzacio.auth.domain;

public enum PhaseDepType {
    BASE,      // F1 sempre obligatòria — punt d'entrada
    OPTIONAL,  // Es pot contractar sense les predecessores
    REQUIRED   // Dependència tècnica real — no es pot saltar
}
