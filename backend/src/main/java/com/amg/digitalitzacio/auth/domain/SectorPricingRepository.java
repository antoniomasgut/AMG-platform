package com.amg.digitalitzacio.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectorPricingRepository extends JpaRepository<SectorPricing, UUID> {

    Optional<SectorPricing> findBySectorAndBusinessSize(BusinessSector sector, BusinessSize size);

    List<SectorPricing> findAllByOrderBySectorAscBusinessSizeAsc();
}
