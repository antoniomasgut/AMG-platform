package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectorTemplateRepository extends JpaRepository<SectorTemplate, UUID> {
    List<SectorTemplate> findBySectorOrderBySortOrder(String sector);
    List<SectorTemplate> findBySectorAndTypeOrderBySortOrder(String sector, String type);
    List<SectorTemplate> findByTypeOrderBySortOrder(String type);
}
