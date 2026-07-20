package com.amg.digitalitzacio.social.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTriageTest {

    // ─── Soroll: NO notificar ────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"👍", "❤️❤️❤️", "🔥🔥", "😍 😍 !!", "...", "!!!"})
    void emojisIPuntuacioSolsNoNotifiquen(String msg) {
        assertThat(CommentTriage.worthNotifying(msg)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Top", "ok", "jajaja", "Wow!", "genial 🔥", "Bravo!!"})
    void reaccionsCurtesNoNotifiquen(String msg) {
        assertThat(CommentTriage.worthNotifying(msg)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://spam.example.com",
        "Gana dinero con crypto trading, DM me",
        "Free followers aquí 👉 link in bio",
        "Escríbeme al whatsapp +34600000000 para invertir"})
    void spamNoNotifica(String msg) {
        assertThat(CommentTriage.worthNotifying(msg)).isFalse();
    }

    @Test
    void nullIBuitNoNotifiquen() {
        assertThat(CommentTriage.worthNotifying(null)).isFalse();
        assertThat(CommentTriage.worthNotifying("")).isFalse();
        assertThat(CommentTriage.worthNotifying("   ")).isFalse();
    }

    // ─── Contingut real: SÍ notificar ────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
        "Quin horari teniu els dissabtes?",
        "M'encanta! Encara teniu places per setembre?",
        "El servei va ser molt lent, no ho recomano",
        "Genial la reforma, quan inaugureu?",
        "Preu del menú del dia?",
        "Hola, teniu wifi?"})
    void preguntesIOpinionsNotifiquen(String msg) {
        assertThat(CommentTriage.worthNotifying(msg)).isTrue();
    }

    @Test
    void comentariNegatiuSempreNotifica() {
        assertThat(CommentTriage.worthNotifying("Fatal, una hora esperant i ningú contestava"))
            .isTrue();
    }

    @Test
    void reaccioAmbContingutAddicionalNotifica() {
        // "genial" sol és reacció, però amb més contingut ja és conversa
        assertThat(CommentTriage.worthNotifying("Genial, hi anirem aquest cap de setmana"))
            .isTrue();
    }
}
