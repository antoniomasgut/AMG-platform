package com.amg.digitalitzacio.domains.domain;

public enum DomainScenario {
    OPENPROVIDER_NEW,       // Escenari B: registre nou via OpenProvider
    OPENPROVIDER_TRANSFER,  // Escenari E: transferència d'un domini existent cap a OpenProvider
    EXTERNAL_OWN,           // Escenari A: domini propi del client, DNS gestionat exteriorment
    EXTERNAL_SUBDOMAIN,     // Escenari D: subdomini del domini del client (carta.negoci.com)
    AMG_SUBDOMAIN           // Escenari C: subdomini de amgdl.com gestionat per AMG
}
