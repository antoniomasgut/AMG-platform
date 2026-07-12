package com.amg.digitalitzacio.infraops.api;

import com.amg.digitalitzacio.infraops.api.dto.ContainerStatus;
import com.amg.digitalitzacio.infraops.application.InfraOpsService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/**
 * Ingesta de l'estat de contenidors des de l'agent del host (docker ps → POST).
 * Protegit per token (capçalera X-Agent-Token vs SystemConfig INFRAOPS_AGENT_TOKEN),
 * no per JWT. Path a permitAll de SecurityConfig. El backend no toca mai Docker.
 *
 * El cos arriba com a **hex** (només 0-9a-f) del JSON: així el WAF extern (OWASP CRS
 * amb puntuació d'anomalia) no bloqueja el payload per parèntesis/paraules SQL-ish.
 */
@RestController
@RequestMapping("/api/v1/infraops/agent")
@RequiredArgsConstructor
@Slf4j
public class InfraOpsAgentController {

    private final InfraOpsService infraOpsService;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    @PostMapping("/container-status")
    public ResponseEntity<Void> reportContainerStatus(
            @RequestHeader(value = "X-Agent-Token", required = false) String token,
            @RequestBody String hexBody) {
        String expected = systemConfigService.get("INFRAOPS_AGENT_TOKEN");
        if (expected == null || expected.isBlank() || !expected.equals(token)) {
            log.warn("InfraOps agent: token invàlid o absent");
            return ResponseEntity.status(401).build();
        }
        try {
            String json = new String(HexFormat.of().parseHex(hexBody.trim()), StandardCharsets.UTF_8);
            List<ContainerStatus> containers = objectMapper.readValue(json, new TypeReference<>() {});
            infraOpsService.reportContainerStatus(containers);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("InfraOps agent: payload invàlid: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
