package com.amg.digitalitzacio.auth.application;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Validador de prerequisits entre fases: detecta combinacions sense sentit o incompletes
@Component
public class PhasePrerequisiteValidator {

    public List<String> validate(Set<String> contractedPhases) {
        var warnings = new ArrayList<String>();

        // F2, F3, F4, F5 necessiten F1 com a base d'agent
        for (String dep : List.of("F2", "F3", "F4", "F5")) {
            if (contractedPhases.contains(dep) && !contractedPhases.contains("F1")) {
                warnings.add(dep + " activa sense F1 (l'agent IA és la base de tots els canals)");
            }
        }

        // F4 té més sentit si hi ha F1 o F3 (seguiment de clients existents)
        if (contractedPhases.contains("F4") && !contractedPhases.contains("F3") && !contractedPhases.contains("F1")) {
            warnings.add("F4 (Fidelització) sense F1 ni F3 — no tindrà leads ni clients per fer seguiment");
        }

        return warnings;
    }
}
