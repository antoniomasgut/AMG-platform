package com.amg.digitalitzacio.infraops.api;

import com.amg.digitalitzacio.infraops.api.dto.ContainerStatus;
import com.amg.digitalitzacio.infraops.application.InfraOpsService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ingesta de l'estat de contenidors des de l'agent del host (docker ps → POST).
 * Protegit per token (capçalera X-Agent-Token vs SystemConfig INFRAOPS_AGENT_TOKEN),
 * no per JWT. Path a permitAll de SecurityConfig. El backend no toca mai Docker.
 */
@RestController
@RequestMapping("/api/v1/infraops/agent")
@RequiredArgsConstructor
@Slf4j
public class InfraOpsAgentController {

    private final InfraOpsService infraOpsService;
    private final SystemConfigService systemConfigService;

    @PostMapping("/container-status")
    public ResponseEntity<Void> reportContainerStatus(
            @RequestHeader(value = "X-Agent-Token", required = false) String token,
            @RequestBody List<ContainerStatus> containers) {
        String expected = systemConfigService.get("INFRAOPS_AGENT_TOKEN");
        if (expected == null || expected.isBlank() || !expected.equals(token)) {
            log.warn("InfraOps agent: token invàlid o absent");
            return ResponseEntity.status(401).build();
        }
        infraOpsService.reportContainerStatus(containers);
        return ResponseEntity.noContent().build();
    }
}
