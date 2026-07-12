# Hosting de landings i imports — arquitectura segura

Reconstruït el 2026-07-12 perquè el backend **no toqui mai Docker** (abans muntava
`/var/run/docker.sock`, root-al-host). Tres casos, tots encaminats per **coolify-proxy**
(l'únic Traefik amb els ports 80/443):

| Cas | Contenidor? | Com |
|-----|-------------|-----|
| **A. Landing del motor** (subdomini gratuït `client.webs.amgdl.com`) | ❌ | El backend la renderitza pel Host (`PublicSiteController`) |
| **B. Import estàtic** (html/css/js, ZIP) | ❌ | El backend serveix els fitxers del volum pel Host |
| **C. Import dins contenidor** (docker-compose del client) | ✅ | El backend escriu un manifest → l'**agent del host** crea el proxy nginx |

El backend només escriu fitxers al volum `websites_data`. Cap `docker run` des del backend.

## Flux de dades (cas C)

```
Portal (admin aprova import) → WebHostingService.deployContainerSite()
  → HostingManifestService escriu  websites_data/_desired/<name>.json  + la conf nginx
        ↓ (cron, al host)
  hosting-reconciler-agent.py llegeix _desired/*.json
     docker run nginx (proxy) --network coolify + labels Host(domini) → connect amg_net
        ↓
  coolify-proxy encamina el domini → proxy → contenidor upstream del client
```

## Routing automàtic (com funciona, ja instal·lat)

El backend **no escriu mai** la config de coolify-proxy. En publicar/despublicar una
landing o desplegar/esborrar un estàtic, escriu un manifest a `websites_data/_desired/`:
- `route-<domini>.json` → l'agent genera un router concret a `amg-sites-auto.yml`.
- `<container>.json` (kind container-proxy) → l'agent crea el contenidor (cas C).

L'agent (`/etc/cron.d/amg-hosting-agent`, cada 2 min) valida el domini
(**anti-hijacking**: només `*.webs.amgdl.com` o domini propi que NO sigui de `amgdl.com`)
i regenera `amg-sites-auto.yml` de forma atòmica. Traefik (file-provider) recarrega en calent
i obté el cert HTTP-01 per cada `Host(...)` concret.

### Estat de la infraestructura (2026-07-12)
- **DNS wildcard `*.webs.amgdl.com` → 65.108.148.62**: ✅ ja existeix al proveïdor.
- **Agent + cron**: ✅ instal·lat (`/opt/amg/infra/scripts/hosting-reconciler-agent.py`).
- **`amg.yml`** (coolify dynamic): només routers core (amgdl.com, api.amgdl.com). Els routers
  de landing es generen a `amg-sites-auto.yml` (gestionat per l'agent — no editar a mà).
- **Permisos del volum**: `/var/lib/docker/volumes/infra_websites_data/_data` ha de ser
  propietat de l'usuari del backend (**uid 100:101 / amg**), altrament el backend no pot
  escriure els manifests. Aplicat amb:
  ```
  chown -R 100:101 /var/lib/docker/volumes/infra_websites_data/_data && chmod 775 ...
  ```
  Persisteix (volum amb nom). Si es recrea el volum, tornar-ho a aplicar.

## Verificació
- **A** (verificat ✅): `estetica-mireia|fisio-llevant|nutricio-sa-salut.webs.amgdl.com` → 200.
  Publica una landing nova → en <2 min el seu subdomini respon (cert automàtic).
- **B**: puja un ZIP amb domini `x.webs.amgdl.com` i aprova → l'agent crea el router amb
  prefix, i el backend serveix `index.html` des del volum.
- **C**: aprova un import CONTAINER → en <2 min l'agent crea `site-*-pro-proxy` a la xarxa
  coolify, i el domini respon.

## Notes de seguretat
- El backend no té accés a Docker (socket eliminat). Superfície d'atac reduïda.
- L'agent valida noms de contenidor (`[a-zA-Z0-9._-]`) i dominis (`[a-zA-Z0-9.-]`) i rebutja
  `..` a les rutes → cap injecció d'arguments a `docker run`.
- El servei estàtic té guarda anti path-traversal (el fitxer resolt ha d'estar dins del
  directori `html/` del tenant).
