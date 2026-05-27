type SectorTemplates = Partial<Record<'BOOKING_RULES' | 'QUOTE_RULES' | 'FOLLOWUP_RULES' | 'CANCELLATION_POLICY', string>>;

const T: Record<string, SectorTemplates> = {

  PINTOR: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon del client
- Tipus de treball: interior / exterior / vernís / esmalts
- Metres quadrats aproximats
- Estat de les parets (bé, esquerdes, humitat, paper pendent de treure)
- Quan necessiten la feina (urgència o dates flexibles)
- Adreça de l'obra

COM GESTIONAR:
- Per a treballs de fins a 2 habitacions: pots donar pressupost orientatiu i proposar data de visita
- Per a obres grans (>50m²): sempre visita prèvia obligatòria
- Temps de resposta a consultes: màxim 2 hores en horari laboral

EXEMPLE: "Hola! Per donar-te un pressupost exacte necessito: tipus de feina (interior/exterior), m² aproximats i estat de les parets. Pots compartir-me les dades i si cal, passem a fer una visita gratuïta."`,

    QUOTE_RULES: `DADES NECESSÀRIES:
- Metres quadrats (m²)
- Tipus de pintura: econòmica / estàndard / premium
- Nombre de mans: 1 / 2 / 3
- Preparació inclosa: sí (estucat, llixat) / no

FÓRMULA ORIENTATIVA:
- Pintura interior econòmica (2 mans): m² × 8€
- Pintura interior estàndard (2 mans): m² × 12€
- Pintura interior premium (2 mans + preparació): m² × 18€
- Pintura exterior: m² × 15€ - 22€ (depèn de l'alçada i accés)
- Sostre: multiplicar m² × 1.3 (més dificultat)
- Preparació intensa (esquerdes, paper): afegir 3-5€/m²

MATERIALS inclosos en tots els casos.
Desplaçaments fins 30km: inclosos. Fora: 0,35€/km.

EXEMPLE: "Per pintar 80m² interiors (2 mans, estàndard): 80 × 12€ = 960€. Preu orientatiu. Et puc passar pressupost detallat per email, el necessites?"`,

    FOLLOWUP_RULES: `SEGUIMENT POSTVENDA:
- 3 dies després d'acabar l'obra: "Hola [nom]! Com ha quedat la pintura? Estàs content/a amb el resultat? Si hi ha qualsevol detall a repassar, digues-m'ho sense problema."
- Si el client diu que ha quedat bé: demanar ressenya Google → [URL GOOGLE REVIEWS]

REACTIVACIÓ (client sense obra en +6 mesos):
- "Hola [nom]! Fa un temps que no ens posem en contacte. Si tens pendent pintar alguna estança o renovar l'exterior, ara treballem amb nous materials de millor durabilitat. Et faig pressupost sense compromís?"

SEGUIMENT PRESSUPOST PENDENT (5 dies sense resposta):
- "Hola [nom], et volia recordar que tens un pressupost pendent. Si vols ajustar alguna cosa o tens dubtes, estic aquí."`,
  },

  ELECTRICISTA: {
    BOOKING_RULES: `CLASSIFICACIÓ URGENT vs. PLANIFICAT (primer pas sempre):
- URGENT: curtcircuit, llum general caiguda, fum/espurnes, risc electrocució → notificar propietari IMMEDIATAMENT + proporcionar 112
- PLANIFICAT: instal·lació nova, reforma, canvi de quadre, punts de llum nous

DADES PER A VISITA PLANIFICADA:
- Nom i telèfon
- Descripció del treball
- Adreça
- Disponibilitat horària (matí/tarda)

DADES PER A URGÈNCIA:
- Adreça exacta
- Descripció del problema
- Si hi ha risc per a persones

TEMPS DE RESPOSTA:
- Urgències: avisar propietari i intentar atenció en <2h
- Planificat: cita en 2-5 dies laborables`,

    QUOTE_RULES: `DADES NECESSÀRIES:
- Tipus de treball (instal·lació nova, reparació, ampliació)
- Metres lineals de cablejat aproximats
- Nombre de punts de llum / endolls nous
- Si cal canviar el quadre elèctric

TARIFES ORIENTATIVES:
- Hora d'oficial: 45€/h (mínim 1h)
- Punt de llum nou (material + instal·lació): 80-120€
- Endoll nou: 60-90€
- Canvi de quadre bàsic (fins 10 circuits): 350-500€
- Canvi de quadre complet (>10 circuits): 600-900€
- Certificat elèctric per a lloguer/venda: 120-180€

Materials a part excepte si s'especifica.
Urgències fora d'horari: +30% sobre tarifa.`,

    FOLLOWUP_RULES: `SEGUIMENT POSTVENDA:
- 1 dia després d'un treball: "Hola [nom]! Tot funciona correctament? Si detectes algun detall a revisar, avisa'm."
- Si positiu: demanar ressenya → [URL GOOGLE REVIEWS]

RECORDATORI REVISIÓ (cada 2 anys per clients habituals):
- "Hola [nom]! Ha passat temps des de la darrera instal·lació. Recomanem una revisió del quadre elèctric cada 2 anys. Vols que passem a fer-la?"

REACTIVACIÓ (sense contacte en +1 any):
- "Hola [nom]! Si tens algun treball elèctric pendent o vols una revisió preventiva, estiguem disponibles. Fem pressupost sense compromís."`,
  },

  FONTANER: {
    BOOKING_RULES: `CLASSIFICACIÓ URGENT vs. PLANIFICAT:
- URGENT: inundació activa, fuita sense poder aturar, problemes de gas → notificar propietari IMMEDIATAMENT. Per gas: derivar al 112 primer.
- PLANIFICAT: instal·lació nova, canvi de banyera, problemes crònics

DADES PER A URGÈNCIA:
- Adreça exacta
- Descripció (on fuita, quanta aigua)
- Si han pogut tallar l'entrada general

DADES PER A TREBALL PLANIFICAT:
- Nom i telèfon
- Descripció del treball
- Adreça
- Disponibilitat

TEMPS RESPOSTA:
- Urgent: <2h
- Planificat: cita en 2-5 dies`,

    QUOTE_RULES: `TARIFES ORIENTATIVES:
- Hora d'oficial: 45€/h (mínim 1h)
- Desplaçament: inclòs fins 20km
- Canvi de grifo: 60-100€ (material + mà d'obra)
- Canvi de fluxòmetre WC: 80-120€
- Desbloqueig desguàs: 60-90€
- Instal·lació rentadora/rentavaixelles: 80-100€
- Canvi de banyera per plat de dutxa: 500-800€ (sense obres d'alicatat)
- Urgència fora d'horari: +40%

Materials a part.
Pressupost exacte sempre requereix visita prèvia per a obres.`,

    FOLLOWUP_RULES: `SEGUIMENT POSTVENDA:
- 2 dies després: "Hola [nom]! Funciona tot correctament? Cap nova fuita o problema?"
- Si positiu: demanar ressenya → [URL GOOGLE REVIEWS]

REACTIVACIÓ (sense contacte en +8 mesos):
- "Hola [nom]! Si tens algun problema de lampisteria o vols una revisió preventiva de les instal·lacions, avisa'ns."`,
  },

  JARDINER: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Tipus de servei: manteniment periòdic / disseny i plantació / poda / neteja puntual
- Metres quadrats del jardí aproximats
- Freqüència desitjada (setmanal, quinzenal, mensual) per a manteniment
- Adreça

SERVEIS RECURRENTS:
La majoria de clients fan contracte de manteniment. Per a clients nous, proposa una visita de valoració gratuïta.

COM CONFIRMAR:
- Servei puntual <4h: pots confirmar cita directament
- Contracte de manteniment o projecte de disseny: visita prèvia obligatòria`,

    QUOTE_RULES: `TARIFES ORIENTATIVES:
- Manteniment mensual (fins 200m²): 60-90€/mes
- Manteniment mensual (200-500m²): 90-150€/mes
- Manteniment mensual (+500m²): valoració personalitzada
- Poda d'arbres petits (<3m): 30-50€/unitat
- Poda d'arbres grans (>3m): 80-150€/unitat
- Tala d'arbre: des de 150€ (depèn de mida i accés)
- Neteja puntual post-temporada: m² × 1,5€ (mínim 80€)
- Disseny i plantació: valoració personalitzada

Desplaçament fins 15km: inclòs.`,

    FOLLOWUP_RULES: `SEGUIMENT SERVEI PUNTUAL:
- 3 dies després: "Hola [nom]! Hom ha quedat el jardí? Si necessites qualsevol ajust o tens alguna planta que no va bé, avisa'm."

RECORDATORI ESTACIONAL (primavera i tardor):
- "Hola [nom]! Estem a [estació], bon moment per a [tractament estacional]. Vols que passem a fer la revisió?"

REACTIVACIÓ clients sense servei en +4 mesos:
- "Hola [nom]! Fa un temps que no et veiem. Si el jardí necessita una revisió o vols reprendre el manteniment, estem disponibles."`,
  },

  NETEJA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Tipus: llar / oficina / local comercial / post-obra / mudança
- Metres quadrats
- Nombre d'habitacions i banys (per a llars)
- Freqüència desitjada (puntual, setmanal, quinzenal, mensual)
- Dia i franja horària preferida
- Si calen productes especials (al·lèrgies, superfícies delicades)

CLIENTS RECURRENTS:
Tracta'ls amb màxima atenció. Si canvien de dia o hora, anota-ho i confirma l'ajust.

COM CONFIRMAR:
- Servei puntual: pots confirmar si hi ha disponibilitat
- Contracte recurrent: confirmar i assignar treballador fix si és possible`,

    QUOTE_RULES: `TARIFES ORIENTATIVES:
- Llar fins 60m²: 60-70€ (2h)
- Llar 60-100m²: 80-100€ (2,5-3h)
- Llar +100m²: 100-130€ (3-4h)
- Oficina: m² × 0,8€ (mínim 60€)
- Neteja post-obra (pols de construcció): m² × 3-5€
- Neteja de mudança (pis buit): m² × 2,5€
- Cristalls exteriors: valoració per accés

Productes inclosos. Material especial (alta qualitat orgànic) +15%.
Descompte contracte mensual: -10%. Trimestral: -15%.`,

    FOLLOWUP_RULES: `SEGUIMENT SERVEI PUNTUAL:
- Hores després del servei: "Hola [nom]! Ha anat tot bé amb la neteja? Si hi ha algun detall que vols que repassem, avisa'ns."

SOL·LICITUD RESSENYA:
Si el client expressa satisfacció → [URL GOOGLE REVIEWS]

REACTIVACIÓ clients inactius (+2 mesos):
- "Hola [nom]! Fa temps que no fem servei a casa teva. Tens pendent una neteja a fons? Ara tenim disponibilitat per a [data aproximada]."

OFERTA CLIENTS PUNTUALS per convertir a recurrents:
- "Si vols un servei recurrent (setmanal/mensual), t'oferim el 10% de descompte a partir del segon servei."`,
  },

  FISIOTERAPEUTA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i cognoms
