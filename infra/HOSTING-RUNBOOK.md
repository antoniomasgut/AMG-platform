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

## Instal·lació al host (una sola vegada)

### 1. DNS — wildcard de subdominis (ACCIÓ AL REGISTRADOR)
Afegir al proveïdor DNS de `amgdl.com`:
```
*.webs.amgdl.com.   A   65.108.148.62
```
Sense això, els subdominis gratuïts (casos A i B) no resolen.

### 2. coolify-proxy — router wildcard cap al backend (casos A + B)
Afegir un fitxer de config dinàmica al Traefik de Coolify (dir del file-provider, típic
`/data/coolify/proxy/dynamic/`), p. ex. `amg-sites.yaml`:
```yaml
http:
  routers:
    amg-sites:
      rule: "HostRegexp(`{sub:[a-z0-9-]+}.webs.amgdl.com`)"
      entryPoints: [https]
      tls:
        certResolver: letsencrypt
      middlewares: [amg-sites-prefix]
      service: amg-sites
  middlewares:
    amg-sites-prefix:
      addPrefix:
        prefix: "/api/v1/sites/serve"
  services:
    amg-sites:
      loadBalancer:
        servers:
          - url: "http://amg-backend:8080"
```
> `amg-backend` és l'àlies del backend a la xarxa `coolify` (veure docker-compose.yml).
> Els imports CONTAINER (cas C) NO passen per aquest router: el seu contenidor porta el
> seu propi label `Host(...)` amb més prioritat.

### 3. Agent de reconciliació (cas C)
```
cp /opt/amg/infra/scripts/hosting-reconciler-agent.py /opt/amg/infra/scripts/
cat >/etc/cron.d/amg-hosting-agent <<'EOF'
*/2 * * * * root /usr/bin/python3 /opt/amg/infra/scripts/hosting-reconciler-agent.py >> /var/log/amg-hosting-agent.log 2>&1
EOF
```
No cal secret: l'agent només llegeix un directori del volum i crea contenidors nginx amb
noms/dominis validats (regex) — no executa res del manifest com a shell.

## Verificació
- **A**: publica una landing amb slug `demo` → `https://demo.webs.amgdl.com` mostra la landing.
- **B**: puja un ZIP amb domini `demo.webs.amgdl.com` i aprova → serveix `index.html`.
- **C**: aprova un import CONTAINER → en <2 min l'agent crea `site-*-pro-proxy`, i el domini respon.

## Notes de seguretat
- El backend no té accés a Docker (socket eliminat). Superfície d'atac reduïda.
- L'agent valida noms de contenidor (`[a-zA-Z0-9._-]`) i dominis (`[a-zA-Z0-9.-]`) i rebutja
  `..` a les rutes → cap injecció d'arguments a `docker run`.
- El servei estàtic té guarda anti path-traversal (el fitxer resolt ha d'estar dins del
  directori `html/` del tenant).
