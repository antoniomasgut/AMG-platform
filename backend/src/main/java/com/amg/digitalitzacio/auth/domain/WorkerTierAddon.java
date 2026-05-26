package com.amg.digitalitzacio.auth.domain;

import java.math.BigDecimal;

public enum WorkerTierAddon {
    AUTONOMO(0, 0),   // Solo — 1 treballador
    PETIT(99, 20),    // Petit — 2-3 treballadors
    MITJA(199, 35),   // Equip — 4-6 treballadors
    EMPRESA(299, 50); // Empresa — 7+ treballadors

    private final int setupAddon;
    private final int monthlyAddon;

    WorkerTierAddon(int setupAddon, int monthlyAddon) {
        this.setupAddon = setupAddon;
        this.monthlyAddon = monthlyAddon;
    }

    public BigDecimal setup() { return BigDecimal.valueOf(setupAddon); }
    public BigDecimal monthly() { return BigDecimal.valueOf(monthlyAddon); }

    public static WorkerTierAddon from(BusinessSize size) {
        if (size == null) return AUTONOMO;
        return switch (size) {
            case PETIT -> PETIT;
            case MITJA -> MITJA;
            case EMPRESA -> EMPRESA;
            default -> AUTONOMO;
        };
    }
}