- Telèfon i/o email
- Primera visita o pacient existent
- Motiu consulta (descripció breu: "dolor lumbar", "recuperació operació genoll", etc.)
  IMPORTANT: NO demanar detalls clínics per WhatsApp, únicament el motiu general
- Disponibilitat horària (dies i franges)
- Si té mutualitat o és particular

DURADA DE LES SESSIONS:
- Primera visita (avaluació + tractament): 60 min
- Visita de seguiment: 45 min
- Sessió de tractament (pacients regulars): 30 min

COM CONFIRMAR:
- Pots confirmar cites per als propers 5 dies
- Recorda sempre: "Confirmaré la cita 24h abans per SMS/WhatsApp"

IMPORTANT: Si el pacient descriu dolor molt agut o símptomes neurològics (formigueig, pèrdua de força), indica que visiti urgències o el metge primer.`,

    QUOTE_RULES: `TARIFES:
- Primera visita (60 min): [PREU]€
- Sessió de seguiment (45 min): [PREU]€
- Sessió ràpida (30 min): [PREU]€
- Paquet de 10 sessions: [PREU]€ (estalvi [X]€)
- Domicili (+15 min desplaçament): +[PREU]€/sessió

MUTUALITATS ACCEPTADES: [LLISTAR MUTUALITATS]
Amb mutualitat: [preu amb mutualitat o "cobertura total / parcial"]

