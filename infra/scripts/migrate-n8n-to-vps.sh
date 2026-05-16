#!/usr/bin/env bash
# migrate-n8n-to-vps.sh — Migra n8n del servidor actual a un VPS dedicat
#
# ÚS:
#   ./migrate-n8n-to-vps.sh --source-host=ACTUAL_VPS_IP --dest-host=NEW_N8N_VPS_IP
#
# FLUX:
#   1. Exportar tots els workflows de n8n (via CLI)
#   2. Exportar credencials (encriptades)
#   3. Copiar al servidor destí
#   4. Arrancar n8n al servidor destí
#   5. Importar workflows i credencials
#   6. Verificar i mostrar instruccions

set -euo pipefail

SOURCE_HOST=""
DEST_HOST=""
SSH_USER="${SSH_USER:-root}"
N8N_DOMAIN="${N8N_DOMAIN:-n8n.portal.amg.cat}"
N8N_ENCRYPTION_KEY="${N8N_ENCRYPTION_KEY:-}"
BACKUP_DIR="/tmp/n8n_migration_$(date +%Y%m%d_%H%M%S)"

for arg in "$@"; do
  case $arg in
    --source-host=*) SOURCE_HOST="${arg#*=}" ;;
    --dest-host=*)   DEST_HOST="${arg#*=}" ;;
    --ssh-user=*)    SSH_USER="${arg#*=}" ;;
    --n8n-domain=*)  N8N_DOMAIN="${arg#*=}" ;;
  esac
done

if [[ -z "$SOURCE_HOST" || -z "$DEST_HOST" ]]; then
  echo "Ús: ./migrate-n8n-to-vps.sh --source-host=1.2.3.4 --dest-host=5.6.7.8"
  exit 1
fi

log() { echo "[$(date '+%H:%M:%S')] $*"; }

# ── Pas 1: Exportar workflows ──────────────────────────────────────────────────
log "1/5 Exportant workflows de n8n..."
ssh "${SSH_USER}@${SOURCE_HOST}" bash <<EOF
  mkdir -p ${BACKUP_DIR}
  docker exec amg_n8n n8n export:workflow --all --output=${BACKUP_DIR}/workflows.json 2>/dev/null || \
    docker exec amg_n8n n8n export:workflow --all --output=/tmp/workflows.json && \
    docker cp amg_n8n:/tmp/workflows.json ${BACKUP_DIR}/workflows.json
  echo "Workflows exportats"
EOF

# ── Pas 2: Exportar credencials ────────────────────────────────────────────────
log "2/5 Exportant credencials (encriptades)..."
ssh "${SSH_USER}@${SOURCE_HOST}" bash <<EOF
  docker exec amg_n8n n8n export:credentials --all --output=/tmp/credentials.json 2>/dev/null && \
    docker cp amg_n8n:/tmp/credentials.json ${BACKUP_DIR}/credentials.json || \
    echo "Avís: no s'han pogut exportar credencials automàticament"
  tar czf ${BACKUP_DIR}.tar.gz -C $(dirname ${BACKUP_DIR}) $(basename ${BACKUP_DIR})
  echo "Backup comprimit"
EOF

# ── Pas 3: Copiar al servidor destí ───────────────────────────────────────────
log "3/5 Copiant backup al servidor destí..."
ssh "${SSH_USER}@${SOURCE_HOST}" \
  "scp ${BACKUP_DIR}.tar.gz ${SSH_USER}@${DEST_HOST}:/tmp/"

ssh "${SSH_USER}@${DEST_HOST}" \
  "cd /tmp && tar xzf $(basename ${BACKUP_DIR}).tar.gz"

# ── Pas 4: Arrancar n8n al servidor destí ─────────────────────────────────────
log "4/5 Arrancant n8n al servidor destí..."
ssh "${SSH_USER}@${DEST_HOST}" bash <<EOF
  docker run -d \
    --name amg_n8n \
    --restart unless-stopped \
    -p 5678:5678 \
    -e N8N_HOST=${N8N_DOMAIN} \
    -e N8N_PROTOCOL=https \
    -e N8N_ENCRYPTION_KEY=${N8N_ENCRYPTION_KEY} \
    -e WEBHOOK_URL=https://${N8N_DOMAIN}/ \
    -v n8n_data:/home/node/.n8n \
    n8nio/n8n:latest

  sleep 15
  echo "n8n arrancat"
EOF

# ── Pas 5: Importar workflows ─────────────────────────────────────────────────
log "5/5 Important workflows al nou n8n..."
BACKUP_NAME=$(basename ${BACKUP_DIR})
ssh "${SSH_USER}@${DEST_HOST}" bash <<EOF
  docker cp /tmp/${BACKUP_NAME}/workflows.json amg_n8n:/tmp/workflows.json
  docker exec amg_n8n n8n import:workflow --input=/tmp/workflows.json

  if [ -f /tmp/${BACKUP_NAME}/credentials.json ]; then
    docker cp /tmp/${BACKUP_NAME}/credentials.json amg_n8n:/tmp/credentials.json
    docker exec amg_n8n n8n import:credentials --input=/tmp/credentials.json
  fi
  echo "Import completat"
EOF

log "Migració n8n completada!"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  ACCIONS MANUALS NECESSÀRIES"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "1. Configura DNS: A n8n.${N8N_DOMAIN%%.*} → ${DEST_HOST}"
echo "   (o actualitza el subdomain al teu proveïdor DNS)"
echo ""
echo "2. Configura Traefik al nou servidor per servir n8n via HTTPS"
echo ""
echo "3. Verifica que els webhooks funcionen als workflows importats"
echo ""
echo "4. Un cop verificat, atura n8n al servidor origen:"
echo "   ssh ${SSH_USER}@${SOURCE_HOST} 'docker stop amg_n8n && docker rm amg_n8n'"
echo ""
echo "════════════════════════════════════════════════════════════════"
