package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.domain.PasswordResetToken;
import com.amg.digitalitzacio.auth.domain.PasswordResetTokenRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Crea automàticament l'usuari CLIENT d'un tenant al go-live i li envia
 * l'email d'invitació amb un enllaç per establir la contrasenya (reutilitza
 * el flux de reset-password amb un TTL ampliat de 7 dies).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientUserProvisioningService {

    private static final int INVITATION_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Crea l'usuari CLIENT del tenant si no existeix i envia la invitació.
     * @return true si s'ha creat i enviat la invitació; false si ja existia o no és possible
     */
    @Transactional
    public boolean provisionForTenant(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return false;

        if (userRepository.existsByTenantIdAndRole(tenantId, Role.CLIENT)) {
            log.debug("[Provisioning] Tenant {} ja té usuari CLIENT", tenantId);
            return false;
        }

        String email = tenant.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("[Provisioning] Tenant {} sense email — no es pot crear l'usuari CLIENT", tenantId);
            return false;
        }
        if (userRepository.existsByEmailAndRole(email, Role.CLIENT)) {
            log.debug("[Provisioning] L'email {} ja té un usuari CLIENT — es reusa", email);
            return false;
        }
        if (userRepository.existsByEmail(email)) {
            log.warn("[Provisioning] L'email {} pertany a un usuari ADMIN/SUPER_ADMIN — no es crea CLIENT duplicat per tenant {}. Assigna el tenant manualment.",
                    email, tenantId);
            return false;
        }

        // Contrasenya aleatòria que mai es comunica: el client n'estableix una via l'enllaç
        var user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(randomSecret()))
                .name(tenant.getName() != null ? tenant.getName() : email)
                .role(Role.CLIENT)
                .tenantId(tenantId)
                .isActive(true)
                .isBlocked(false)
                .failedAttempts(0)
                .build());

        String token = randomSecret();
        resetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(RefreshTokenService.hashToken(token))
                .expiresAt(Instant.now().plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS))
                .used(false)
                .build());

        String setupLink = "https://amgdl.com/ca/reset-password?token=" + token;
        sendInvitationEmail(email, tenant.getName(), setupLink);

        log.info("[Provisioning] Usuari CLIENT creat per tenant {} i invitació enviada a {}", tenantId, email);
        return true;
    }

    private void sendInvitationEmail(String to, String name, String setupLink) {
        String subject = "El teu accés al panel d'AMG Digitalització";
        String body = """
                Hola %s,

                El teu servei ja està actiu! Hem creat el teu accés al panel de gestió,
                on podràs veure les converses del teu agent, els contactes i molt més.

                Per començar, estableix la teva contrasenya aquí (l'enllaç caduca en %d dies):

                %s

                El teu usuari és aquest mateix correu: %s

                Si tens qualsevol dubte, respon a aquest correu o escriu-nos per WhatsApp.

                L'equip d'AMG Digitalització
                """.formatted(name != null ? name : "client", INVITATION_TTL_DAYS, setupLink, to);
        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            log.warn("[Provisioning] No s'ha pogut enviar la invitació a {}: {}", to, e.getMessage());
        }
    }

    private String randomSecret() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
