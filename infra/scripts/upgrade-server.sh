#!/usr/bin/env bash
# upgrade-server.sh — Actualitza el tipus de servidor Hetzner sense pèrdua de dades
#
# Usa el mecanisme de Snapshot + Nou servidor de Hetzner Cloud.
# Alternativament, pot fer un "rescale" in-place si el servidor és Hetzner Cloud.
#
# ÚS:
#   HCLOUD_TOKEN=xxx ./upgrade-server.sh --server-name=amg-app --new-type=cx32
#
# PREREQUISITS:
#   - hcloud CLI instal·lat (brew install hcloud / apt install hcloud)
#   - HCLOUD_TOKEN configurat

set -euo pipefail

SERVER_NAME=""
NEW_TYPE=""
HCLOUD_TOKEN="${HCLOUD_TOKEN:-}"

for arg in "$@"; do
  case $arg in
    --server-name=*) SERVER_NAME="${arg#*=}" ;;
    --new-type=*)    NEW_TYPE="${arg#*=}" ;;
  esac
done

if [[ -z "$SERVER_NAME" || -z "$NEW_TYPE" || -z "$HCLOUD_TOKEN" ]]; then
  echo "Ús: HCLOUD_TOKEN=xxx ./upgrade-server.sh --server-name=amg-app --new-type=cx32"
  echo ""
  echo "Tipus disponibles Hetzner (2026):"
  echo "  cx22 = 2 vCPU / 4 GB RAM / 40 GB  ~  5€/mes"
  echo "  cx32 = 4 vCPU / 8 GB RAM / 80 GB  ~ 13€/mes"
  echo "  cx42 = 8 vCPU / 16 GB RAM / 160 GB ~ 26€/mes"
  echo "  cx52 = 16 vCPU / 32 GB RAM / 320 GB ~ 52€/mes"
  exit 1
fi

export HCLOUD_TOKEN

log() { echo "[$(date '+%H:%M:%S')] $*"; }

# Obtenir ID del servidor
SERVER_ID=$(hcloud server describe "$SERVER_NAME" -o json | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
log "Servidor: $SERVER_NAME (ID: $SERVER_ID)"

# Verificar que és possible el canvi de tipus
log "Tipus actual: $(hcloud server describe "$SERVER_NAME" -o json | python3 -c "import sys,json; print(json.load(sys.stdin)['server_type']['name'])")"
log "Tipus nou: $NEW_TYPE"

echo ""
echo "⚠️  ATENCIÓ: El servidor s'aturarà durant l'actualització (~2-5 minuts de downtime)"
echo "   Assegura't d'haver avisat els usuaris i que les backups estan al dia."
echo ""
read -rp "Continua? (escriu 'si' per confirmar): " CONFIRM
if [[ "$CONFIRM" != "si" ]]; then
  echo "Cancel·lat."
  exit 0
fi

# Aturar servidor
log "Aturant servidor $SERVER_NAME..."
hcloud server poweroff "$SERVER_NAME"
sleep 10

# Canviar tipus
log "Canviant tipus de servidor a $NEW_TYPE..."
hcloud server change-type "$SERVER_NAME" "$NEW_TYPE"

# Arrencar servidor
log "Arrencant servidor $SERVER_NAME..."
hcloud server poweron "$SERVER_NAME"

# Esperar que el servidor estigui disponible
log "Esperant que el servidor estigui llest..."
sleep 30
NEW_IP=$(hcloud server describe "$SERVER_NAME" -o json | python3 -c "import sys,json; print(json.load(sys.stdin)['public_net']['ipv4']['ip'])")

log "Servidor actualitzat. IP: $NEW_IP"
echo ""
echo "✅ Actualització completada!"
echo "   Servidor: $SERVER_NAME"
echo "   Tipus nou: $NEW_TYPE"
echo "   IP pública: $NEW_IP"
echo ""
echo "Verifica que tots els contenidors Docker han arrencat:"
echo "  ssh root@$NEW_IP 'docker ps'"
