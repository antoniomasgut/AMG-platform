package com.amg.digitalitzacio.prospecting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Llista de supressió global: emails que han demanat no rebre més comunicacions
 * comercials (LSSI art. 21 / RGPD art. 21). Es comprova abans de cada enviament
 * de pitch, independentment de la campanya.
 */
@Entity
@Table(name = "prospect_optouts")
@Getter @Setter
public class ProspectOptOut {
    @Id
    @Column(length = 255)
    private String email;

    private UUID prospectId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
