package com.amg.digitalitzacio.content.domain;

/**
 * Pilars de contingut recurrents (Spec 58 §2). Es roten cada mes.
 * Cada pilar porta un brief per defecte (què ha de fotografiar el tenant).
 */
public enum ContentPillar {
    NOVELTY(
            "Una peça o producte nou, amb llum natural (penjat o posat). Fes-la horitzontal i vertical.",
            "Ex: un article que acabi d'arribar."),
    COMBINE(
            "Un conjunt o ús del producte (2-3 elements junts), en maniquí o de prop.",
            "Ex: un look complet o el producte en ús."),
    SHOP(
            "Tu, l'aparador o l'ambient del negoci. Posa cara al negoci!",
            "Ex: el propietari atenent o l'aparador."),
    SOCIAL_PROOF(
            "Un client content (amb permís) o una selecció de temporada.",
            "Ex: una ressenya destacada o novetats de temporada.");

    private final String defaultBrief;
    private final String defaultExample;

    ContentPillar(String defaultBrief, String defaultExample) {
        this.defaultBrief = defaultBrief;
        this.defaultExample = defaultExample;
    }

    public String getDefaultBrief() {
        return defaultBrief;
    }

    public String getDefaultExample() {
        return defaultExample;
    }

    /** Ordre de rotació dels pilars per a l'auto-generació. */
    public static final ContentPillar[] ROTATION = { NOVELTY, COMBINE, SHOP, SOCIAL_PROOF };
}
