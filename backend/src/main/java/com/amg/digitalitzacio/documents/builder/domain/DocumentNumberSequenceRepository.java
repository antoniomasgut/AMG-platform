package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentNumberSequenceRepository
        extends JpaRepository<DocumentNumberSequence, DocumentNumberSequence.SeqKey> {

    @Modifying
    @Query(value = """
        INSERT INTO document_number_sequences (tenant_id, doc_type, year, next_number, updated_at)
        VALUES (:tenantId, :docType, :year, 1, now())
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void initIfAbsent(@Param("tenantId") UUID tenantId,
                      @Param("docType") String docType,
                      @Param("year") int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DocumentNumberSequence s WHERE s.tenantId = :tenantId AND s.docType = :docType AND s.year = :year")
    Optional<DocumentNumberSequence> findForUpdate(@Param("tenantId") UUID tenantId,
                                                   @Param("docType") String docType,
                                                   @Param("year") int year);
}
