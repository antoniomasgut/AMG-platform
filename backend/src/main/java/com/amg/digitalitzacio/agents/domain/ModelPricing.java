package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "model_pricing")
@Getter @Setter @NoArgsConstructor
public class ModelPricing {

    @Id
    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "input_cost_micros", nullable = false)
    private long inputCostMicros;

    @Column(name = "output_cost_micros", nullable = false)
    private long outputCostMicros;

    @Column(name = "markup_percent", nullable = false)
    private int markupPercent = 20;

    @Column(name = "client_input_micros", nullable = false)
    private long clientInputMicros;

    @Column(name = "client_output_micros", nullable = false)
    private long clientOutputMicros;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public static ModelPricing of(String modelName, String provider,
                                   long inputCostMicros, long outputCostMicros, int markupPercent) {
        var p = new ModelPricing();
        p.modelName = modelName;
        p.provider = provider;
        p.inputCostMicros = inputCostMicros;
        p.outputCostMicros = outputCostMicros;
        p.markupPercent = markupPercent;
        p.clientInputMicros  = inputCostMicros  * (100 + markupPercent) / 100;
        p.clientOutputMicros = outputCostMicros * (100 + markupPercent) / 100;
        p.updatedAt = Instant.now();
        return p;
    }

    public void applyMarkup(int markupPercent) {
        this.markupPercent   = markupPercent;
        this.clientInputMicros  = inputCostMicros  * (100 + markupPercent) / 100;
        this.clientOutputMicros = outputCostMicros * (100 + markupPercent) / 100;
        this.updatedAt = Instant.now();
    }
}
