package com.amg.digitalitzacio.auth.domain;

public enum ServicePhase {
    F1,  // Captació (WhatsApp/Telegram/Email 24/7, leads automàtics)
    F2,  // Agenda (cites, recordatoris, cancel·lacions)
    F3,  // Pressupostos (generació PDF, seguiment)
    F4,  // Seguiment (postvenda, reactivació, resenyes Google)
    F5   // Alertes & Equip (grup Telegram intern, informes diaris)
}
