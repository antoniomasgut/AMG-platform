package com.amg.digitalitzacio.shared.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantNotificationConfigRepository extends JpaRepository<TenantNotificationConfig, UUID> {
}
