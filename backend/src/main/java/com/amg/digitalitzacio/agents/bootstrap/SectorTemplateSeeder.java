package com.amg.digitalitzacio.agents.bootstrap;

import com.amg.digitalitzacio.agents.domain.SectorTemplate;
import com.amg.digitalitzacio.agents.domain.SectorTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorTemplateSeeder {

    private final SectorTemplateRepository repository;

    @PostConstruct
    void seed() {
        if (repository.count() > 0) {
            log.info("Sector templates table already populated ({} rows), skipping seed", repository.count());
            return;
        }

        var templates = buildTemplates();
        repository.saveAll(templates);
        log.info("Seeded {} sector templates", templates.size());
    }

    private List<SectorTemplate> buildTemplates() {
        return List.of(
            // ─── RESTAURANTE ──────────────────────────────────────
            tpl("RESTAURANTE", "prospecting", "Email fred (principal)",
                "Assumpte: Una pregunta ràpida sobre {NOM_NEGOCI}\n\nHola {NOM_CONTACTE},\n\nHe vist que {NOM_NEGOCI} té {NUM_REVIEWS} ressenyes a Google amb {ESTRELLES} estrelles — els últims comentaris parlen de {PROBLEMA_OBSERVAT}.\n\nAjudem restaurants a {CIUTAT} a millorar la seua presència digital i omplir les hores baixes.\n\nA {CLIENT_REF} vam aconseguir +23% reserves entre setmana en 90 dies.\n\nVols que t'enviï un resum de com ho fem?\n\n{TEU_NOM}\n\n---\nTo: Casual, eficient | Millor dia: Dijous | Hora: 15-17h | CTA: Baixa fricció", 0),
            tpl("RESTAURANTE", "prospecting", "Seguiment (5-7 dies)",
                "Assumpte: {NOM_NEGOCI} — idea ràpida\n\nHola {NOM_CONTACTE},\n\nNomés un segon missatge. Molts restaurants ens diuen que el problema no és tenir clients, sinó que repeteixin entre setmana.\n\nLa solució: un programa de fidelització per WhatsApp. Punts per visita, descompte al cinquè cop.\n\nVols que t'ho expliqui en 5 minuts?", 1),
            tpl("RESTAURANTE", "meta-ads", "Campanya de captació",
                "Objectiu: Trànsit a web + reserves\nRadi: {RADI}km | Edat: 25-65 | Interessos: Gastronomia\n\nCreativitats:\n1. \"Sopar per a 2 + vi = {PREU_MENU}€ — Només {DIES} a la setmana\"\n2. \"Menú diari per {PREU_MENU}€ — De dilluns a divendres\"\n3. Vídeo: Plat estrella del xef\n\nPressupost: {PRESSUPOST}€/dia | 7 dies / 2 creativitats", 0),
            tpl("RESTAURANTE", "meta-ads", "Campanya de fidelització",
                "Objectiu: Repetició de visita\nPúblic: Clients existents (Custom Audience de 180 dies)\n\nCreativitats:\n1. \"Torna: 10% descompte en la teva pròxima visita\"\n2. \"Aquest mes: {OFERTA_ESPECIAL}\"\n\nPressupost: {PRESSUPOST}€/dia | 5 dies", 1),
            tpl("RESTAURANTE", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un restaurant a {CIUTAT} (Mallorca).\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de taula\n2. Informar sobre la carta, menú del dia i especialitats\n3. Gestionar al·lèrgies i intoleràncies\n4. Gestionar grups (+8 persones → derivar)\n5. Promocionar ofertes especials\n\nPER A RESERVA: dia/hora, comensals, al·lèrgies, ocasió, nom i telèfon\n\nHORARI: {HORARI_DINAR} / {HORARI_SOPAR} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── TALLER_MECANIC ───────────────────────────────────
            tpl("TALLER_MECANIC", "prospecting", "Email fred",
                "Assumpte: Manteniment per a {NOM_NEGOCI}\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem tallers a {CIUTAT} a omplir els forats de calendari amb recordatoris automàtics per WhatsApp.\n\nEls clients reben un avís quan toca revisió (ITV, canvi d'oli, pneumàtics) i contesten per reservar directament.\n\nResultat: +30% reserves preventives, menys dies morts.\n\nVols que t'ho ensenyi en 5 minuts?\n\n{TEU_NOM}", 0),
            tpl("TALLER_MECANIC", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (crida o formulari)\nRadi: {RADI}km | Edat: 25-65 | Homeowners\n\nCreativitats:\n1. \"Revisió aire condicionat {PREU_REVISIO}€ — inclou gas + diagnosi\"\n2. \"ITV propera? Revisió prèvia i vine tranquil\"\n3. Vídeo testimoni: Client content recollint el cotxe\"", 0),
            tpl("TALLER_MECANIC", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un taller mecànic a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de cites\n2. Informar sobre serveis\n3. Fer recordatoris de manteniment (ITV, olis, pneumàtics)\n4. Derivar pressupostos complexos al responsable\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── FISIOTERAPEUTA ───────────────────────────────────
            tpl("FISIOTERAPEUTA", "prospecting", "Email fred",
                "Assumpte: Fisioteràpia a {CIUTAT} — {NOM_NEGOCI}\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem centres de fisioteràpia a reduir les cancel·lacions d'última hora.\n\nSistema automàtic: confirmació 24h abans per WhatsApp + llista d'espera automàtica per omplir forats.\n\nResultat: menys cancel·lacions i més pacients.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("FISIOTERAPEUTA", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (formulari de cita)\nRadi: {RADI}km | Edat: 30-65 | Dones 60%\n\nCreativitats:\n1. \"Primera visita: {PREU_VISITA}€ — valoració completa\"\n2. \"Tens mal d'esquena? Prova una sessió de prova\"\n3. \"Tornar a caminar sense dolor és possible\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("FISIOTERAPEUTA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un centre de fisioteràpia a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de sessions\n2. Informar sobre tractaments\n3. Recordatori de sessions\n4. Gestió de cancel·lacions i replanificacions\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── PSICOLEG ─────────────────────────────────────────
            tpl("PSICOLEG", "prospecting", "Email fred",
                "Assumpte: Com podem ajudar-te a fer créixer {NOM_NEGOCI}\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem psicòlegs a gestionar les cites i millorar l'experiència del pacient.\n\nReserva online 24/7, recordatoris automàtics per WhatsApp i gestió de llista d'espera.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("PSICOLEG", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (formulari)\nRadi: {RADI}km | Edat: 25-55\n\nCreativitats:\n1. \"Primera visita: {PREU_VISITA}€\"\n2. \"Teràpia online disponible — des de casa teva\"\n3. \"Especialista en ansietat i estrès\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("PSICOLEG", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un consultori de psicologia a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de sessions\n2. Informar sobre serveis sense donar diagnòstics\n3. Recordatori de sessions\n4. Derivar urgències al 024\n\nHORARI: {HORARI} | Consulta online disponible: {ONLINE}", 0),

            // ─── PERRUQUERIA ──────────────────────────────────────
            tpl("PERRUQUERIA", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — idea per omplir agenda\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem perruqueries a omplir els forats d'agenda.\n\nSistema de reserves online 24/7 + WhatsApp automàtic que recorda als clients quan toca repetició (color, tractament).\n\n+25% reserves en clients que ja teniu.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("PERRUQUERIA", "meta-ads", "Campanya de captació",
                "Objectiu: Reserves (formulari)\nRadi: {RADI}km | Edat: 25-65 | Dones 80%, Homes 20%\n\nCreativitats:\n1. \"Primer tall: {PREU_TALL}€\"\n2. \"Color + tall: {PREU_COLOR}€\"\n3. \"Tractament capil·lar: {PREU_TRACTAMENT}€\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("PERRUQUERIA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una perruqueria/estètica capil·lar a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de cites\n2. Informar sobre serveis\n3. Promocionar ofertes\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── ELECTRICISTA ─────────────────────────────────────
            tpl("ELECTRICISTA", "prospecting", "Email fred",
                "Assumpte: Electricista a {CIUTAT} — {NOM_NEGOCI}\n\nHola {NOM_CONTACTE},\n\nA {EMPRESA} ajudem electricistes a captar clients sense esforç.\n\nSistema automàtic: campanya de Meta Ads + WhatsApp que gestiona les sol·licituds.\n\nEl client arriba, demana pressupost i reps l'alerta al mòbil.\n\nVols que t'ho ensenyi en 5 minuts?\n\n{TEU_NOM}", 0),
            tpl("ELECTRICISTA", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (crida)\nRadi: {RADI}km | Edat: 25-65 | Homeowners\n\nCreativitats:\n1. \"Urgència elèctrica? Arribem en 1 hora\"\n2. \"Canvi de llum: {PREU_LLUM}€\"\n3. \"Certificat elèctric: {PREU_CERTIFICAT}€\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("ELECTRICISTA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un electricista professional a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar sol·licituds de pressupost\n2. Prioritzar urgències\n3. Derivar emergències immediatament\n\nHORARI: {HORARI} | Urgències 24h: {URGENCIES}", 0),

            // ─── FONTANER ─────────────────────────────────────────
            tpl("FONTANER", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — com captar més clients\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem fontaners a tenir clients cada dia.\n\nSistema de Meta Ads + formulari web que envia les sol·licituds directament al teu WhatsApp.\n\nSense esforç, només rebre avisos.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("FONTANER", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (crida)\nRadi: {RADI}km | Edat: 25-65 | Homeowners\n\nCreativitats:\n1. \"Fuita? Reparació en 2h\"\n2. \"Canvi de caldera: {PREU_CALDERA}€\"\n3. \"Desembús: {PREU_DESEMBUS}€\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("FONTANER", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, un fontaner professional a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar sol·licituds de pressupost i urgències\n2. Preguntar per la urgència per prioritzar\n3. Derivar emergències\n\nHORARI: {HORARI} | Urgències 24h: {URGENCIES}", 0),

            // ─── VETERINARI ───────────────────────────────────────
            tpl("VETERINARI", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — fidelització de clients\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem clíniques veterinàries a automatitzar recordatoris de vacunes i revisions.\n\nEls clients reben un recordatori per WhatsApp quan toca revisió o vacuna, i contesten per reservar.\n\n+30% visites de manteniment.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("VETERINARI", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (formulari)\nRadi: {RADI}km | Edat: 25-65 | Propietaris mascotes\n\nCreativitats:\n1. \"Primera visita: {PREU_VISITA}€ — inclou xip + vacuna\"\n2. \"Revisió anual: {PREU_REVISIO}€\"\n3. \"Desparasitació: {PREU_DESPARASITACIO}€\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("VETERINARI", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una clínica veterinària a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar reserves de consultes\n2. Informar sobre serveis\n3. Recordatori de vacunacions\n4. Derivar urgències\n\nHORARI: {HORARI} | Urgències: {URGENCIES}", 0),

            // ─── GESTORIA ─────────────────────────────────────────
            tpl("GESTORIA", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — estalvieu temps amb automatització\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem gestories a oferir serveis digitals als seus clients.\n\nDes de portals de client online fins a automatització de recordatoris fiscals.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("GESTORIA", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (formulari)\nRadi: {RADI}km | Edat: 30-65 | Emprenedors\n\nCreativitats:\n1. \"Portal del client: accedeix als teus documents 24/7\"\n2. \"Recordatoris fiscals automàtics — mai un retard\"\n3. \"Estalvia 10 hores/mes en gestions\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("GESTORIA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una gestoria a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar sol·licituds de contacte\n2. Informar sobre serveis sense assessorament vinculant\n3. Derivar consultes tècniques\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── ACADEMIA ─────────────────────────────────────────
            tpl("ACADEMIA", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — més alumnes per al curs vinent\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem acadèmies a omplir places amb campanyes automàtiques.\n\nSistema de Meta Ads + formulari de contacte + WhatsApp.\n\nResultat: +30% sol·licituds de matrícula.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("ACADEMIA", "meta-ads", "Campanya de captació",
                "Objectiu: Leads (formulari)\nRadi: {RADI}km | Edat: 18-65\n\nCreativitats:\n1. \"Prova gratuïta: primera classe sense compromís\"\n2. \"Cursos intensius: estiu 2026\"\n3. \"Preparació d'exàmens: {PREU_CURS}€/mes\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("ACADEMIA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una acadèmia a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Informar sobre cursos i horaris\n2. Gestionar sol·licituds de matrícula\n3. Gestionar classes de prova\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── INMOBILIARIA ─────────────────────────────────────
            tpl("INMOBILIARIA", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — leads qualificats cada setmana\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Ajudem immobiliàries a generar leads qualificats.\n\nMeta Ads + landing page + WhatsApp: el client interessat contacta automàticament per veure la propietat.\n\nVols que t'ho ensenyi?\n\n{TEU_NOM}", 0),
            tpl("INMOBILIARIA", "meta-ads", "Campanya de captació",
                "Objectiu: Visites programades\nRadi: {RADI}km | Edat: 25-65 | Cercant casa\n\nCreativitats:\n1. \"Pis a {CIUTAT}: {PREU_PIS}€ — visita avui\"\n2. \"Casa amb piscina a {ZONA} — {PREU_CASA}€\"\n3. \"Inversors: rendibilitat del 7% a {ZONA}\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("INMOBILIARIA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una immobiliària a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Gestionar sol·licituds de visites\n2. Informar sobre propietats\n3. Demanar preferències\n4. Derivar a l'assessor comercial\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0),

            // ─── AGENCIA_IA ───────────────────────────────────────
            tpl("AGENCIA_IA", "prospecting", "Email fred",
                "Assumpte: {NOM_NEGOCI} — col·laboració entre agències\n\nHola {NOM_CONTACTE},\n\nSoc {TEU_NOM} de {EMPRESA}. Som una agència de digitalització local. Treballem amb agències per externalitzar la implementació tècnica.\n\nTecnologia pròpia: IA conversacional, automatitzacions, webs.\n\nVols que t'expliqui com col·laborar?\n\n{TEU_NOM}", 0),
            tpl("AGENCIA_IA", "meta-ads", "Campanya de captació",
                "Objectiu: Leads B2B (formulari)\nRadi: {RADI}km | Edat: 25-55 | Càrrec: CEO/Màrqueting\n\nCreativitats:\n1. \"Automatitza el teu negoci amb IA — demo gratis\"\n2. \"Web + IA + WhatsApp en un sol producte\"\n3. \"Cas d'èxit: {CLIENT_REF} estalvia 20h/setmana\"\n\nPressupost: {PRESSUPOST}€/dia | 7 dies", 0),
            tpl("AGENCIA_IA", "agent-prompt", "Prompt base per a l'assistent",
                "Ets l'assistent virtual de {NOM_NEGOCI}, una agència d'IA i digitalització a {CIUTAT}.\n\nSERVEIS: {SERVEIS}\n\nROL PRINCIPAL:\n1. Informar sobre serveis\n2. Gestionar sol·licituds de consulta\n3. Qualificar leads\n4. Derivar projectes complexos\n\nHORARI: {HORARI} | Tancat: {DIA_TANCAMENT}", 0)
        );
    }

    private SectorTemplate tpl(String sector, String type, String title, String body, int sortOrder) {
        return SectorTemplate.builder()
                .sector(sector)
                .type(type)
                .title(title)
                .body(body)
                .sortOrder(sortOrder)
                .build();
    }
}
