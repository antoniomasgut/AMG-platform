package com.amg.digitalitzacio.documents.delivery.domain;

public enum DocumentViewType {
    BUDGET,
    INVOICE,
    MEDICAL_REPORT,
    GENERIC;

    public boolean hasAcceptAction() {
        return this == BUDGET;
    }
}