IMPORTANT: MAI donis diagnòstics ni plans de tractament per missatgeria. Tota informació clínica, a la cita.`,

    FOLLOWUP_RULES: `RECORDATORI CITA (24h abans):
- "Hola [nom]! Et recordem la teva cita de fisioteràpia demà [dia] a les [hora]. Si no pots venir, avisa'ns amb la màxima antelació possible. Fins demà!"

SEGUIMENT POSTVENDA (2 dies després):
- "Hola [nom]! Com et trobes després de la sessió? Si notes algun canvi o tens dubtes, escriu-nos."

REACTIVACIÓ pacients inactius (+3 mesos):
- "Hola [nom]! Fa temps que no et veiem. Si tens algun dolor o vols reprendre el tractament, estiguem aquí per ajudar-te."`,
  },

  PSICOLEG: {
    BOOKING_RULES: `DADES A RECOLLIR (mínim):
- Nom (o si prefereix, pot usar inicials o pseudònim en un primer moment)
- Telèfon o email
- Primera consulta o seguiment
- Motiu general: "ansietat", "gestió emocional", "problemes de parella", etc.
  IMPORTANT: NO demanar detalls personals per missatgeria

DURADA: Sessions de 50 min (hora terapèutica estàndard)

CONFIDENCIALITAT: Tota comunicació és confidencial. No comparteixis informació entre clients.

