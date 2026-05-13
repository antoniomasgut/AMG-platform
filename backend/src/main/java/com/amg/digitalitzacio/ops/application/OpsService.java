package com.amg.digitalitzacio.ops.application;

import com.amg.digitalitzacio.ops.api.dto.*;
import com.amg.digitalitzacio.ops.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OpsService {
    HealthResponse getHealth();
    AllHealthResponse getAllHealth();
    List<IncidentResponse> listIncidents(String status, String serviceName);
    IncidentResponse acknowledgeIncident(UUID incidentId);
    IncidentResponse resolveIncident(UUID incidentId);
    OpsDashboardResponse getDashboard();
    BackupResponse startBackup(String type, String description);
    List<BackupResponse> listBackups(String type);
    List<LogEntryResponse> getLogs(String serviceName, int limit);
}
