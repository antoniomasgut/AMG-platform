package com.amg.digitalitzacio.domains.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositori JPA per a tarifes per extensió de domini
public interface TldPricingRepository extends JpaRepository<TldPricing, String> {

    // Només retorna els TLDs actius per a la consulta de disponibilitat
    List<TldPricing> findByIsActiveTrue();
}