CRISIS: Si el client descriu pensaments de fer-se mal o de fer mal a altri, NOTIFICA IMMEDIATAMENT el professional i proporciona el telèfon d'atenció a la crisi: 024

COM CONFIRMAR:
- Confirma la cita de forma discreta i professional
- "Queda confirmada la teva cita per al [dia] a les [hora]. Si necessites canviar-la, avisa'ns amb 24h d'antelació."`,

    FOLLOWUP_RULES: `RECORDATORI CITA (24h abans):
- Missatge discret: "Recordatori cita [dia] a les [hora]. Si necessites canviar-la, avisa'ns avui si pots."

SEGUIMENT: NO fer seguiment proactiu de contingut terapèutic per missatgeria.
Únicament confirmacions de cites i informació administrativa.

REACTIVACIÓ: Per a clients que han interromput el tractament, NO contactar proactivament per motius de salut mental. Únicament si el professional ho indica explícitament.`,
  },

  NUTRICIONISTA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Primera visita o seguiment
- Objectiu general: "pèrdua de pes", "nutrició esportiva", "alimentació saludable", etc.
  IMPORTANT: Tractar els temes de pes i imatge amb màxima sensibilitat
- Disponibilitat horària
- Si té alguna patologia o al·lèrgia alimentària (registrar per al professional, sense aprofundir per WhatsApp)

DURADA:
- Primera visita (historial + pla inicial): 60 min
- Revisió de seguiment: 30 min

COM CONFIRMAR:
- Pots confirmar cites directament
- Recorda: "Et demanem que portis un registre del menjar dels últims 3 dies per a la primera visita."`,

    QUOTE_RULES: `TARIFES:
- Primera visita + pla personalitzat: [PREU]€
- Revisió mensual: [PREU]€
- Paquet inicial (1ª visita + 3 revisions): [PREU]€
- Seguiment online: [PREU]€/mes

IMPORTANT: MAI donis plans d'alimentació específics per WhatsApp. Tota informació nutricional, a la cita.`,

    FOLLOWUP_RULES: `SEGUIMENT ENTRE SESSIONS:
- "Hola [nom]! Com va tot amb el pla? Recorda apuntar el que menges per a la propera revisió. Qualsevol dubte, escriu-me."

RECORDATORI PESADA/MESURES (si aplicable):
- "Hola [nom]! Dimecres tens revisió. Pesa't demà al matí en dejú i anota les mides si tens cinta mètrica."

REACTIVACIÓ (sense visita en +2 mesos):
- "Hola [nom]! Fa un temps que no ens veiem. Si vols reprendre el seguiment o t'has plantejat nous objectius, aquí estem."`,
  },

  PERRUQUERIA: {
    BOOKING_RULES: `DADES A RECOLLIR DE CADA CLIENT:
- Nom i cognoms
- Telèfon o email de contacte
- Servei sol·licitat
- Data i hora preferida (i alternativa si és possible)

DURADA DELS SERVEIS:
- Tall cabell: 30 min
- Coloració completa: 2 hores
- Mecxes: 1,5 hores
- Tractament queratina: 2,5 hores

DISPONIBILITAT:
- Slot de 30 min de dilluns a divendres 9:00-18:30 (darrera cita 18:30)
- Dissabte fins les 13:30
- Màxim 6 cites per dia

COM CONFIRMAR:
- Pots confirmar cites per als propers 3 dies sense consultar.
- Per a cites en més de 3 dies, diu que "us ho confirmem en menys de 2 hores".
- Si la data sol·licitada no té disponibilitat, proposa les 2 opcions més properes.`,

    QUOTE_RULES: `PREUS ESTÀNDARD:
- Tall de cabell dona: des de 25€
- Tall de cabell home: des de 15€
- Coloració completa: des de 45€ (preu exacte depèn del cabell)
- Mecxes: des de 60€
- Tractament queratina: 80€
- Rentat i assecat: 15€

PRESSUPOST PERSONALITZAT:
Per a coloració i tractaments: "El preu exacte depèn del tipus de cabell. Et puc donar un pressupost exacte quan vegis amb nosaltres, però orientativament estaria entre [X€] i [Y€]."`,

    FOLLOWUP_RULES: `SEGUIMENT POSTVENDA:
- 2 dies després: "Hola [nom]! Esperem que hagis quedat content/a. Tens algun dubte o comentari? Estem aquí per ajudar-te 😊"
- Si el client diu que ha quedat bé: demanar ressenya Google → [URL GOOGLE REVIEWS]

RECORDATORI CITA PRÒXIMA (per a clients de color/tractament):
- 6-8 setmanes després: "Hola [nom]! Ja fa un mes i mig des de la teva darrera visita. Quan vols posar la propera cita?"

REACTIVACIÓ clients inactius (+3 mesos):
- "Hola [nom]! Fa un temps que no et veiem! Tens el 15% de descompte en la propera visita si reserves abans de final de mes 🎁"`,

    CANCELLATION_POLICY: `CANCEL·LACIÓ:
- Es pot cancel·lar sense cost fins a 24 hores abans de la cita.
- Cancel·lació amb menys de 24h: 50% del servei (si el client és recurrent, perdona a la primera vegada).
- No-show sense avisar: 100% del servei.

CANVI DE DATA/HORA:
- Es pot canviar sense cost fins a 4 hores abans.

PROCEDIMENT:
- Per cancel·lar: escriure a aquest xat o trucar al [TELÈFON].
- Confirma sempre la recepció: "Hem cancel·lat la teva cita del [data]. Quan vulguis tornar a reservar, avisa'm!"`,
  },

  ESTETICA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Tractament sol·licitat
