---
name: git-versioning
description: "Gestiona el versionat git del projecte AMG: revisa canvis, crea commits amb missatges convencionals, gestiona branques per funcionalitats, i puja al repositori remot. Activa aquesta skill quan l'usuari vulgui: fer commit, pujar canvis, crear una branca, fer push, veure l'estat dels canvis, preparar una release, o qualsevol acció de versionat. Triggers: 'fes commit', 'puja els canvis', 'git', 'versiona', 'crea branca', 'push', 'quins canvis hi ha', 'guarda els canvis', 'nova branca per', 'release', 'merge', 'pull request', 'PR'."
---

# Git Versioning — AMG Platform

Gestiones el cicle complet de versionat git: anàlisi de canvis → commit semàntic → push. Segueixes les convencions de commits del projecte AMG i protegeixes la branca `main`.

**Regla fonamental: mai forces un push a `main` ni fas `--amend` de commits ja publicats.**

---

## Pas 1 — Analitzar l'estat actual

Executa sempre primer:

```bash
git status
git diff --stat
git log --oneline -5
```

Amb aquesta informació determina:

- **Què ha canviat** — fitxers modificats, nous, esborrats
- **A quina branca estem** — si és `main`, proposa crear branca feature
- **Quants commits de diferència** amb `origin/main`

Si no hi ha cap canvi (`git status` net), informa l'usuari i ofereix:
- Veure l'historial recent
- Crear una branca per treballar
- Fer pull dels canvis remots

---

## Pas 2 — Determinar el tipus de canvi

Analitza els fitxers modificats i classifica automàticament:

| Si els canvis afecten... | Tipus de commit |
|--------------------------|-----------------|
| Funcionalitat nova | `feat` |
| Correcció d'error | `fix` |
| Refactorització sense canvi funcional | `refactor` |
| Documentació (`.md`, `specs/`) | `docs` |
| Tests | `test` |
| Configuració (`.json`, `.yml`, `.env`) | `chore` |
| Estils / CSS | `style` |
| Infra / Docker / CI | `ci` |

### Convencions de commit per mòduls AMG

Si el canvi pertany a un mòdul de la plataforma, afegeix el número entre parèntesis:

```
feat(01-auth): implementa refresh token endpoint
fix(03-leads): corregeix filtre per data de creació
docs(00-arch): actualitza diagrama de components
chore: actualitza .gitignore
```

Si el canvi afecta un **kit** independent:

```
feat(kit-web-scrolling): afegeix animació parallax al hero
fix(kit-dashboard-facturas): corregeix lectura de PDFs amb IVA negatiu
```

---

## Pas 3 — Triar el flux adequat

### Flux A — Canvis petits sobre branca actual

Si l'usuari treballa en una branca feature i els canvis són petits (bugfix, ajust de documentació, millora menor):

1. `git add <fitxers específics>` — mai `git add .` si hi ha fitxers sensibles
2. Proposa el missatge de commit seguint la convenció
3. Confirma amb l'usuari abans de fer el commit
4. `git push`

### Flux B — Nova funcionalitat (recomanat quan estem a `main`)

Si l'usuari vol implementar una funcionalitat nova estant a `main`:

1. Proposa nom de branca: `feat/NN-nom-curt` (ej: `feat/01-auth-jwt`, `feat/kit-seo-mejoras`)
2. `git checkout -b feat/NN-nom-curt`
3. Recorda a l'usuari que ha de fer la implementació en aquesta branca
4. Quan acabi, ofereix crear PR o fer merge

### Flux C — Preparar release / merge a main

Quan la funcionalitat està llesta per merge:

1. Revisa que tots els canvis estan commitats (`git status` net)
2. `git checkout main && git pull`
3. `git merge --no-ff feat/NN-nom` per preservar historial
4. `git push`
5. Elimina la branca feature: `git branch -d feat/NN-nom`

---

## Pas 4 — Executar i confirmar

Abans de qualsevol acció destructiva o irreversible (push, merge, reset), **mostra el comandament exacte i demana confirmació**:

```
Estic a punt de fer:
  git commit -m "feat(01-auth): implementa JWT refresh token"
  git push origin feat/01-auth-jwt

Confirmes? (s/n)
```

Excepcions — executa directament sense confirmació:
- `git status`
- `git log`
- `git diff`
- `git branch`

---

## Pas 5 — Gestió d'errors habituals

### Push rebutjat (remote té canvis nous)
```bash
git pull --rebase origin <branca>
# Si hi ha conflictes, mostra quins fitxers xoquen i demana instruccions
git push
```

### Fitxers sensibles staged per accident
Si detectes `.env`, `*.key`, `*credentials*`, `facturas/*.pdf` a punt de ser commitats sense intenció:
- Avisa l'usuari immediatament
- `git reset HEAD <fitxer>` per treure'ls de l'stage
- Suggereix afegir-los al `.gitignore`

### Branca desincronitzada
```bash
git fetch origin
git status  # mostra quants commits de diferència
```

---

## Pas 6 — Presentar el resultat

Després de cada acció, mostra:

1. **Acció feta** — comandament executat
2. **Estat resultant** — branca actual, commits pendents de push (si n'hi ha)
3. **Proper pas suggerit** — si cal fer PR, si cal continuar implementant, etc.

Format de resum:

```
✅ Commit creat: feat(kit-seo): afegeix puntuació per velocitat de càrrega
📍 Branca: feat/kit-seo-millores  →  1 commit per davant d'origin/main
🔜 Proper pas: git push quan acabis els canvis del mòdul
```

---

## Referència ràpida de comandaments

```bash
# Estat
git status
git log --oneline --graph -10
git diff <fitxer>

# Branques
git checkout -b feat/nom-branca
git checkout main
git branch -a

# Stage selectiu (preferit sobre git add .)
git add <fitxer1> <fitxer2>
git add -p  # interactiu, tria canvis per fragments

# Commit
git commit -m "tipus(àmbit): descripció en present"

# Sincronització
git pull --rebase
git push -u origin <branca>  # primera vegada
git push                      # vegades següents

# Netejar
git branch -d feat/branca-acabada  # local
git push origin --delete feat/branca  # remota
```
