package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(5)
@RequiredArgsConstructor
public class SectorPhaseSeeder implements ApplicationRunner {

    private final SectorPhaseRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;

        // ── Pintura / Oficis de Reforma ──────────────────────────────────
        // Mateixa estructura per: PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA, TALLER_MECANIC
        for (var s : List.of(BusinessSector.PINTOR, BusinessSector.ELECTRICISTA,
                BusinessSector.FONTANER, BusinessSector.JARDINER,
                BusinessSector.NETEJA, BusinessSector.TALLER_MECANIC)) {
            repo.saveAll(List.of(
                phase(s, 1, "Generador de Pressupostos",
                    "Envia mesures per Telegram → el bot genera el pressupost i l'envia al client per email",
                    PhaseDepType.BASE, null, 99, 59),
                phase(s, 2, "Entrega Automàtica al Client",
                    "El pressupost generat s'envia automàticament per WhatsApp i email sense intervenció",
                    PhaseDepType.OPTIONAL, "1", 49, 20),
                phase(s, 3, "Seguiment i Acceptació",
                    "Si el client no respon en X dies, el bot envia recordatori. Notifica l'operari quan accepta/rebutja",
                    PhaseDepType.OPTIONAL, "1", 49, 18),
                phase(s, 4, "Agenda de Visita de Mesura",
                    "El client sol·licita visita per WhatsApp → el bot consulta agenda i confirma",
                    PhaseDepType.REQUIRED, "1", 79, 18),
                phase(s, 5, "Recordatoris Automàtics",
                    "24h abans de la visita, el bot envia recordatori amb opció de confirmar o cancel·lar",
                    PhaseDepType.OPTIONAL, "4", 29, 12),
                phase(s, 6, "Interpretació de Fotos",
                    "L'operari envia fotos → el bot analitza amb visió artificial i enriqueix el pressupost",
                    PhaseDepType.OPTIONAL, "1", 79, 15),
                phase(s, 7, "Progrés i Postvenda",
                    "L'operari reporta avances per Telegram → el bot notifica el client. Sol·licita valoració al final",
                    PhaseDepType.OPTIONAL, "4", 49, 10)
            ));
        }

        // ── Fisioterapeuta / Centre de Salut ─────────────────────────────
        // Mateixa estructura per: FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA
        for (var s : List.of(BusinessSector.FISIOTERAPEUTA,
                BusinessSector.PSICOLEG, BusinessSector.NUTRICIONISTA)) {
            repo.saveAll(List.of(
                phase(s, 1, "Agenda de Cites",
                    "El pacient reserva per WhatsApp → el bot mostra disponibilitat i confirma",
                    PhaseDepType.BASE, null, 99, 59),
                phase(s, 2, "Historial del Pacient",
                    "El professional consulta l'historial per Telegram. El pacient pot sol·licitar-lo per email",
                    PhaseDepType.OPTIONAL, "1", 69, 25),
                phase(s, 3, "Registre de Sessió",
                    "El professional dicta nota de veu per Telegram → el bot estructura i guarda l'historial",
                    PhaseDepType.REQUIRED, "1,2", 79, 22),
                phase(s, 4, "Seguiment entre Sessions",
                    "El professional pauta exercicis → el bot els envia per email/WhatsApp i recull evolució",
                    PhaseDepType.OPTIONAL, "1", 79, 18),
                phase(s, 5, "Gestió de Bons i Pagaments",
                    "Control de sessions per bo, avís quan queden 2 sessions, renovació per WhatsApp, factures per email",
                    PhaseDepType.OPTIONAL, "1", 69, 15),
                phase(s, 6, "Reactivació i Fidelització",
                    "Detecta pacients inactius i llança oferta personalitzada. Campanyes estacionals",
                    PhaseDepType.OPTIONAL, "1", 49, 12)
            ));
        }

