package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Genera i persiteix el missatge de prospecció personalitzat (aiPitch) per a un prospect concret.
 * Delega la generació del text a ProspectAnalysisService.generatePitch().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectMessageGeneratorService {

    private final ProspectAnalysisService prospectAnalysisService;
    private final ProspectRepository prospectRepository;

    @Transactional
    public String generatePitch(UUID prospectId) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        prospectAnalysisService.generatePitch(prospect);
        prospectRepository.save(prospect);
        log.info("Pitch generat per prospect {} ({})", prospectId, prospect.getName());
        return prospect.getAiPitch() != null ? prospect.getAiPitch() : "";
    }

    /**
     * Genera el pitch internament sense persistir (per a ús des de l'orchestrator).
     */
    public void generateAndSave(Prospect prospect) {
        prospectAnalysisService.generatePitch(prospect);
    }
}
