package com.amg.digitalitzacio.documents.delivery.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantDocumentPreferencesRepository extends JpaRepository<TenantDocumentPreferences, UUID> {}
