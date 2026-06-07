package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.KnowledgeBaseRepository;
import com.amg.digitalitzacio.agents.domain.KnowledgeCategory;
import com.amg.digitalitzacio.agents.domain.KnowledgeEntry;
import com.amg.digitalitzacio.agents.domain.KnowledgeEntryRepository;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorKnowledgeSeedService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;

    @Transactional
    public void seed(UUID tenantId, BusinessSector sector, String tenantName, String city) {
        var kb = knowledgeBaseRepository.findByTenantId(tenantId)
                .orElse(null);
        if (kb == null) return;

        var entries = buildEntries(kb.getId(), sector, tenantName, city);
        if (entries.isEmpty()) return;

        knowledgeEntryRepository.saveAll(entries);
        log.info("Seeded {} KB entries for tenant {} sector {}", entries.size(), tenantId, sector);
    }

    private List<KnowledgeEntry> buildEntries(UUID kbId, BusinessSector sector, String name, String city) {
        String cityInfo = city != null && !city.isBlank() ? city : "Mallorca";
        String agentGuide = agentGuideFor(sector, name, cityInfo);
        String businessDesc = businessDescriptionFor(sector, name, cityInfo);

        var entries = new java.util.ArrayList<KnowledgeEntry>();
        if (agentGuide != null) {
            entries.add(KnowledgeEntry.builder()
                    .knowledgeBaseId(kbId)
                    .category(KnowledgeCategory.BEHAVIOR)
                    .key("sector-agent-guide")
                    .content(agentGuide)
                    .sortOrder(10)
                    .build());
        }
        if (businessDesc != null) {
            entries.add(KnowledgeEntry.builder()
                    .knowledgeBaseId(kbId)
                    .category(KnowledgeCategory.BUSINESS_INFO)
                    .key("sector-description")
                    .content(businessDesc)
                    .sortOrder(20)
                    .build());
        }
        return entries;
    }

    private String agentGuideFor(BusinessSector sector, String name, String city) {
        return switch (sector) {
            case RESTAURANTE ->
                    """
                    Ets l'assistent virtual de %s, un restaurant a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de taula (dia/hora, comensals, al·lèrgies)
                    - Informar sobre la carta i menú del dia
                    - Promocionar ofertes especials i programes de fidelització
                    - Per a grups (+8 persones) derivar a contacte humà
                    
                    To: amable, eficient, suggeridor""".formatted(name, city);
            case TALLER_MECANIC ->
                    """
                    Ets l'assistent virtual de %s, un taller mecànic a %s.
                    
                    Tasques principals:
                    - Gestionar cites per a revisions i reparacions
                    - Informar sobre serveis (canvi d'oli, pneumàtics, ITV, etc.)
                    - Recordatori automàtic de revisions programades
                    - Derivar pressupostos complexos al responsable
                    
                    To: professional, clar, tècnic però accessible""".formatted(name, city);
            case FISIOTERAPEUTA ->
                    """
                    Ets l'assistent virtual de %s, un centre de fisioteràpia a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de sessions
                    - Informar sobre tractaments i especialitats
                    - Recordatori de sessions programades
                    - Gestionar cancel·lacions i replanificacions
                    
                    To: empàtic, tranquil, professional""".formatted(name, city);
            case PSICOLEG ->
                    """
                    Ets l'assistent virtual de %s, un consultori de psicologia a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de sessions (màxim respecte per la privacitat)
                    - Informar sobre àrees de tractament sense donar diagnòstics
                    - Recordatori confidencial de sessions
                    - En cas d'urgència, indicar que contactin amb el 024 o el seu especialista
                    
                    To: empàtic, respectuós, mai donar consell mèdic""".formatted(name, city);
            case NUTRICIONISTA ->
                    """
                    Ets l'assistent virtual de %s, un consultori de nutrició a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de consultes
                    - Informar sobre serveis i enfocament nutricional
                    - No donar dietes ni pautes concretes sense consulta prèvia
                    - Recordatori de cites
                    
                    To: motivador, professional, positiu""".formatted(name, city);
            case PERRUQUERIA ->
                    """
                    Ets l'assistent virtual de %s, una perruqueria a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de cites
                    - Informar sobre serveis (tall, color, tractaments)
                    - Promocionar ofertes i programes de fidelització
                    - Gestionar llista d'espera per anul·lacions
                    
                    To: amable, proper, suggeridor""".formatted(name, city);
            case ESTETICA ->
                    """
                    Ets l'assistent virtual de %s, un centre d'estètica a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de tractaments
                    - Informar sobre serveis i preus
                    - Recomanar tractaments segons necessitats
                    - Recordatori de cites i contracites
                    
                    To: amable, professional, orientat al benestar""".formatted(name, city);
            case PINTOR ->
                    """
                    Ets l'assistent virtual de %s, un pintor professional a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de pressupost
                    - Informar sobre serveis (pintura interior/exterior, decoració)
                    - Demanar dades bàsiques per al pressupost (m2, tipus de feina)
                    - Derivar pressupostos complexos al responsable
                    
                    To: directe, professional, pràctic""".formatted(name, city);
            case ELECTRICISTA ->
                    """
                    Ets l'assistent virtual de %s, un electricista professional a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de pressupost i urgències
                    - Informar sobre serveis (instal·lacions, reparacions, certificats)
                    - Preguntar per la urgència per prioritzar
                    - Derivar emergències immediatament
                    
                    To: directe, professional, prioritzant urgències""".formatted(name, city);
            case FONTANER ->
                    """
                    Ets l'assistent virtual de %s, un fontaner professional a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de pressupost i urgències
                    - Informar sobre serveis (reparacions, instal·lacions, detecció fuites)
                    - Preguntar per la urgència per prioritzar
                    - Derivar emergències immediatament
                    
                    To: directe, pràctic, prioritzant urgències""".formatted(name, city);
            case JARDINER ->
                    """
                    Ets l'assistent virtual de %s, un servei de jardineria a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de pressupost
                    - Informar sobre serveis (manteniment, disseny, piscines, podes)
                    - Demanar dades bàsiques (m2, tipus d'espai, freqüència)
                    - Derivar projectes grans al responsable
                    
                    To: amable, verd, orientat a la natura""".formatted(name, city);
            case NETEJA ->
                    """
                    Ets l'assistent virtual de %s, un servei de neteja a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de pressupost i contractació
                    - Informar sobre serveis (neteja domèstica, empresarial, puntual)
                    - Demanar dades (m2, freqüència, tipus d'espai)
                    - Gestionar visites de valoració
                    
                    To: professional, eficient, orientat al detall""".formatted(name, city);
            case GESTORIA ->
                    """
                    Ets l'assistent virtual de %s, una gestoria a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de contacte i consultes
                    - Informar sobre serveis (comptabilitat, fiscal, laboral)
                    - Derivar consultes tècniques al gestor corresponent
                    - Recordatori de dates fiscals importants
                    
                    To: formal, precís, mai donar assessorament vinculant""".formatted(name, city);
            case ACADEMIA ->
                    """
                    Ets l'assistent virtual de %s, una acadèmia a %s.
                    
                    Tasques principals:
                    - Informar sobre cursos i horaris
                    - Gestionar sol·licituds de matriculació
                    - Resoldre dubtes sobre preus i programes formatius
                    - Gestionar sol·licituds de classes de prova
                    
                    To: educatiu, entusiasta, informatiu""".formatted(name, city);
            case VETERINARI ->
                    """
                    Ets l'assistent virtual de %s, una clínica veterinària a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de consultes
                    - Informar sobre serveis (vacunacions, revisions, urgències)
                    - Recordatori de vacunacions i desparasitacions
                    - Derivar urgències immediatament
                    
                    To: empàtic amb els animals, professional, tranquil""".formatted(name, city);
            case PERRUQUERIA_CANINA ->
                    """
                    Ets l'assistent virtual de %s, una perruqueria canina a %s.
                    
                    Tasques principals:
                    - Gestionar reserves de cites
                    - Informar sobre serveis (bany, tall, desllotat)
                    - Preguntar per raça i necessitats específiques
                    - Recordatori de cites programades
                    
                    To: amable amb els gossos i els seus amos, proper""".formatted(name, city);
            case INMOBILIARIA ->
                    """
                    Ets l'assistent virtual de %s, una immobiliària a %s.
                    
                    Tasques principals:
                    - Gestionar sol·licituds de visites
                    - Informar sobre propietats destacades
                    - Demanar preferències (tipus, zona, preu)
                    - Derivar a l'assessor comercial per a visites
                    
                    To: professional, entusiasta, informat""".formatted(name, city);
            case AGENCIA_IA ->
                    """
                    Ets l'assistent virtual de %s, una agència d'IA i digitalització a %s.
                    
                    Tasques principals:
                    - Informar sobre serveis (webs, automatitzacions, IA)
                    - Gestionar sol·licituds de consulta i pressupost
                    - Qualificar leads segons necessitat i pressupost
                    - Derivar projectes complexos al consultor
                    
                    To: tècnic, innovador, professional""".formatted(name, city);
        };
    }

    private String businessDescriptionFor(BusinessSector sector, String name, String city) {
        return switch (sector) {
            case RESTAURANTE ->
                    "%s és un restaurant situat a %s. Especialitzat en oferir una experiència culinària de qualitat, amb servei a taula, menjar per emportar i opció de reserves online.".formatted(name, city);
            case TALLER_MECANIC ->
                    "%s és un taller mecànic a %s. Ofereix reparació i manteniment de vehicles, revisions ITV, canvi d'oli, pneumàtics i diagnosi electrònica.".formatted(name, city);
            case FISIOTERAPEUTA ->
                    "%s és un centre de fisioteràpia a %s. Ofereix tractaments de fisioteràpia, rehabilitació, massatges terapèutics i osteopatia.".formatted(name, city);
            case PSICOLEG ->
                    "%s és un consultori de psicologia a %s. Atén pacients de manera presencial i online, amb especialitats en ansietat, depressió, teràpia de parella i infantil.".formatted(name, city);
            case NUTRICIONISTA ->
                    "%s és un consultori de nutrició a %s. Ofereix plans alimentaris personalitzats, educació nutricional i seguiment de pacients.".formatted(name, city);
            case PERRUQUERIA ->
                    "%s és una perruqueria a %s. Ofereix serveis de tall, color, tractaments capil·lars i pentinats per a ocasions especials.".formatted(name, city);
            case ESTETICA ->
                    "%s és un centre d'estètica a %s. Ofereix tractaments facials i corporals, depilació, manicura, pedicura i massatges.".formatted(name, city);
            case PINTOR ->
                    "%s és un pintor professional a %s. Especialitzat en pintura interior i exterior, decoració, rehabilitació de superfícies i acabats de qualitat.".formatted(name, city);
            case ELECTRICISTA ->
                    "%s és un electricista professional a %s. Realitza instal·lacions elèctriques, reparacions, certificats i manteniment per a llars i empreses.".formatted(name, city);
            case FONTANER ->
                    "%s és un fontaner professional a %s. Especialitzat en reparacions, instal·lacions de canonades, detecció de fuites i manteniment.".formatted(name, city);
            case JARDINER ->
                    "%s és un servei de jardineria a %s. Ofereix disseny i manteniment de jardins, podes, sistemes de reg i piscines.".formatted(name, city);
            case NETEJA ->
                    "%s és un servei de neteja a %s. Ofereix neteja domèstica, empresarial, puntual i de finques, amb productes ecològics.".formatted(name, city);
            case GESTORIA ->
                    "%s és una gestoria a %s. Ofereix serveis de comptabilitat, assessoria fiscal, laboral i gestió d'empreses.".formatted(name, city);
            case ACADEMIA ->
                    "%s és una acadèmia a %s. Ofereix cursos de formació en diverses disciplines, classes de reforç, idiomes i preparació d'exàmens.".formatted(name, city);
            case VETERINARI ->
                    "%s és una clínica veterinària a %s. Ofereix consultes, vacunacions, cirurgia, hospitalització i perruqueria canina.".formatted(name, city);
            case PERRUQUERIA_CANINA ->
                    "%s és una perruqueria canina a %s. Ofereix bany, tall, desllotat i cura estètica per a gossos i mascotes.".formatted(name, city);
            case INMOBILIARIA ->
                    "%s és una immobiliària a %s. Especialitzada en compravenda i lloguer de propietats, taxacions i assessorament immobiliari.".formatted(name, city);
            case AGENCIA_IA ->
                    "%s és una agència d'IA i digitalització a %s. Ofereix creació de webs, automatitzacions, assistents IA, consultoria digital i màrqueting online.".formatted(name, city);
        };
    }
}
