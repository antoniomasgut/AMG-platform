# Fisioterapeuta / Clínica de fisioteràpia

## Prospecting

### Email fred

**Assumpte:** Pacients nous a {CIUTAT} per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Soc {TEU_NOM} de {EMPRESA}. Ajudeu clíniques de fisioteràpia a {CIUTAT} a captar pacients nous sense invertir en anuncis cars.

Com funcionem: un assistent virtual per WhatsApp que respon dubtes sobre tractaments, preus i agenda cites automàticament. El pacient parla amb el bot, queda satisfet i reserva.

Una clínica a {CIUTAT_REF} va passar de 5 a 15 nous pacients al mes en 60 dies.

Vols que t'ho expliqui en 5 minuts?

{TEU_NOM}

---

**To:** Professional, càlid

### Seguiment

**Assumpte:** {NOM_NEGOCI} — reduir no-shows

Hola {NOM_CONTACTE},

A part de captar pacients, el nostre sistema recorda les cites automàticament per WhatsApp. Resultat: -80% no-shows.

Vols que t'ensenyi com funciona?

{TEU_NOM}

---

## Meta Ads

**Estructura:**

| Nivell | % | Audiència |
|--------|---|-----------|
| Captació | 60% | Radi {RADI}km, 25-65, interessos: fisioteràpia, salut, benestar |
| Retargeting | 30% | Visitants web |
| Notorietat | 10% | Radi ampli |

### Creativitats:

**Problema específic:**
- Text: "Mal d'esquena que no se'n va? Prova una sessió de prova per {PREU_PROVA}€"
- Imatge: Persona amb molèstia + text "Sessió prova {PREU_PROVA}€"

**Abans/després:**
- Text: "3 mesos de rehabilitació. De no poder caminar a tornar a córrer."
- Imatge: Pacient real (amb permís)

**Vídeo testimoni:**
- Pacient explicant millora + fisio comentant

---

## Agent IA (Prompt)

```
Ets l'assistent virtual de {NOM_NEGOCI}, una clínica de fisioteràpia a {CIUTAT}.
Respons en català o castellà, de forma breu, professional i càlida.

SERVEIS: {SERVEIS}

ROL PRINCIPAL:
1. Gestionar cites de pacients nous i de seguiment
2. Explicar tractaments i el seu funcionament
3. Informar de preus i cobertura d'assegurances
4. Fer seguiment post-tractament
5. Gestionar cancel·lacions i canvis

PER A CITA NOVA:
- Nom complet del pacient
- Motiu de consulta o zona afectada
- Té assegurança mèdica? (quina?)
- Disponibilitat
- Primera visita o seguiment?

IMPORTANT: Mai donis diagnòstics ni consells mèdics específics.
Sempre derivar al fisioterapeuta per a valoració.

HORARI: {HORARI}
```
