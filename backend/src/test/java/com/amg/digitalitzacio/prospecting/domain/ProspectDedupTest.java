package com.amg.digitalitzacio.prospecting.domain;

import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que findExistingPlaceIds/Phones estiguin escopats per campaignId
 * i no generin falsos positius entre campanyes.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class ProspectDedupTest {

    @Autowired private TestEntityManager em;
    @Autowired private ProspectRepository prospectRepository;

    @Test
    void placeId_foundOnlyWithinSameCampaign() {
        UUID campaignA = UUID.randomUUID();
        UUID campaignB = UUID.randomUUID();

        em.persistAndFlush(Prospect.builder()
                .campaignId(campaignA)
                .name("Restaurant Sa Premsa")
                .googlePlaceId("ChIJabc123")
                .status(ProspectStatus.NEW)
                .source(ProspectSource.GOOGLE_MAPS)
                .build());

        // Consulta en la mateixa campanya → troba el placeId
        Set<String> foundInA = prospectRepository.findExistingPlaceIds(
                campaignA, Set.of("ChIJabc123"));
        assertThat(foundInA).containsExactly("ChIJabc123");

        // Consulta en una altra campanya → no ha de trobar el placeId
        Set<String> foundInB = prospectRepository.findExistingPlaceIds(
                campaignB, Set.of("ChIJabc123"));
        assertThat(foundInB).isEmpty();
    }

    @Test
    void phone_foundOnlyWithinSameCampaign() {
        UUID campaignA = UUID.randomUUID();
        UUID campaignB = UUID.randomUUID();

        em.persistAndFlush(Prospect.builder()
                .campaignId(campaignA)
                .name("Forn de pa Can Mateu")
                .phone("+34971123456")
                .googlePlaceId("ChIJphone_test")
                .status(ProspectStatus.NEW)
                .source(ProspectSource.GOOGLE_MAPS)
                .build());

        Set<String> foundInA = prospectRepository.findExistingPhones(
                campaignA, Set.of("+34971123456"));
        assertThat(foundInA).containsExactly("+34971123456");

        Set<String> foundInB = prospectRepository.findExistingPhones(
                campaignB, Set.of("+34971123456"));
        assertThat(foundInB).isEmpty();
    }

    @Test
    void unknownId_returnsEmpty() {
        UUID campaign = UUID.randomUUID();
        assertThat(prospectRepository.findExistingPlaceIds(campaign, Set.of("ChIJunknown"))).isEmpty();
        assertThat(prospectRepository.findExistingPhones(campaign, Set.of("+34000000000"))).isEmpty();
    }
}
