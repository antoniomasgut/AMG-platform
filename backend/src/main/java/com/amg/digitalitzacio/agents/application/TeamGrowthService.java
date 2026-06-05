package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.TenantTeamMember;
import com.amg.digitalitzacio.agents.domain.TenantTeamMemberRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.application.NexePricingFormula;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamGrowthService {

    private static final int UPSELL_TRIGGER_COUNT = 2; // interaccions del nou membre abans d'enviar upsell

    private final TenantTeamMemberRepository memberRepository;
    private final TenantRepository tenantRepository;
    private final NexePricingFormula pricingFormula;
    private final TelegramBotClient telegramBotClient;

    /**
     * Registra una interacció d'un membre de l'equip i comprova si cal enviar l'upsell de F5.
     * S'ha de cridar quan un missatge arriba al webhook intern (no al webhook de clients).
     */
    @Transactional
    public void recordAndCheck(UUID tenantId, Long telegramUserId, String firstName, Long chatId) {
        var member = memberRepository
                .findByTenantIdAndTelegramUserId(tenantId, telegramUserId)
                .orElseGet(() -> {
                    log.info("Nou membre detectat al grup Telegram — tenant={} userId={} name={}",
                            tenantId, telegramUserId, firstName);
                    return memberRepository.save(TenantTeamMember.builder()
                            .tenantId(tenantId)
                            .telegramUserId(telegramUserId)
                            .firstName(firstName)
                            .interactionCount(0)
                            .firstSeenAt(Instant.now())
                            .build());
                });

        member.setInteractionCount(member.getInteractionCount() + 1);
        memberRepository.save(member);

        // Comprova si cal enviar upsell: 2a+ persona, 2 interaccions, F5 no contractada, upsell no enviat
        boolean isNewMember = member.getInteractionCount() == UPSELL_TRIGGER_COUNT;
        boolean isMultiUser = memberRepository.countByTenantId(tenantId) > 1;
        boolean upsellPending = member.getUpsellSentAt() == null;

        if (isNewMember && isMultiUser && upsellPending) {
            tenantRepository.findById(tenantId).ifPresent(tenant -> {
                if (!hasF5(tenant)) {
                    sendUpsell(tenant, chatId, member.getFirstName());
                    member.setUpsellSentAt(Instant.now());
                    memberRepository.save(member);
                }
            });
        }
    }

    private boolean hasF5(Tenant tenant) {
        return tenant.isPhaseActive("F5");
    }

    private void sendUpsell(Tenant tenant, Long chatId, String newMemberName) {
        var sector = tenant.getSector();
        var size = tenant.getBusinessSize();
        var monthly = pricingFormula.monthlyPerPhase(5, sector, size);
        var setup = pricingFormula.setupPerPhase(5, sector, size);

        var name = newMemberName != null ? newMemberName : "un nou membre";
        var msg = String.format(
            "👥 Hem detectat que %s també gestiona el negoci des d'aquí.\n\n" +
            "Per coordinar millor qui s'ocupa de cada sol·licitud, teniu disponible " +
            "la fase d'Equip (F5) per %s€/mes (setup %s€).\n\n" +
            "Inclou: assignació de tasques, alertes d'equip i gestió de documentació interna.\n\n" +
            "Voleu activar-la? Contacteu-nos i us la configurem avui. 🚀",
            name, monthly.toPlainString(), setup.toPlainString()
        );

        var tenantId = tenant.getId();
        boolean sent = telegramBotClient.sendMessageForTenant(tenantId, chatId, msg);
        if (sent) {
            log.info("Upsell F5 enviat al tenant={} chat={}", tenantId, chatId);
        } else {
            log.warn("No s'ha pogut enviar l'upsell F5 al tenant={}", tenantId);
        }
    }
}
