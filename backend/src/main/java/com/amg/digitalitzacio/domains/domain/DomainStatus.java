package com.amg.digitalitzacio.domains.domain;

// Estats del cicle de vida d'un domini gestionat
public enum DomainStatus {
    PENDING_PURCHASE,
    REGISTERING,
    ACTIVE,
    DNS_PENDING,
    TRANSFER_IN,
    TRANSFER_OUT,
    EXPIRING_SOON,
    EXPIRED,
    CANCELLED
}
