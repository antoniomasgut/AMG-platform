package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/drive")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class DriveAdminController {

    private final GoogleCalendarService calendarService;

    record ShareRequest(String email, String role) {}

    @PostMapping("/share")
    public ResponseEntity<?> shareDriveFolder(@RequestBody ShareRequest req) {
        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requerit"));
        }
        String role = req.role() != null && Set.of("reader", "writer", "commenter").contains(req.role())
                ? req.role() : "reader";
        try {
            calendarService.shareDriveAMGFolder(req.email(), role);
            return ResponseEntity.ok(Map.of("message", "Carpeta Tenants/ compartida amb " + req.email()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> driveStatus() {
        boolean saConfigured = calendarService.isConfigured();
        return ResponseEntity.ok(Map.of(
                "saConfigured", saConfigured,
                "configured", saConfigured
        ));
    }
}
