package com.amg.digitalitzacio.shared.api;

import com.amg.digitalitzacio.auth.application.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Slf4j
public class PublicContactController {

    private final EmailService emailService;

    public record ContactRequest(String name, String email, String message) {}
    public record ContactResponse(boolean ok) {}

    @PostMapping
    public ResponseEntity<ContactResponse> submit(@RequestBody ContactRequest req) {
        if (req.name() == null || req.name().isBlank() ||
            req.email() == null || !req.email().contains("@") ||
            req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(new ContactResponse(false));
        }

        String text = """
                Nova consulta des de amgdl.com

                Nom: %s
                Email: %s

                Missatge:
                %s
                """.formatted(req.name().strip(), req.email().strip(), req.message().strip());

        try {
            emailService.sendEmail("hola@amgdl.com", "Consulta web: " + req.name().strip(), text);
        } catch (Exception e) {
            log.warn("Contact form email failed (non-fatal): {}", e.getMessage());
        }
        return ResponseEntity.ok(new ContactResponse(true));
    }
}
