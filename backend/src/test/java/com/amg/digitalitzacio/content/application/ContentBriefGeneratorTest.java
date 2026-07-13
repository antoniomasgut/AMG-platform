package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.content.domain.ContentPillar;
import com.amg.digitalitzacio.shared.ai.AIProvider;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentBriefGeneratorTest {

    @Mock AIProviderRouter aiRouter;
    @Mock AIProvider provider;

    private ContentBriefGenerator generator() {
        return new ContentBriefGenerator(aiRouter, new ObjectMapper());
    }

    @Test
    void parsesJsonArrayIntoBriefs() {
        when(aiRouter.forModel(anyString())).thenReturn(provider);
        when(provider.chat(anyString(), anyList(), anyString())).thenReturn("""
                ```json
                [
                  {"pillar":"NOVELTY","brief":"Foto d'un tall nou","example":"Ex: un degradat"},
                  {"pillar":"COMBINE","brief":"Un pentinat de festa","example":"Ex: recollit"},
                  {"pillar":"SHOP","brief":"El teu saló","example":"Ex: la cadira"},
                  {"pillar":"SOCIAL_PROOF","brief":"Una clienta contenta","example":"Ex: somrient"}
                ]
                ```
                """);

        Map<ContentPillar, ContentBriefGenerator.Brief> result = generator().generate("Saló X", "PERRUQUERIA", "ca");

        assertThat(result).hasSize(4);
        assertThat(result.get(ContentPillar.NOVELTY).brief()).isEqualTo("Foto d'un tall nou");
        assertThat(result.get(ContentPillar.SOCIAL_PROOF).example()).isEqualTo("Ex: somrient");
    }

    @Test
    void ignoresUnknownPillars() {
        when(aiRouter.forModel(anyString())).thenReturn(provider);
        when(provider.chat(anyString(), anyList(), anyString())).thenReturn(
                "[{\"pillar\":\"UNKNOWN\",\"brief\":\"x\",\"example\":\"y\"},"
                + "{\"pillar\":\"SHOP\",\"brief\":\"El local\",\"example\":\"\"}]");

        Map<ContentPillar, ContentBriefGenerator.Brief> result = generator().generate("X", "RESTAURANTE", "ca");

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(ContentPillar.SHOP);
    }

    @Test
    void aiFailure_returnsEmptyMap() {
        when(aiRouter.forModel(anyString())).thenReturn(provider);
        when(provider.chat(anyString(), anyList(), anyString())).thenThrow(new RuntimeException("boom"));

        Map<ContentPillar, ContentBriefGenerator.Brief> result = generator().generate("X", "PINTOR", "ca");

        assertThat(result).isEmpty();
    }

    @Test
    void malformedJson_returnsEmptyMap() {
        when(aiRouter.forModel(anyString())).thenReturn(provider);
        when(provider.chat(anyString(), anyList(), anyString())).thenReturn("no és json");

        assertThat(generator().generate("X", "PINTOR", "ca")).isEmpty();
    }
}
