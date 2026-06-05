package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MetaAdsConfigRepository extends JpaRepository<MetaAdsConfig, UUID> {
    List<MetaAdsConfig> findByEnabledTrue();
}
