package com.amg.digitalitzacio.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectorPhaseRepository extends JpaRepository<SectorPhase, UUID> {
    List<SectorPhase> findBySectorOrderByPhaseNumber(BusinessSector sector);
    Optional<SectorPhase> findBySectorAndPhaseNumber(BusinessSector sector, int phaseNumber);
    long countBySector(BusinessSector sector);
}
