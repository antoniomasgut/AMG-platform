# Guia d'Allotjament Web — AMG Digitalització

**Versió:** 1.0 · **Data:** Juny 2026  
**Contacte:** hola@amgdl.com · 654 048 164

---

## Índex

1. [Import Bàsic — Web estàtica (HTML/CSS/JS)](#1-import-bàsic--web-estàtica-htmlcssjs)
2. [Import Pro — Web amb contenidors Docker](#2-import-pro--web-amb-contenidors-docker)
3. [Procés de revisió i desplegament](#3-procés-de-revisió-i-desplegament)
4. [Preguntes freqüents](#4-preguntes-freqüents)

---

## 1. Import Bàsic — Web estàtica (HTML/CSS/JS)

### Descripció

Servei per a webs fetes amb HTML, CSS i JavaScript pur (sense servidor). AMG la puja, la serveix via Nginx i la vincula al teu domini amb SSL automàtic.

**Indicat per a:** webs corporatives simples, landings, portfolis, catàlegs sense gestor de continguts.

**No indicat per a:** WordPress, Prestashop, o qualsevol web que requereixi PHP, base de dades o servidor d'aplicació.

### Com preparar el fitxer ZIP

El fitxer a pujar ha de ser un **ZIP** que compleixi els requisits següents:

#### ✅ Requisits obligatoris

| Requisit | Detall |
|----------|--------|
| **`index.html` a l'arrel del ZIP** | El fitxer principal ha d'estar directament al ZIP, no dins d'una carpeta |
| **Mida màxima 50 MB** | Si el teu ZIP és més gran, comprimir les imatges o contacta AMG |
| **Fitxers estàtics únicament** | HTML, CSS, JS, imatges (JPG, PNG, WebP, SVG, GIF), fonts, PDFs |

#### ✅ Estructura correcta

```
web.zip
├── index.html          ← a l'arrel, obligatori
├── about.html
├── contacte.html
├── css/
│   └── style.css
├── js/
│   └── main.js
├── images/
│   ├── logo.png
│   └── hero.jpg
└── fonts/
    └── inter.woff2
```

#### ❌ Estructura incorrecta (causes de rebuig)

```
web.zip
└── carpeta-web/        ← carpeta extra: index.html NO és a l'arrel
    ├── index.html
    └── style.css
```

```
web.zip
├── index.php           ← PHP no acceptat
├── .htaccess           ← no acceptat
└── config.env          ← fitxers de configuració de servidor, no acceptats
```

#### ❌ Fitxers no acceptats

Els fitxers següents seran motiu de rebuig automàtic:

- `.php`, `.php3`, `.php4`, `.phtml` — scripts PHP
- `.htaccess`, `.htpasswd` — configuració Apache
- `.env`, `.env.local` — fitxers d'entorn amb possibles secrets
- `.sh`, `.bash`, `.py`, `.rb`, `.pl` — scripts executables de servidor
- `.exe`, `.dll`, `.so` — executables binaris

### Com crear el ZIP correctament

**En Mac / Linux:**
```bash
# Ves a la carpeta de la teva web
cd /ruta/a/la/teva/web

# Comprimir el contingut (no la carpeta)
zip -r ../web.zip .
```

**En Windows:**
1. Obre la carpeta de la teva web
2. Selecciona **tots els fitxers i carpetes** de dins (Ctrl+A)
3. Clic dret → Comprimir a fitxer ZIP
4. ⚠️ No seleccionis la carpeta en si, sinó el contingut

### Actualitzar la web

Pots actualitzar la teva web en qualsevol moment pujant un nou ZIP. No cal sol·licitar una nova revisió — AMG ha revisat la web inicialment i les actualitzacions es publiquen immediatament sense interrupció del servei.

---

## 2. Import Pro — Web amb contenidors Docker

### Descripció

Servei per a webs complexes amb backend, base de dades o múltiples serveis. La web s'empaqueta amb **Docker Compose** i AMG la revisa, configura i desplega al servidor.

**Indicat per a:** WordPress, Laravel, Symfony, Node.js + base de dades, Prestashop, aplicacions a mida amb backend.

**Requereix:** Fase F5 del pla NexeLocal + revisió manual AMG (termini 48h laborables).

### Fitxers a lliurar

| Fitxer | Obligatori | Descripció |
|--------|-----------|------------|
| `docker-compose.yml` | ✅ Sí | Definició de tots els serveis |
| `.env.example` | Recomanat | Llista de variables d'entorn necessàries (sense valors reals) |
| Descripció del projecte | ✅ Sí | Breu explicació de la web i les seves tecnologies |

---

### Requisits obligatoris del docker-compose.yml

#### ✅ 1. Límits de RAM i CPU en cada servei

Tots els serveis han de declarar els seus límits de recursos. AMG assigna per defecte **512 MB de RAM** i **0.5 CPU** per tenant. Es pot ampliar fins a 2 GB / 2 CPU sota petició i cost addicional.

```yaml
services:
  web:
    image: wordpress:6.5
    mem_limit: 256m      # obligatori
    cpus: "0.5"          # obligatori
```

---

#### ✅ 2. Enrutament del tràfic via Traefik (sense ports exposats)

**No s'han d'exposar ports** (`ports:`) cap a l'exterior. Tot el tràfic web entra per Traefik. Cal afegir les labels correctes al servei que serveix l'aplicació web:

```yaml
services:
  web:
    image: nginx:alpine
    # NO posar: ports: ["80:80"]
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.NOM_UNIC.rule=Host(`el-teu-domini.com`)"
      - "traefik.http.routers.NOM_UNIC.tls=true"
      - "traefik.http.routers.NOM_UNIC.tls.certresolver=letsencrypt"
      - "traefik.http.services.NOM_UNIC.loadbalancer.server.port=80"
```

> **Important:** `NOM_UNIC` ha de ser un identificador sense espais ni punts (p.ex. `maweb`, `client-restaurant`). Ha de ser diferent per a cada servei.

---

#### ✅ 3. Imatges de Docker Hub oficial o GitHub Container Registry

Només s'accepten imatges de fonts conegudes i de confiança:

```yaml
# ✅ Acceptat — Docker Hub oficial
image: wordpress:6.5
image: mysql:8.0
image: nginx:alpine
image: node:20-alpine
image: python:3.12-slim
image: postgres:16-alpine

# ✅ Acceptat — GitHub Container Registry
image: ghcr.io/empresa/app:v1.2.3

# ❌ No acceptat — registres desconeguts
image: registre-privat.exemple.com/app:latest
```

---

#### ✅ 4. Tag de versió concret (no `:latest`)

L'ús de `:latest` no és acceptable perquè no garanteix reproduïbilitat. Sempre especifica una versió concreta:

```yaml
image: wordpress:6.5       ✅
image: mysql:8.0.35        ✅

image: wordpress:latest    ❌
image: mysql               ❌  # sense tag = latest implícit
```

---

#### ✅ 5. Volums nomenats per a la persistència de dades

Les dades persistents (base de dades, fitxers pujats per usuaris) han d'usar volums nomenats. No s'accepten rutes absolutes del servidor host:

```yaml
services:
  db:
    image: mysql:8.0
    volumes:
      - db_data:/var/lib/mysql    ✅ volum nomenat

  app:
    image: wordpress:6.5
    volumes:
      - wp_uploads:/var/www/html/wp-content/uploads    ✅

# Cal declarar els volums al final del compose:
volumes:
  db_data:
  wp_uploads:
```

---

#### ✅ 6. Política de reinici definida

Tots els serveis han d'incloure `restart: unless-stopped` per garantir la disponibilitat després de reinicis del servidor:

```yaml
services:
  web:
    restart: unless-stopped    ✅
  db:
    restart: unless-stopped    ✅
```

---

#### ✅ 7. Variables sensibles via .env.example

Les contrasenyes, claus API i secrets han d'estar parametritzats amb variables d'entorn `${VARIABLE}`. **Mai escriguis valors reals al docker-compose.yml.**

```yaml
services:
  db:
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}    ✅
      MYSQL_PASSWORD: ${DB_PASSWORD}               ✅
      # MAI fer:
      # MYSQL_ROOT_PASSWORD: "la_meva_clau_secreta"  ❌
```

Puja un fitxer `.env.example` amb els noms de totes les variables necessàries, sense els valors reals:

```env
# .env.example — variables necessàries (sense valors)
DB_NAME=nom_de_la_base_de_dades
DB_USER=usuari
DB_PASSWORD=
DB_ROOT_PASSWORD=
APP_SECRET_KEY=
```

AMG configurarà els valors reals de forma segura al servidor.

---

### ❌ Coses que causaran el rebuig automàtic

| Prohibit | Exemple | Motiu |
|----------|---------|-------|
| Mode privilegiat | `privileged: true` | Risc de seguretat crític |
| Muntar el socket de Docker | `- /var/run/docker.sock:/...` | Permet escalar privilegis |
| Muntar directoris del host | `- /etc:/host-etc` | Accés no autoritzat al servidor |
| Exposar ports directament | `ports: ["80:80"]` | Bypass del proxy Traefik |
| Accedir a la xarxa interna AMG | `networks: [amg_net]` | Aïllament obligatori |
| Imatges no verificades | Registres privats desconeguts | Risc de codi maliciós |
| Capacitats de Linux addicionals | `cap_add: [NET_ADMIN]` | Elevació de privilegis |

---

### Exemple complet — WordPress + MySQL

```yaml
# docker-compose.yml
services:

  wordpress:
    image: wordpress:6.5
    restart: unless-stopped
    environment:
      WORDPRESS_DB_HOST: db
      WORDPRESS_DB_NAME: ${DB_NAME}
      WORDPRESS_DB_USER: ${DB_USER}
      WORDPRESS_DB_PASSWORD: ${DB_PASSWORD}
    volumes:
      - wp_content:/var/www/html/wp-content
    networks:
      - app_net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.maweb.rule=Host(`exemple.com`)"
      - "traefik.http.routers.maweb.tls=true"
      - "traefik.http.routers.maweb.tls.certresolver=letsencrypt"
      - "traefik.http.services.maweb.loadbalancer.server.port=80"
    mem_limit: 256m
    cpus: "0.5"
    depends_on:
      - db

  db:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${DB_NAME}
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - app_net
    mem_limit: 256m
    cpus: "0.5"

volumes:
  wp_content:
  db_data:

networks:
  app_net:
```

```env
# .env.example
DB_NAME=wordpress_db
DB_USER=wp_user
DB_PASSWORD=
DB_ROOT_PASSWORD=
```

---

## 3. Procés de revisió i desplegament

### Import Bàsic

```
Client puja el ZIP al portal
       ↓
AMG revisa el contingut (seguretat bàsica) — fins 24h
       ↓
  Aprovat → Web activa al domini  ✅
  Rebutjat → Notificació amb el motiu  ❌
```

### Import Pro

```
Client envia compose + .env.example + descripció
       ↓
AMG revisa el codi (checklist de seguretat) — fins 48h laborables
       ↓
  Rebutjat → Notificació amb els canvis necessaris  ❌
       ↓
  Aprovat → AMG configura variables d'entorn reals
       ↓
  AMG desplega manualment al servidor
       ↓
  Web activa al domini amb SSL automàtic  ✅
       ↓
  Backup nocturn automàtic (volums + dump BD) → GCS
```

---

## 4. Preguntes freqüents

**Puc actualitzar la web sense tornar a demanar revisió?**  
Import Bàsic: sí, pots pujar un nou ZIP en qualsevol moment.  
Import Pro: els canvis al compose requereixen nova revisió. Els canvis de contingut (fitxers, imatges) no.

**Quant tarda en activar-se la web?**  
Import Bàsic: menys de 24 hores des que AMG aprova.  
Import Pro: fins a 48h per a la revisió + temps de desplegament manual (normalment el mateix dia de l'aprovació).

**Puc tenir un domini propi (no d'AMG)?**  
Sí. Hauràs d'apuntar el DNS del teu domini cap al servidor d'AMG. L'equip t'indicarà l'IP i els registres DNS necessaris.

**Què passa amb les dades si cancel·lo el servei?**  
Import Bàsic: pots descarregar el ZIP original en qualsevol moment.  
Import Pro: pots exportar el compose + dump de la base de dades des del portal.  
AMG conserva una còpia de backup durant 30 dies addicionals després de la cancel·lació.

**Té límit de visites o tràfic?**  
No hi ha límit de visites. El límit és de recursos del servidor (RAM / CPU) definits al compose. Per a webs amb molt tràfic, consulta amb AMG per ampliar els recursos.

**El SSL és automàtic?**  
Sí. AMG gestiona els certificats SSL via Let's Encrypt. Es renoven automàticament sense cap gestió per part teva.

---

*Darrera actualització: Juny 2026 — AMG Digitalització · hola@amgdl.com*
