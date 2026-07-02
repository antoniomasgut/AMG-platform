package com.amg.digitalitzacio.engine.domain;

public enum BlockType {
    HERO,
    TEXT,
    SERVICES,
    GALLERY,
    CONTACT_FORM,
    FAQ,
    TESTIMONIALS,
    CTA,
    FOOTER,
    MAP,
    OPENING_HOURS,
    PRICING,
    TEAM,
    VIDEO,
    REVIEWS,
    TRUST_BAR,
    CHAT_CTA,
    STEPS;

    /** Nom del tipus de bloc per al JSON de la landing (guions, no guions baixos). */
    public String typeName() {
        return name().toLowerCase().replace('_', '-');
    }
}