- Primera vegada o client habitual (alguns tractaments requereixen historial)
- Data i hora preferida

DURADA DELS TRACTAMENTS:
- Facial bàsic: 60 min
- Depilació làser zona petita (llavi, axil·les): 30 min
- Depilació làser zona gran (cames, esquena): 60 min
- Tractament corporal: 90 min
- Manicura: 45 min
- Pedicura: 60 min

IMPORTANT:
- Tractaments làser: preguntar si han pres sol recentment (contraindicació)
- Alguns tractaments necessiten interval entre sessions: informa el client de la freqüència recomanada

COM CONFIRMAR:
- Per als propers 5 dies: confirma directament
- Més de 5 dies: "Ho confirmem en menys de 2 hores"`,

    QUOTE_RULES: `PREUS ORIENTATIUS:
- Facial bàsic: [PREU]€
- Facial anti-edat: [PREU]€
- Depilació làser zona petita: [PREU]€/sessió o [PREU]€ pack 6 sessions
- Depilació làser zona gran: [PREU]€/sessió o [PREU]€ pack 6 sessions
- Tractament corporal reafirmant: [PREU]€

PACK SESSIONS: Per a tractaments làser, sempre recomanar pack (5-10 sessions) → estalvi [X]%.`,

    FOLLOWUP_RULES: `SEGUIMENT POSTVENDA:
- 2 dies després: "Hola [nom]! Com t'ha anat amb el tractament? Algun dubte o algun punt a revisar?"

RECORDATORI PROPERA SESSIÓ (tractaments làser/sèries):
- "Hola [nom]! Ja toca la propera sessió del teu tractament. Quan vols posar la cita?"

