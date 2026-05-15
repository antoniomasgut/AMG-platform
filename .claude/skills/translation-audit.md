# Skill: translation-audit

Audita tots els fitxers de missatges i18n del projecte per detectar:

1. **Claus que falten** — claus presents a `ca.json` (master) que no existeixen en `es`, `en` o `de`
2. **Claus extra** — claus en traduccions que no existeixen al master
3. **Valors sense traduir** — text idèntic al català en un idioma diferent (quan hauria d'estar traduït)
4. **Errors semàntics / de context** — frases que no encaixen amb la filosofia del projecte o el to de la marca
5. **Errors ortogràfics o gramaticals** — errors en qualsevol idioma
6. **Inconsistències de to** — mescla de tractament formal/informal dins un idioma

## Passos

1. Llegeix `frontend/messages/ca.json` (master)
2. Llegeix `frontend/messages/es.json`, `en.json`, `de.json`
3. Compara claus recursivament (aplana el JSON i compara els paths)
4. Revisa el contingut semàntic i lingüístic de cadascun
5. Genera un informe estructurat amb:
   - ✅ Seccions correctes
   - ⚠️ Advertències (possible error)
   - ❌ Errors (clau que falta, text sense traduir, error clar)

## Context del projecte

- **Marca:** AMG Digitalitzacions — enginyeria digital per a pimes de Mallorca
- **To:** professional però proper, directe, sense excessiva formalitat
- **Filosofia:** no hi ha preus fixos, treballem per fases, manteniment + backups + incidències inclosos
- **Audiència:** propietaris de pimes locals, no perfils tècnics
- **Idioma master:** català (`ca`)
- **Tractament:** 
  - CA: informal (tu/teu)
  - ES: informal (tú/tu)  
  - EN: neutral (you/your)
  - DE: formal (Sie/Ihr) — Mallorca té molts turistes alemanys que esperen formalitat

Executa l'auditoria i presenta l'informe directament, sense crear fitxers intermedis.
