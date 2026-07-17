package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.domain.TenantPhaseActivation;
import com.amg.digitalitzacio.auth.domain.TenantPhaseActivationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhaseActivationService {

    private final TenantPhaseActivationRepository repo;

    /**
     * Registra l'activació d'una fase. Idempotent: no fa res si ja hi ha un registre actiu.
     */
    @Transactional
    public void recordActivation(UUID tenantId, String phase, String source, UUID budgetId, String activatedBy) {
        if (repo.existsByTenantIdAndPhaseAndDeactivatedAtIsNull(tenantId, phase)) {
            return; // ja activa, idempotent
        }
        var activation = new TenantPhaseActivation();
        activation.setTenantId(tenantId);
        activation.setPhase(phase);
        activation.setSource(source);
        activation.setBudgetId(budgetId);
        activation.setActivatedBy(activatedBy);
        activation.setActivatedAt(Instant.now());
        repo.save(activation);
        log.info("[PhaseActivation] Fase {} activada per tenant {} via {}", phase, tenantId, source);
    }

    /**
     * Registra la desactivació de la darrera activació activa d'una fase.
     */
    @Transactional
    public void recordDeactivation(UUID tenantId, String phase, String deactivatedBy) {
        repo.findFirstByTenantIdAndPhaseAndDeactivatedAtIsNullOrderByActivatedAtDesc(tenantId, phase)
            .ifPresent(a -> {
                a.setDeactivatedAt(Instant.now());
                a.setDeactivatedBy(deactivatedBy);
                repo.save(a);
            });
    }

    /**
     * Retorna l'historial complet d'activacions d'un tenant, ordenat per data descendent.
     */
    @Transactional(readOnly = true)
    public List<TenantPhaseActivation> getHistory(UUID tenantId) {
        return repo.findByTenantIdOrderByActivatedAtDesc(tenantId);
    }
}
