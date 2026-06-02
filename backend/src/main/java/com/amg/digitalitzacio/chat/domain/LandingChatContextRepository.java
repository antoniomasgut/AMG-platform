package com.amg.digitalitzacio.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LandingChatContextRepository extends JpaRepository<LandingChatContext, UUID> {
}
