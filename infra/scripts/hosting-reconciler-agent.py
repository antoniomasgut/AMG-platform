#!/usr/bin/env python3
"""
Agent de reconciliació de hosting — corre al HOST via cron.

El backend NO toca mai Docker. Escriu manifestos d'estat desitjat a
{volum websites_data}/_desired/*.json i aquest agent els reconcilia:
crea/elimina els contenidors proxy dels imports de tipus CONTAINER, a la
xarxa `coolify` i amb labels Traefik perquè coolify-proxy els encamini.

Manifest (deploy):
  {"action":"deploy","kind":"container-proxy","containerName":"...",
   "domain":"client.webs.amgdl.com","memory":"32m",
   "confSubPath":"{tenant}/proxy/nginx-proxy.conf"}
Manifest (remove):
  {"action":"remove","containerName":"..."}

Instal·lació (host):
  */2 * * * * root /usr/bin/python3 /opt/amg/infra/scripts/hosting-reconciler-agent.py >> /var/log/amg-hosting-agent.log 2>&1
"""
import json
import os
import re
import subprocess
import sys

VOLUME = os.environ.get("HOSTING_VOLUME", "infra_websites_data")
COOLIFY_NET = os.environ.get("COOLIFY_NETWORK", "coolify")
UPSTREAM_NET = os.environ.get("UPSTREAM_NETWORK", "amg_net")
COOLIFY_DYNAMIC_DIR = os.environ.get("COOLIFY_DYNAMIC_DIR", "/data/coolify/proxy/dynamic")
BACKEND_URL = os.environ.get("BACKEND_URL", "http://amg-backend:8080")
STATIC_PREFIX = "/api/v1/sites/serve"
NGINX_IMAGE = "nginx:alpine"
SAFE = re.compile(r"^[a-zA-Z0-9._-]+$")
DOMAIN_RE = re.compile(r"^[a-zA-Z0-9.-]+$")
BASE_DOMAIN = os.environ.get("LANDING_BASE_DOMAIN", "webs.amgdl.com")
# Hosts interns que un site MAI pot reclamar (anti-hijacking del routing)
RESERVED_HOSTS = {"amgdl.com", "www.amgdl.com", "api.amgdl.com",
                  "storage.amgdl.com", "minio.amgdl.com", "monitor.amgdl.com"}


def domain_allowed(domain):
    """Un site només pot reclamar un subdomini de webs.amgdl.com o un domini propi
    que NO sigui de amgdl.com (evita segrestar api/amgdl/etc.)."""
    if not DOMAIN_RE.match(domain) or ".." in domain or domain in RESERVED_HOSTS:
        return False
    if domain.endswith("." + BASE_DOMAIN):
        return domain != BASE_DOMAIN
    # Domini propi del client: no pot ser cap host del nostre domini principal
    return not domain.endswith(".amgdl.com") and "." in domain


def run(cmd, check=True):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if check and r.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd)} -> {r.returncode}: {r.stderr.strip()}")
    return r


def volume_mountpoint():
    r = run(["docker", "volume", "inspect", VOLUME, "-f", "{{.Mountpoint}}"], check=False)
    if r.returncode == 0 and r.stdout.strip():
        return r.stdout.strip()
    return f"/var/lib/docker/volumes/{VOLUME}/_data"


def container_running(name):
    r = run(["docker", "inspect", "-f", "{{.State.Running}}", name], check=False)
    return r.returncode == 0 and r.stdout.strip() == "true"


def container_exists(name):
    r = run(["docker", "inspect", name], check=False)
    return r.returncode == 0


def remove_container(name):
    if container_exists(name):
        run(["docker", "rm", "-f", name], check=False)
        print(f"[reconciler] eliminat {name}")


def deploy_proxy(m, vol):
    name = m["containerName"]
    domain = m["domain"]
    conf_sub = m["confSubPath"]
    # Validacions: noms i dominis controlats (no injecció d'arguments)
    if not SAFE.match(name) or not DOMAIN_RE.match(domain) or ".." in conf_sub:
        print(f"[reconciler] manifest invàlid, ignorat: {name}", file=sys.stderr)
        return
    conf_host = os.path.normpath(os.path.join(vol, conf_sub))
    if not conf_host.startswith(vol) or not os.path.isfile(conf_host):
        print(f"[reconciler] conf no trobada per {name}: {conf_host}", file=sys.stderr)
        return
    if container_running(name):
        return  # ja desplegat
    remove_container(name)  # recrea si existeix aturat

    router = re.sub(r"[^a-zA-Z0-9]", "-", domain)
    memory = m.get("memory", "32m")
    cmd = [
        "docker", "run", "-d",
        "--name", name,
        "--network", COOLIFY_NET,
        "--memory", memory,
        "--restart", "unless-stopped",
        "-v", f"{conf_host}:/etc/nginx/conf.d/default.conf:ro",
        "--label", "traefik.enable=true",
        "--label", f"traefik.docker.network={COOLIFY_NET}",
        "--label", f"traefik.http.routers.{router}.rule=Host(`{domain}`)",
        "--label", f"traefik.http.routers.{router}.tls=true",
        "--label", f"traefik.http.routers.{router}.tls.certresolver=letsencrypt",
        "--label", f"traefik.http.services.{router}.loadbalancer.server.port=80",
        "--label", "amg.type=container-site-proxy",
        NGINX_IMAGE,
    ]
    run(cmd)
    # Connecta també a la xarxa de l'upstream del client perquè el proxy_pass el trobi
    run(["docker", "network", "connect", UPSTREAM_NET, name], check=False)
    print(f"[reconciler] desplegat {name} → {domain}")