REACTIVACIÓ clients inactius (+2 mesos):
- "Hola [nom]! Fa temps que no et veiem. Tenim noves promocions en [tractament]. Vols que et reservem?"`,
  },

  GESTORIA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Tipus de consulta: nou client / tràmit fiscal / laboral / constitució empresa / altra
- Primera cita o tràmit de seguiment
- Disponibilitat horària

TIPUS DE VISITES:
- Cita de valoració (nou client): 30 min
- Tràmit fiscal recurrent: 20 min
- Constitució de societat: 60 min
- Declaració de renda: 30-45 min

COM GESTIONAR DOCUMENTACIÓ:
Per a cites recurrents, pots indicar la documentació necessària:
- Declaració renda: nòmines, certificats bancaris, rebuts deduïbles
- Model 303: factures emeses i rebudes del trimestre
- No demanis documents específics fins que el gestor t'ho confirmi

IMPORTANT: Mai donis consell fiscal o legal específic per WhatsApp.`,

    QUOTE_RULES: `SERVEIS I PREUS:
- Declaració de renda senzilla: [PREU]€
- Declaració de renda complexa (inversions, immobles): [PREU]€
- Model 303 trimestral: [PREU]€/trimestre
- Model 130 autònom: [PREU]€/trimestre
- Alta d'autònom: [PREU]€
- Constitució SL: des de [PREU]€
- Nòmines (servei laboral): [PREU]€/treballador/mes

Per a serveis complexos o empreses, sempre cita prèvia per valoració personalitzada.`,

    FOLLOWUP_RULES: `RECORDATORIS TERMINIS FISCALS (configurables):
- Model 303 Q1: "Hola [nom]! El termini per presentar l'IVA del primer trimestre és el 20 d'abril. Tens preparades les factures? Posa't en contacte amb nosaltres."
- Model 130 autònom: similar, cada trimestre
- Declaració renda: "Hola [nom]! La campanya de la renda comença el [data]. Ens poses en contacte per concertar cita?"

REACTIVACIÓ clients sense gestió en +1 any:
- "Hola [nom]! Ens posem en contacte per si necessites ajuda amb tràmits o tens alguna consulta pendent."`,
  },

  ACADEMIA: {
    BOOKING_RULES: `DADES PER A UNA INSCRIPCIÓ:
- Nom i cognoms
- Telèfon i email
- Curs d'interès
- Nivell actual (per a idiomes o matèries progressives)
- Disponibilitat horària (grups matí / tarda / online)
- Si necessita finançament o beca

GESTIÓ:
- Comprova disponibilitat de places abans de confirmar inscripció
- Per a cursos amb llista d'espera: "Ara mateix el grup [X] està complet, però puc apuntar-te a la llista d'espera. Si s'obre plaça, t'avisem en 24h."

INFORMACIÓ IMPORTANT PER COMPARTIR:
- Data d'inici del curs
- Durada i horari
- Preu i modalitats de pagament
- Material inclòs o a part`,

    QUOTE_RULES: `PREUS DELS CURSOS:
[COMPLETAR PER CADA CURS: nom, durada, preu total, opcions de pagament]

Exemple format:
- Anglès B1 (setembre-juny): [PREU]€/mes o [PREU]€ curs complet (estalvi [X]€)
- Curs intensiu estiu (4 setmanes): [PREU]€
- Classes particulars: [PREU]€/hora

FINANÇAMENT: [Si s'ofereix, indicar modalitats: en X quotes sense interessos, etc.]`,

    FOLLOWUP_RULES: `SEGUIMENT ALUMNES:
- Inici de curs: "Hola [nom]! Demà comença el curs. Tens tot el material? Algun dubte abans de començar?"
- Mig curs: "Hola [nom]! Com vas amb el curs? Si tens algun dubte o necessites ajuda addicional, parla amb el professor o escriu-nos."

RECORDATORI DE PAGAMENT:
- "Hola [nom]! Et recordem que el pagament de [mes] es passa el [data]. Si tens cap problema, contacta'ns."

REACTIVACIÓ (ex-alumnes sense renovació):
- "Hola [nom]! Hem obert la inscripció del nou curs de [matèria]. Com que ja ens coneixes, tindràs prioritat de plaça fins al [data]."`,
  },

  TALLER_MECANIC: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom i telèfon
- Marca, model i any del vehicle
- Matrícula
- Descripció del problema o servei desitjat (revisió, soroll, avaria, ITV, canvi oli, etc.)
- Urgència: pot circular / no pot circular
- Dia i hora preferida per a deixar el vehicle

COM GESTIONAR:
- Si el vehicle NO pot circular: avisar propietari per organitzar grua
- Si el vehicle SÍ pot circular: cita en 1-5 dies laborables
- Temps estimat al taller: indicar sempre que confirmarem un cop diagnosticat

AVÍS QUAN ESTIGUI LLEST:
Quan el vehicle estigui reparat, enviar: "Hola [nom]! El teu [marca model] ja està llest per recollir. L'import total és [X]€. Pots passar quan vulguis en horari d'oficina."`,

    QUOTE_RULES: `TARIFES ORIENTATIVES:
- Hora d'oficial mecànic: [PREU]€/h
- Canvi d'oli + filtre: [PREU]€ (inclou mà d'obra)
- Revisió pre-ITV: [PREU]€
- Canvi de pastilles de fre (eix): [PREU]€ mà d'obra + preu de les pastilles
- Diagnosi amb escàner: [PREU]€

IMPORTANT: Per a qualsevol avaria, el pressupost exacte requereix diagnosi al taller. El cost de la diagnosi és [PREU]€ i es descompta del pressupost si s'aprova la reparació.`,

    FOLLOWUP_RULES: `SEGUIMENT POST-REPARACIÓ:
- 3 dies després: "Hola [nom]! Com va el [marca model]? Tot correctament?"
- Si positiu: demanar ressenya → [URL GOOGLE REVIEWS]

RECORDATORI MANTENIMENT (quan s'acosta la data o km):
- "Hola [nom]! Fa [X] mesos que et vam fer el darrer canvi d'oli. Si ja dus [km]km, és recomanable fer la propera revisió aviat."

REACTIVACIÓ (sense visita en +1 any):
- "Hola [nom]! Fa un any que vas passar per aquí. Si el teu vehicle necessita revisió o ITV, recorda que som el teu taller de confiança."`,
  },

  VETERINARI: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom del propietari i telèfon
