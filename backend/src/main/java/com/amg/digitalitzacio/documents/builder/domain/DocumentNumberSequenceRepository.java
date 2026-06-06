package com.amg.digitalitzacio.documents.builder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentNumberSequenceRepository extends JpaRepository<DocumentNumberSequence, UUID> {
}