        // ── Restaurante / Bar / Cafeteria ────────────────────────────────
        repo.saveAll(List.of(
            phase(BusinessSector.RESTAURANTE, 1, "Reserves de Taula",
                "El client reserva per WhatsApp → el bot mostra disponibilitat i confirma. Notificació per Telegram",
                PhaseDepType.BASE, null, 99, 69),
            phase(BusinessSector.RESTAURANTE, 2, "Recordatoris de Reserva",
                "24-48h abans el bot envia recordatori amb opció de confirmar o cancel·lar. Allibera taula automàticament",
                PhaseDepType.OPTIONAL, "1", 29, 20),
            phase(BusinessSector.RESTAURANTE, 3, "Consulta de Menú i Carta",
                "El client consulta el menú del dia, plats, preus i al·lèrgens per WhatsApp a qualsevol hora",
                PhaseDepType.OPTIONAL, null, 39, 15),
            phase(BusinessSector.RESTAURANTE, 4, "Comandes per Emportar",
                "El client fa la comanda per WhatsApp → el bot confirma i notifica cuina per Telegram. Factura per email",
                PhaseDepType.OPTIONAL, null, 89, 22),
            phase(BusinessSector.RESTAURANTE, 5, "Fidelització i Comunicació",
                "Menú del dia automàtic a clients subscrits. Campanyes d'ofertes i events. Felicitacions en dates especials",
                PhaseDepType.OPTIONAL, "1", 49, 12)
        ));

        // ── Academia / Centre de Formació ────────────────────────────────
        repo.saveAll(List.of(
            phase(BusinessSector.ACADEMIA, 1, "Informació i Captació",
                "L'interessat consulta cursos, preus i horaris per WhatsApp → el bot respon i guia cap a la matrícula",
                PhaseDepType.BASE, null, 79, 55),
            phase(BusinessSector.ACADEMIA, 2, "Matrícula i Alta",
                "L'alumne es matricula per WhatsApp → el bot recull les dades i envia benvinguda per email",
                PhaseDepType.REQUIRED, "1", 89, 25),
            phase(BusinessSector.ACADEMIA, 3, "Gestió d'Assistència",
                "El professor registra assistència per Telegram → el bot notifica absències a famílies per WhatsApp",
                PhaseDepType.OPTIONAL, "2", 79, 20),
            phase(BusinessSector.ACADEMIA, 4, "Seguiment del Progrés",
                "El professor registra notes per Telegram → el bot genera un informe i l'envia a la família per email",
                PhaseDepType.OPTIONAL, "2", 69, 18),
            phase(BusinessSector.ACADEMIA, 5, "Gestió de Pagaments i Recordatoris",
                "Recordatori de pagament mensual per WhatsApp. Escala impagaments. Factures per email",
                PhaseDepType.OPTIONAL, "2", 79, 18),
            phase(BusinessSector.ACADEMIA, 6, "Renovació i Fidelització",
                "A prop del final de curs el bot contacta l'alumne i ofereix el nivell següent amb descompte per renovació anticipada",
                PhaseDepType.OPTIONAL, "2", 49, 12)
        ));

        // ── Clínica Veterinària ──────────────────────────────────────────
        // Mateixa estructura per: VETERINARI, PERRUQUERIA_CANINA
        for (var s : List.of(BusinessSector.VETERINARI, BusinessSector.PERRUQUERIA_CANINA)) {
            repo.saveAll(List.of(
                phase(s, 1, "Agenda de Cites",
                    "El propietari agenda per WhatsApp indicant el tipus de servei → el bot confirma i notifica per Telegram",
                    PhaseDepType.BASE, null, 99, 59),
                phase(s, 2, "Historial de la Mascota",
                    "El veterinari/estilista consulta l'historial complet per Telegram. El propietari pot sol·licitar-lo per email",
                    PhaseDepType.OPTIONAL, "1", 69, 24),
                phase(s, 3, "Registre de Consulta",
                    "El professional registra diagnòstic i tractament per Telegram → el bot guarda l'historial i envia instruccions per email",
                    PhaseDepType.REQUIRED, "1,2", 69, 20),
                phase(s, 4, "Recordatoris de Vacunes i Revisions",
                    "El bot monitoritza el calendari sanitari i envia recordatori per WhatsApp quan s'apropa una vacuna",
                    PhaseDepType.OPTIONAL, "2", 59, 18),
                phase(s, 5, "Seguiment de Tractaments",
                    "Quan es pauta un tractament, el bot envia recordatoris de medicació i recull reportes d'evolució",
                    PhaseDepType.OPTIONAL, "3", 69, 15),
                phase(s, 6, "Fidelització i Postvenda",
                    "Felicita l'aniversari de la mascota. Recordatoris de revisions periòdiques. Campanyes estacionals",
                    PhaseDepType.OPTIONAL, "1", 39, 10)
            ));
        }