- Nom de la mascota, espècie i raça
- Primera visita o pacient existent
- Motiu: revisió anual / vacuna / problema de salut / urgència
- Descripció breu del problema (si escau)

URGÈNCIES VETERINÀRIES — notificar propietari IMMEDIATAMENT:
- Animal inconscient o amb convulsions
- Dificultat respiratòria greu
- Accident de trànsit o traumatisme
- Ingesta de substància tòxica
- Cremades o hemorràgies actives

DURADA:
- Revisió anual: 20 min
- Vacuna sense revisió: 10 min
- Primera visita problema: 30 min
- Urgència: derivar directament al propietari`,

    QUOTE_RULES: `TARIFES ORIENTATIVES:
- Consulta general: [PREU]€
- Vacuna (per vacuna): [PREU]€
- Desparasitació interna: [PREU]€
- Desparasitació externa: [PREU]€
- Anàlisi de sang bàsic: [PREU]€
- Radiografia: des de [PREU]€
- Esterilització gat/gata: [PREU]€
- Esterilització gos/gossa: des de [PREU]€ (depèn del pes)

Preu exacte, especialment per a cirurgies, sempre requereix valoració presencial.`,

    FOLLOWUP_RULES: `RECORDATORI VACUNES (calendari):
- "Hola [nom]! En [data] toca la vacuna anual de [nom mascota]. Vols posar cita?"

SEGUIMENT POST-CONSULTA:
- 2 dies després de tractament: "Hola [nom]! Com es troba [nom mascota]? Ha millorat?"

RECORDATORI DESPARASITACIÓ:
- "Hola [nom]! Ja fa [X] mesos de l'última desparasitació de [nom mascota]. Quan vols passar a per la pròxima dosi?"`,
  },

  PERRUQUERIA_CANINA: {
    BOOKING_RULES: `DADES A RECOLLIR:
- Nom del propietari i telèfon
- Nom, raça i pes aproximat de la mascota
- Serveis desitjats: bany / tall / bany + tall / mani-pedi / tots
- Estat del pelatge: curt i net / llarg i emmadeixat / normal
- Alguna indicació especial del veterinari o comportament a tenir en compte
- Data i hora preferida

TEMPS PER SERVEI (orientatiu per raça gran):
- Bany sol: 1-1,5h
- Bany + tall: 2-3h
- Races petites: -30 min aproximadament

IMPORTANT: Informa el client que el temps pot variar. "T'avisem per WhatsApp quan estigui llest."`,

    QUOTE_RULES: `TARIFES ORIENTATIVES (per mida):
- Races petites (fins 5kg): bany [PREU]€, bany+tall [PREU]€
- Races mitjanes (5-15kg): bany [PREU]€, bany+tall [PREU]€
- Races grans (15-30kg): bany [PREU]€, bany+tall [PREU]€
- Races molt grans (+30kg): des de [PREU]€

Suplements: pelatge enredat o molt llarg +[PREU]€, gat +[PREU]€.

Preu exacte sempre en valorar la mascota en persona (el pelatge pot variar).`,

    FOLLOWUP_RULES: `AVÍS QUAN LA MASCOTA ESTIGUI LLESTA:
- "Hola [nom]! El [nom mascota] ja està llest i molt guapo/a 🐾 Pots passar a recollir-lo/la quan vulguis."

SEGUIMENT:
- 3 dies després: "Hola! Com ha quedat el pelatge de [nom mascota]? Tot correctament?"

RECORDATORI PROPERA VISITA:
- 6-8 setmanes (races que ho necessiten): "Hola [nom]! Ja toca la propera visita de [nom mascota]. Quan vols posar cita?"`,
  },

  RESTAURANTE: {
    BOOKING_RULES: `DADES A RECOLLIR PER A UNA RESERVA:
