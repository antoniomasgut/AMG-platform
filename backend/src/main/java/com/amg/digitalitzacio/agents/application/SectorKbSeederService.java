package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sembra la Knowledge Base amb contingut base del sector quan el tenant s'activa.
 * Només actua si la KB és buida — no sobreescriu contingut existent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SectorKbSeederService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;

    record SeedEntry(KnowledgeCategory category, String key, String content, int order) {}

    @Transactional
    public void seedIfEmpty(UUID tenantId, String sectorName) {
        if (tenantId == null || sectorName == null) return;

        BusinessSector sector;
        try {
            sector = BusinessSector.valueOf(sectorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[KbSeed] Sector desconegut: {}", sectorName);
            return;
        }

        var kb = knowledgeBaseRepository.findByTenantId(tenantId).orElseGet(() -> {
            var newKb = new KnowledgeBase();
            newKb.setTenantId(tenantId);
            newKb.setIsActive(true);
            return knowledgeBaseRepository.save(newKb);
        });

        if (knowledgeEntryRepository.countByKnowledgeBaseId(kb.getId()) > 0) {
            log.debug("[KbSeed] KB ja té contingut per tenant {} — saltant", tenantId);
            return;
        }

        List<SeedEntry> entries = buildSeedEntries(sector);
        for (var e : entries) {
            var entry = KnowledgeEntry.builder()
                    .knowledgeBaseId(kb.getId())
                    .category(e.category())
                    .key(e.key())
                    .content(e.content())
                    .sortOrder(e.order())
                    .isActive(true)
                    .build();
            knowledgeEntryRepository.save(entry);
        }

        log.info("[KbSeed] {} entrades sembrades per tenant {} (sector {})", entries.size(), tenantId, sector);
    }

    private List<SeedEntry> buildSeedEntries(BusinessSector sector) {
        var common = List.of(
            new SeedEntry(KnowledgeCategory.BUSINESS_INFO, "descripcio_general",
                "Sóc l'agent IA del negoci. Puc ajudar-te a demanar informació, obtenir un pressupost o reservar una cita.", 0),
            new SeedEntry(KnowledgeCategory.SCHEDULE, "horari_general",
                "L'horari d'atenció és de dilluns a divendres de 9h a 18h. Pots deixar-me el teu missatge fora d'horari i et contestarem el següent dia laborable.", 0),
            new SeedEntry(KnowledgeCategory.FAQ, "com_contactar",
                "Pots contactar-nos per WhatsApp, per telèfon o omplint el formulari de la nostra web. Respondrem el més aviat possible.", 0),
            new SeedEntry(KnowledgeCategory.FAQ, "pressupost_gratuit",
                "Sí, els pressupostos són gratuïts i sense compromís. Pots sol·licitar-ne un directament per aquí.", 1),
            new SeedEntry(KnowledgeCategory.FAQ, "zona_servei",
                "Treballem principalment a Mallorca. Contacta'ns per confirmar si cobrim la teva zona.", 2)
        );

        var specific = switch (sector) {
            case PINTOR -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_pintura",
                    "Oferim serveis de pintura interior i exterior, pintura decorativa, estucat i vernissat de fusteria.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "temps_feina_pintura",
                    "El temps depèn de la superfície. Un pis de 90m² interior sol tardar entre 3 i 5 dies.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "tipus_pintura",
                    "Treballem amb marques de qualitat com Titan, Valentine i Jotun. T'assessorem sobre el millor acabat per a cada superfície.", 4)
            );
            case ELECTRICISTA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_electricitat",
                    "Instal·lació i reparació elèctrica, quadres de llum, punts de càrrega per a vehicle elèctric, domòtica i certificats d'instal·lació.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "urgencies_electricitat",
                    "Atenem urgències elèctriques. En cas d'emergència, contacta'ns directament per telèfon.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "certificat_instalacio",
                    "Emetem certificats d'instal·lació elèctrica (BT) homologats per a tràmits oficials i companyies subministradores.", 4)
            );
            case FONTANER -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_fontaneria",
                    "Reparació de fuites, instal·lació de sanitaris, calefacció, aire condicionat i manteniment de calderes.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "urgencies_fontaneria",
                    "Atenem urgències de fontaneria com trencaments o fuites. Contacta'ns directament per prioritzar l'atenció.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "manteniment_caldera",
                    "Recomanem fer una revisió anual de la caldera per garantir l'eficiència i complir amb la normativa.", 4)
            );
            case JARDINER -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_jardineria",
                    "Manteniment de jardins, disseny de zones verdes, talla de gespa, sistemes de reg i tractaments fitosanitaris.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "freq_manteniment",
                    "La freqüència recomanada és quinzenal a l'estiu i mensual a l'hivern, depenent del tipus de jardí.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "pressupost_jardí",
                    "El pressupost de manteniment depèn de la mida i tipus de jardí. Sol·licita una visita gratuïta per obtenir un preu exacte.", 4)
            );
            case FISIOTERAPEUTA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_fisio",
                    "Fisioteràpia musculoesquelètica, tractaments de columna, rehabilitació post-operatòria, drenatge limfàtic i teràpia manual.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "primera_visita",
                    "La primera visita inclou una valoració completa. Dura uns 60 minuts i t'expliquem el pla de tractament.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "assegurances_fisio",
                    "Treballem amb les principals assegurances mèdiques. Consulta'ns si la teva hi és inclosa.", 4)
            );
            case PERRUQUERIA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_perruqueria",
                    "Talls, coloració, mecxes, tractaments capil·lars, permanents i arranjaments per a ocasions especials.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "reserva_cita",
                    "Pots reservar cita directament per aquí o trucar-nos. Recomanem reservar amb 2-3 dies d'antelació.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "preu_tall",
                    "Els preus varien segons el servei. Contacta'ns per a informació detallada del nostre menú de serveis.", 4)
            );
            case ESTETICA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_estetica",
                    "Depilació làser, tractaments facials, massatges, ungles, tractaments corporals i estètica avançada.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "primera_sessio",
                    "Recomanem una consulta inicial gratuïta per valorar el teu cas i recomanar el tractament adequat.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "resultats_laser",
                    "Els resultats de la depilació làser varien per persona. En general es necessiten entre 6 i 8 sessions.", 4)
            );
            case GESTORIA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_gestoria",
                    "Assessoria fiscal i comptable, declaració de renda, autònoms, societats, nòmines i tràmits administratius.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "renda_termini",
                    "La campanya de la Renda sol obrir-se a l'abril. Recomanem no esperar a l'últim moment per evitar cues.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "nou_autonom",
                    "T'ajudem amb tot el procés d'alta com a autònom: tràmits a Hisenda, Seguretat Social i assessorament inicial.", 4)
            );
            case TALLER_MECANIC -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_taller",
                    "Manteniment i reparació de vehicles, canvi d'oli, frens, ITV, pneumàtics i diagnòstic electrònic.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "pressupost_reparacio",
                    "T'enviem un pressupost detallat abans de començar qualsevol reparació. Mai fem res sense la teva aprovació.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "temps_reparacio",
                    "El temps depèn de la reparació. Un manteniment bàsic sol estar llest el mateix dia.", 4)
            );
            case VETERINARI, PERRUQUERIA_CANINA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_veterinari",
                    sector == BusinessSector.VETERINARI
                        ? "Consultes veterinàries, vacunacions, cirurgia, urgències, anàlisis i assessorament nutricional."
                        : "Bany, talla, estètica canina i felina, tractaments de pelatge i recollida a domicili.",
                    0),
                new SeedEntry(KnowledgeCategory.FAQ, "primera_visita_animal",
                    "La primera visita inclou una revisió general de l'animal. Porta el carnet de vacunació si en tens.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "urgencies_vet",
                    "Atenem urgències veterinàries durant l'horari de consulta. Fora d'horari, contacta la clínica d'urgències més propera.", 4)
            );
            case ACADEMIA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_academia",
                    "Classes particulars, reforç escolar, idiomes, preparació d'exàmens oficials i cursos de formació.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "classes_prova",
                    "Oferim una classe de prova gratuïta per a nous alumnes. Contacta'ns per concertar-la.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "grups_nivell",
                    "Formem grups reduïts per nivell i edat per garantir un aprenentatge efectiu.", 4)
            );
            case MARE_DE_DIA -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "servei_mare_de_dia",
                    "Som una llar familiar d'atenció a la infància. Cuidem nens i nenes de 4 mesos a 3 anys en un entorn familiar i personalitzat, amb un màxim de 6 infants per grup.", 0),
                new SeedEntry(KnowledgeCategory.SCHEDULE, "horari",
                    "Atenem de dilluns a divendres de 7:30h a 17:00h. Consulta disponibilitat per a mitja jornada o jornada completa.", 1),
                new SeedEntry(KnowledgeCategory.BUSINESS_INFO, "preus",
                    "La quota mensual inclou àpats adaptats a l'edat, material didàctic i activitats de desenvolupament. Contacta'ns per conèixer el preu actual i la disponibilitat de places.", 2),
                new SeedEntry(KnowledgeCategory.FAQ, "documentacio",
                    "Per formalitzar la inscripció cal: cartilla de vacunació actualitzada, targeta sanitària, autorització mèdica per a urgències i contracte d'inscripció signat.", 3),
                new SeedEntry(KnowledgeCategory.FAQ, "visita_coneixenca",
                    "Oferim visites de coneixença sense compromís per a les famílies interessades. Pots sol·licitar-la aquí i et proposem data i hora disponible.", 4),
                new SeedEntry(KnowledgeCategory.FAQ, "places_espera",
                    "Les places són limitades. Si en aquest moment no n'hi ha cap de lliure, t'inclourem a la llista d'espera i t'avisarem en primer lloc quan n'hi hagi una disponible.", 5),
                new SeedEntry(KnowledgeCategory.FAQ, "infants_malalts",
                    "Per salut de tots els infants, no podem atendre nens amb febre, vòmits o malalties contagioses. En cas de malaltia, cal avisar abans de les 8h del matí.", 6)
            );
            default -> List.of(
                new SeedEntry(KnowledgeCategory.SERVICE, "serveis_principals",
                    "Oferim serveis professionals de qualitat. Contacta'ns per a més informació sobre el que podem fer per tu.", 0),
                new SeedEntry(KnowledgeCategory.FAQ, "com_demanar",
                    "Pots sol·licitar informació, demanar un pressupost o reservar una cita directament per aquí.", 3)
            );
        };

        return new java.util.ArrayList<>() {{
            addAll(common);
            addAll(specific);
        }};
    }
}
