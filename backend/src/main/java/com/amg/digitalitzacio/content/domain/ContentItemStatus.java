package com.amg.digitalitzacio.content.domain;

/** Estat d'una publicació planificada (Spec 58 §4). */
public enum ContentItemStatus {
    PLANNED,
    PHOTO_REQUESTED,
    PHOTO_RECEIVED,
    AWAITING_APPROVAL,
    PUBLISHED,
    FAILED,
    SKIPPED
}