- Nom
- Telèfon
- Dia i hora
- Nombre de comensals
- Si és per a alguna celebració especial (aniversari, reunió d'empresa, etc.)
- Al·lèrgies o intoleràncies rellevants

COM CONFIRMAR:
- Si hi ha taula disponible: confirma directament
- Si no hi ha disponibilitat: proposa alternatives (altra hora, altra data, llista d'espera)

POLÍTICA: Les reserves per a grups de +8 persones requereixen confirmació del propietari.

EXEMPLE: "Perfecte! Taula reservada per a [N] persones el [data] a les [hora] a nom de [nom]. Si us plau, confirmeu la reserva si hi ha algun canvi. Fins aviat!"`,

    FOLLOWUP_RULES: `SEGUIMENT POST-VISITA:
- 1 dia després: "Hola [nom]! Esperem que hagis gaudit del sopar/dinar. Qualsevol comentari o suggeriment, t'escoltem."
- Si positiu: demanar ressenya → [URL GOOGLE REVIEWS]

REACTIVACIÓ (sense visita en +2 mesos):
- "Hola [nom]! Hem incorporat nous plats a la carta. Quan vols tornar a visitar-nos? 🍽️"`,

    CANCELLATION_POLICY: `CANCEL·LACIÓ DE RESERVES:
- Reserves individuals: cancel·lar amb 2h d'antelació mínima
- Grups de +6 persones: cancel·lar amb 24h d'antelació
- No-show sense avisar per a grups: possibilitat de no acceptar futures reserves

PROCEDIMENT: Escriure a aquest xat o trucar directament al restaurant.`,
  },

  INMOBILIARIA: {
    BOOKING_RULES: `DADES PER A VISITA A UN IMMOBLE:
- Nom i telèfon
- Immoble d'interès (referència o adreça)
- Perfil: comprador / llogater / inversor
- Disponibilitat horària
- Si és primera visita o ja han vist l'immoble

DADES PER A TAXACIÓ / VENDA:
- Adreça de l'immoble
- Tipus: pis / casa / local / solar
- Metres quadrats aproximats
- Estat: a reformar / bon estat / nou

COM GESTIONAR:
- Visites a immobles: pots organitzar directament si el comercial té disponibilitat
- Valoració d'immoble per a venda: sempre cita presencial`,

    QUOTE_RULES: `SERVEIS I COMISSIONS:
- Intermediació venda: [X]% del preu de venda (mínim [PREU]€)
- Intermediació lloguer: [X] mensualitats
- Gestió de lloguer mensual: [PREU]€/mes
- Taxació professional: [PREU]€

IMPORTANT: Mai donis valoracions de preu per WhatsApp sense veure l'immoble. Indica sempre que és "orientatiu" i que la visita definirà el preu real.`,

    FOLLOWUP_RULES: `SEGUIMENT COMPRADORS:
- Setmana 1: "Hola [nom]! Hem rebut nous immobles que podrien interessar-te. T'envio les fitxes?"
- Si han visitat: "Hola [nom]! Alguna impressió sobre l'immoble que vau veure? Qualsevol dubte, estem aquí."

SEGUIMENT VENEDORS:
- Mensual: "Hola [nom]! T'informem de l'activitat del mes: X visites rebudes, X consultes. L'immoble continua en cartera."

REACTIVACIÓ:
- "Hola [nom]! Han canviat les condicions del mercat en la teva zona. Vols una actualització de la valoració del teu immoble?"`,
  },

  AGENCIA_IA: {
    BOOKING_RULES: `DADES PER A UNA REUNIÓ DE DESCOBERTA:
- Nom i empresa
- Email i telèfon
- Descripció breu del negoci (sector, mida)
- Reptes o necessitats principals (automatitzar WhatsApp, gestionar cites, pressupostos, etc.)
- Disponibilitat per a una videotrucada de 30 min

COM CONFIRMAR:
- Pots confirmar la cita per als propers 5 dies
- Envia sempre confirmació per email amb l'enllaç a la reunió (Calendly/Meet)`,

    QUOTE_RULES: `MODEL DE PREUS:
- Setup (implementació inicial): des de 150€ (varia per mida empresa i fases)
- Quota mensual: des de 59€/mes (F1) fins a 129€/mes (F1+F2+F3+F4)

El preu exacte depèn del sector, mida de l'empresa i fases seleccionades.
Sempre: consulta gratuïta → proposta per fases → pressupost en 24h.`,

    FOLLOWUP_RULES: `SEGUIMENT POST-REUNIÓ:
- 1 dia després: "Hola [nom]! Hem preparat una proposta basada en la nostra conversa. La tens a l'email. Qualsevol dubte, estem aquí."

SEGUIMENT PROPOSTA PENDENT (5 dies sense resposta):
- "Hola [nom], volíem saber si has tingut temps de revisar la proposta. Si vols que l'adaptem o tens alguna pregunta, avisa'ns."`,
  },
};

export function getSectorTemplates(sector: string | null | undefined): SectorTemplates {
  if (!sector) return {};
  return T[sector] ?? {};
}