        // ── Peluqueria / Centre d'Estètica ───────────────────────────────
        for (var s : List.of(BusinessSector.PERRUQUERIA, BusinessSector.ESTETICA)) {
            repo.saveAll(List.of(
                phase(s, 1, "Reserva de Cita",
                    "El client reserva per WhatsApp amb el seu estilista favorit o el primer disponible → confirmació i notificació Telegram",
                    PhaseDepType.BASE, null, 79, 49),
                phase(s, 2, "Recordatoris de Cita",
                    "24-48h abans el bot envia recordatori amb opció de confirmar o cancel·lar. Allibera el buit automàticament",
                    PhaseDepType.OPTIONAL, "1", 29, 18),
                phase(s, 3, "Historial del Client",
                    "L'estilista consulta historial per Telegram: serveis, color, preferències, productes, notes",
                    PhaseDepType.OPTIONAL, "1", 59, 16),
                phase(s, 4, "Fidelització i Reactivació",
                    "Detecta clients inactius i llança oferta personalitzada. Campanyes estacionals. Programa de punts",
                    PhaseDepType.OPTIONAL, "1", 49, 14),
                phase(s, 5, "Gestió de Productes",
                    "Després de cada sessió el bot recomana productes de manteniment. El client pot demanar-los per WhatsApp",
                    PhaseDepType.OPTIONAL, "3", 49, 12)
            ));
        }

        // ── Gestoria ─────────────────────────────────────────────────────
        repo.saveAll(List.of(
            phase(BusinessSector.GESTORIA, 1, "Captació i Consulta Inicial",
                "El client consulta serveis, preus i documentació per WhatsApp → el bot respon i agenda reunió",
                PhaseDepType.BASE, null, 79, 55),
            phase(BusinessSector.GESTORIA, 2, "Alta i Recollida de Documentació",
                "El client fa l'alta per WhatsApp → el bot recull les dades i envia llista de documents per email",
                PhaseDepType.REQUIRED, "1", 89, 25),
            phase(BusinessSector.GESTORIA, 3, "Gestió de Terminis i Recordatoris",
                "El bot monitoritza terminis fiscals i envia recordatoris per WhatsApp. Notifica el professional per Telegram",
                PhaseDepType.OPTIONAL, "2", 79, 20),
            phase(BusinessSector.GESTORIA, 4, "Enviament de Documents i Informes",
                "El bot envia documents processats, declaracions i informes per email. El professional pot afegir notes per Telegram",
                PhaseDepType.OPTIONAL, "2", 69, 18),
            phase(BusinessSector.GESTORIA, 5, "Gestió de Pagaments",
                "Recordatori de pagament mensual per WhatsApp. Factures per email. Escala impagaments",
                PhaseDepType.OPTIONAL, "2", 69, 15),
            phase(BusinessSector.GESTORIA, 6, "Renovació i Fidelització",
                "A prop del final d'any el bot contacta el client i ofereix revisió anual amb descompte per continuïtat",
                PhaseDepType.OPTIONAL, "2", 49, 12)
        ));

