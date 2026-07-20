package com.amg.digitalitzacio.social.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtmTaggerTest {

    @Test
    void afegeixUtmAUrlSimple() {
        String out = UtmTagger.tag("Visita'ns a https://canarebecca.webs.amgdl.com avui!", "INSTAGRAM");
        assertThat(out).isEqualTo(
            "Visita'ns a https://canarebecca.webs.amgdl.com?utm_source=instagram&utm_medium=social&utm_campaign=amg_social avui!");
    }

    @Test
    void urlAmbQueryExistentUsaAmpersand() {
        String out = UtmTagger.tag("Mira https://example.com/page?lang=ca", "FACEBOOK");
        assertThat(out).contains("?lang=ca&utm_source=facebook&utm_medium=social");
    }

    @Test
    void noDuplicaUtmSiJaExisteix() {
        String in = "https://example.com?utm_source=newsletter";
        assertThat(UtmTagger.tag(in, "FACEBOOK")).isEqualTo(in);
    }

    @Test
    void respectaPuntuacioFinal() {
        String out = UtmTagger.tag("Reserva a https://example.com/cita.", "GOOGLE_BUSINESS");
        assertThat(out).endsWith("utm_source=google_business&utm_medium=social&utm_campaign=amg_social.");
    }

    @Test
    void urlDinsParentesis() {
        String out = UtmTagger.tag("(més info: https://example.com)", "LINKEDIN");
        assertThat(out).isEqualTo("(més info: https://example.com?utm_source=linkedin&utm_medium=social&utm_campaign=amg_social)");
    }

    @Test
    void captionSenseUrlQuedaIgual() {
        String in = "Avui obrim a les 9! 🎉";
        assertThat(UtmTagger.tag(in, "INSTAGRAM")).isEqualTo(in);
    }

    @Test
    void captionNullOBuitNoFalla() {
        assertThat(UtmTagger.tag(null, "INSTAGRAM")).isNull();
        assertThat(UtmTagger.tag("", "INSTAGRAM")).isEmpty();
    }

    @Test
    void xarxaDesconegudaNoToca() {
        String in = "https://example.com";
        assertThat(UtmTagger.tag(in, "TIKTOK")).isEqualTo(in);
    }

    @Test
    void multiplesUrls() {
        String out = UtmTagger.tag("A https://a.com i B https://b.com", "FACEBOOK");
        assertThat(out)
            .contains("https://a.com?utm_source=facebook")
            .contains("https://b.com?utm_source=facebook");
    }
}
