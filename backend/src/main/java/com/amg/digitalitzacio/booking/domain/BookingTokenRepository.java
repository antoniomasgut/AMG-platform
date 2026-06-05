package com.amg.digitalitzacio.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingTokenRepository extends JpaRepository<BookingToken, UUID> {
    Optional<BookingToken> findByToken(String token);
    List<BookingToken> findByLeadId(UUID leadId);
}