        // ── Agència IA ───────────────────────────────────────────────────
        repo.saveAll(List.of(
            phase(BusinessSector.AGENCIA_IA, 1, "Captació i Presentació",
                "El lead consulta serveis, tarifes i casos d'èxit per WhatsApp → el bot respon i qualifica l'interès",
                PhaseDepType.BASE, null, 150, 89),
            phase(BusinessSector.AGENCIA_IA, 2, "Qualificació i Reunió",
                "El bot recull informació sobre el projecte (sector, objectiu, pressupost) i agenda la primera reunió",
                PhaseDepType.REQUIRED, "1", 200, 49),
            phase(BusinessSector.AGENCIA_IA, 3, "Pressupost Automàtic",
                "L'equip defineix l'abast per Telegram → el bot genera un pressupost preliminar i l'envia per email per aprovació",
                PhaseDepType.OPTIONAL, "2", 150, 39),
            phase(BusinessSector.AGENCIA_IA, 4, "Onboarding de Projecte",
                "En confirmar el projecte el bot coordina el kick-off: envia qüestionari d'alta, recull credencials i assigna equip",
                PhaseDepType.OPTIONAL, "2", 100, 29),
            phase(BusinessSector.AGENCIA_IA, 5, "Reporting i Seguiment",
                "Actualitzacions automàtiques d'estat de projecte per WhatsApp. KPIs mensuals per email. Alertes d'incidències",
                PhaseDepType.OPTIONAL, "4", 100, 29),
            phase(BusinessSector.AGENCIA_IA, 6, "Expansió de Compte",
                "Detecta senyals d'upsell (creixement del client, nous sectors) i genera propostes noves automàticament",
                PhaseDepType.OPTIONAL, "2", 150, 49)
        ));

        // ── Inmobiliaria ─────────────────────────────────────────────────
        repo.saveAll(List.of(
            phase(BusinessSector.INMOBILIARIA, 1, "Captació de Propietats",
                "El propietari registra la propietat per WhatsApp → el bot recull dades i sol·licita fotos. Doc per email",
                PhaseDepType.BASE, null, 119, 79),
            phase(BusinessSector.INMOBILIARIA, 2, "Cerca i Filtratge",
                "El client descriu el que busca per WhatsApp → el bot filtra el catàleg i retorna propietats que encaixen",
                PhaseDepType.REQUIRED, "1", 89, 28),
            phase(BusinessSector.INMOBILIARIA, 3, "Agenda de Visites",
                "El client sol·licita visita → el bot coordina disponibilitat de l'agent i el propietari i confirma. Recordatori inclòs",
                PhaseDepType.REQUIRED, "1,2", 99, 25),
            phase(BusinessSector.INMOBILIARIA, 4, "Seguiment Post-visita",
                "Després de la visita el bot recull feedback del client i qualifica el lead. Notifica l'agent per Telegram",
                PhaseDepType.OPTIONAL, "3", 69, 18),
            phase(BusinessSector.INMOBILIARIA, 5, "Gestió d'Ofertes",
                "El client fa l'oferta per WhatsApp → el bot la trasllada a l'agent i al propietari. Contraoferes al fil. Doc per email",
                PhaseDepType.OPTIONAL, "3", 89, 18),
            phase(BusinessSector.INMOBILIARIA, 6, "Fidelització i Referèncias",
                "Valoració automàtica en tancar operació. Programa de referits amb incentiu per nou client referit",
                PhaseDepType.OPTIONAL, "1", 39, 12)
        ));
    }

    private SectorPhase phase(BusinessSector sector, int number, String name, String description,
                               PhaseDepType depType, String requiredPhases, int setup, int monthly) {
        return phase(sector, number, name, description, depType, requiredPhases, setup, monthly, aiCents(number), waMsgs(number));
    }

    private SectorPhase phase(BusinessSector sector, int number, String name, String description,
                               PhaseDepType depType, String requiredPhases, int setup, int monthly,
                               int aiCents, int waMsgs) {
        return SectorPhase.builder()
                .sector(sector).phaseNumber(number)
                .name(name).description(description)
                .dependencyType(depType).requiredPhases(requiredPhases)
                .setupPrice(BigDecimal.valueOf(setup))
                .monthlyPrice(BigDecimal.valueOf(monthly))
                .aiBudgetEurCents(aiCents)
                .whatsappMessageBudget(waMsgs)
                .build();
    }

    // Pressupostos per defecte per número de fase (admin pot ajustar per sector)
    private static int aiCents(int phaseNumber) {
        return switch (phaseNumber) {
            case 1 -> 500;
            case 2 -> 200;
            case 3 -> 100;
            case 4 ->  50;
            default ->  50;
        };
    }

    private static int waMsgs(int phaseNumber) {
        return switch (phaseNumber) {
            case 1 -> 200;
            case 2 -> 100;
            case 3 ->  50;
            case 4 ->  30;
            default ->   0;
        };
    }
}
