package com.amg.digitalitzacio.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MeetingSettingsRepository extends JpaRepository<MeetingSettings, UUID> {}
