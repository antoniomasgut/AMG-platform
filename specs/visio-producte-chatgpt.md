# Visió de producte — Conversa ChatGPT

> Document de referència per a la planificació del producte NexeLocal.
> Estructura: nucli comú (5 mòduls) + packs específics per sector.

---

## Nucli comú (tots els sectors)

### 1. Captació
**Objectiu:** No perdre oportunitats.

**Funcions:**
- Formularis web
- WhatsApp
- Email
- Classificació automàtica
- Creació de fitxa

**Missatge comercial:** *"Totes les sol·licituds queden registrades automàticament."*

---

### 2. Seguiment
**Objectiu:** Que ningú es quedi sense resposta.

**Funcions:**
- Recordatoris interns
- Missatges automàtics
- Seguiment programat
- Detecció de clients oblidats

**Missatge:** *"Mai oblidis tornar a contactar un client."*

---

### 3. Agenda
**Objectiu:** Reduir feina administrativa.

**Funcions:**
- Confirmacions
- Reprogramacions
- Recordatoris
- Gestió d'absències

**Missatge:** *"Menys trucades i menys cites perdudes."*

---

### 4. Comunicació
**Objectiu:** Centralitzar converses.

**Funcions:**
- WhatsApp
- Email
- Notes internes
- Resums IA

**Missatge:** *"Tota la informació de cada client en un únic lloc."*

---

### 5. Alertes
**Objectiu:** Que el negoci no depengui de la memòria.

**Funcions:**
- Leads sense resposta
- Cites pendents
- Pressupostos oblidats
- Pagaments pendents

**Missatge:** *"T'avisem abans que hi hagi un problema."*

---

## Pack Clíniques (fisioterapeutes, ostèopates, etc.)

### 6. Pacients
**Funcions:**
- Historial de visites
- Seguiments
- Recordatoris de revisió
- Recuperació de pacients inactius

**Exemple:** *"Fa 6 mesos que en Joan no torna. Vols enviar-li un recordatori?"*

### 7. Ressenyes
**Funcions:**
- Sol·licitud automàtica
- Seguiment
- Estadístiques

---

## Pack Oficis (pintors, electricistes, fusters, etc.)

### 6. Pressupostos
**Funcions:**
- Registre
- Seguiment
- Recordatoris
- Conversió

**Exemple:** *"Hi ha 8 pressupostos sense resposta des de fa 10 dies."*

### 7. Cobraments
**Funcions:**
- Avisos
- Recordatoris
- Seguiment de factures

---

## Mòduls futurs (roadmap)

| Mòdul | Funció |
|-------|--------|
| **Documents** | Llegir PDFs, extreure dades, crear fitxes |
| **Informes** | Resum diari/setmanal: nous clients, pressupostos, cites, incidències |
| **Analítica** | Conversió, temps de resposta, clients recuperats, valor generat |

---

## MVP mínim recomanat

Començar amb 5 mòduls:

1. Captació
2. Seguiment
3. Agenda
4. Comunicació
5. Alertes

Després activar packs per sector:

- **Pack Clíniques** → Pacients + Ressenyes
- **Pack Oficis** → Pressupostos + Cobraments

---

## Correspondència amb fases NexeLocal

| Mòdul ChatGPT | Fase NexeLocal | Estat |
|---------------|----------------|-------|
| Captació | F1 — Captació | ✅ Implementat |
| Seguiment | F4 — Seguiment | ✅ Implementat |
| Agenda | F2 — Agenda | ✅ Implementat |
| Comunicació | F1 + Inbox omnicanal | ✅ Implementat (Mòdul 25) |
| Alertes | F5 — Alertes & Equip | ✅ Implementat |
| Pacients (Clíniques) | F4 — Seguiment + Historial de Visites | ✅ Implementat (VisitRecord entity + CRUD + UI) |
| Ressenyes (Clíniques) | F4 — Seguiment (ReviewRequestScheduler) | ✅ Implementat |
| Pressupostos (Oficis) | F3 — Pressupostos | ✅ Implementat |
| Cobraments (Oficis) | Mòdul 07 Billing + GoCardless | ✅ Implementat |
| Documents | DocumentService + DocumentController | ✅ Implementat (PDF/txt → IA extracció → lead) |
| Informes | ReportScheduler (diari 08h + setmanal dill.) | ✅ Implementat (Telegram automàtic) |
| Analítica | AnalyticsService + AnalyticsController + UI | ✅ Implementat (/portal/analytics) |
