package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.AgentMode;

/**
 * Resultat de la fase 1 (transaccional) de handleIncoming/processWidgetMessage.
 * Conté tot el necessari per a la fase d'inferència IA i la fase de persistència.
 */
record IncomingPreparation(
    AgentMode agentMode,
    Long telegramChatId,
    String whatsappPhoneNumber,
    String whatsappMetaPhoneNumberId,
    String senderEmail,
    String senderName,
    String replyToEmail,
    CustomerContext context,
    String preferredModel,
    String fallbackModel,
    boolean isNewContact,
    String notificationChannel,  // TELEGRAM / WHATSAPP / EMAIL — com notificar l'operador
    String operatorPhone         // telèfon de contacte de l'operador per a notificacions WA
) {}
