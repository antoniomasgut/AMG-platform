package com.amg.digitalitzacio.auth.domain;

public enum ServicePhase {
    F1,  // Comunicació base (WhatsApp/Telegram/Email 24/7)
    F2,  // Gestió de cites (agenda, recordatoris, cancel·lacions)
    F3,  // Pressupostos (generació PDF, seguiment)
    F4,  // Fidelització (seguiment postvenda, reactivació, resenyes)
    F5   // Equip (coordinació empleats, partes diaris)
}