def render_routes_yaml(routes):
    """Genera el fitxer dinàmic de coolify-proxy amb un router concret per site.
    Landing → backend a l'arrel; estàtic → backend amb addPrefix /api/v1/sites/serve."""
    lines = ["http:", "  routers:"]
    for domain, kind in sorted(routes.items()):
        router = "amgauto-" + re.sub(r"[^a-zA-Z0-9]", "-", domain)
        mids = ["amgauto-static-prefix"] if kind == "static" else []
        lines += [
            f"    {router}:",
            f"      rule: \"Host(`{domain}`)\"",
            "      entryPoints: [https]",
            "      tls:",
            "        certResolver: letsencrypt",
            "      service: amgauto-backend",
        ]
        if mids:
            lines.append("      middlewares: [" + ", ".join(mids) + "]")
        lines += [
            f"    {router}-http:",
            f"      rule: \"Host(`{domain}`)\"",
            "      entryPoints: [http]",
            "      middlewares: [amgauto-redirect" + ("".join(", " + x for x in mids)) + "]",
            "      service: amgauto-backend",
        ]
    lines += [
        "  services:",
        "    amgauto-backend:",
        "      loadBalancer:",
        "        servers:",
        f"          - url: \"{BACKEND_URL}\"",
        "  middlewares:",
        "    amgauto-redirect:",
        "      redirectScheme:",
        "        scheme: https",
        "        permanent: true",
        "    amgauto-static-prefix:",
        "      addPrefix:",
        f"        prefix: \"{STATIC_PREFIX}\"",
        "",
    ]
    return "\n".join(lines)


def reconcile_routes(desired):
    """Recull rutes deploy dels manifests route-*.json i regenera el fitxer dinàmic."""
    routes = {}
    for fn in sorted(os.listdir(desired)):
        if not fn.startswith("route-") or not fn.endswith(".json"):
            continue
        path = os.path.join(desired, fn)
        try:
            with open(path) as f:
                m = json.load(f)
        except Exception:
            continue
        domain = m.get("domain", "")
        if m.get("action") == "remove":
            os.remove(path)  # consumit → exclòs de la regeneració
            continue
        if not domain_allowed(domain):
            print(f"[reconciler] ruta rebutjada (domini no permès): {domain}", file=sys.stderr)
            continue
        routes[domain] = "static" if m.get("routeKind") == "static" else "landing"

    out = os.path.join(COOLIFY_DYNAMIC_DIR, "amg-sites-auto.yml")
    yaml = render_routes_yaml(routes)
    try:
        tmp = out + ".tmp"
        with open(tmp, "w") as f:
            f.write(yaml)
        os.replace(tmp, out)  # escriptura atòmica; Traefik file-provider recarrega
    except Exception as e:
        print(f"[reconciler] no s'ha pogut escriure {out}: {e}", file=sys.stderr)


def main():
    vol = volume_mountpoint()
    desired = os.path.join(vol, "_desired")
    if not os.path.isdir(desired):
        return
    # Contenidors (cas C)
    for fn in sorted(os.listdir(desired)):
        if not fn.endswith(".json") or fn.startswith("route-"):
            continue
        path = os.path.join(desired, fn)
        try:
            with open(path) as f:
                m = json.load(f)
        except Exception as e:
            print(f"[reconciler] manifest il·legible {fn}: {e}", file=sys.stderr)
            continue
        action = m.get("action")
        name = m.get("containerName", "")
        if not SAFE.match(name):
            continue
        try:
            if action == "remove":
                remove_container(name)
                os.remove(path)  # consumit
            elif action == "deploy" and m.get("kind") == "container-proxy":
                deploy_proxy(m, vol)
        except Exception as e:
            print(f"[reconciler] error processant {fn}: {e}", file=sys.stderr)
    # Rutes de sites servits pel backend (casos A + B)
    try:
        reconcile_routes(desired)
    except Exception as e:
        print(f"[reconciler] error reconciliant rutes: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
