package com.amg.digitalitzacio.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetLink) {
        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("AMG Digitalització — Recuperació de contrasenya");
        message.setText("""
                Has sol·licitat restablir la teva contrasenya.

                Fes clic en aquest enllaç per restablir-la:
                %s

                Si no has sol·licitat aquest canvi, ignora aquest missatge.

                L'enllaç és vàlid durant 30 minuts.
                """.formatted(resetLink));
        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
