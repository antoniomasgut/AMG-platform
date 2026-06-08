package com.amg.digitalitzacio.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingTokenRepository extends JpaRepository<BookingToken, UUID> {
    Optional<BookingToken> findByToken(String token);
    List<BookingToken> findByLeadId(UUID leadId);

    @Query("SELECT b FROM BookingToken b WHERE b.tenantId = :tenantId AND b.confirmed = true AND b.meetingAt BETWEEN :from AND :to")
    List<BookingToken> findConfirmedWithMeetingInWindow(@Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);
}
