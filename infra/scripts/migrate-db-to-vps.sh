#!/usr/bin/env bash
# migrate-db-to-vps.sh — Migra PostgreSQL del servidor actual a un VPS dedicat
#
# ÚS:
#   ./migrate-db-to-vps.sh --source-host=ACTUAL_VPS_IP --dest-host=NEW_DB_VPS_IP
#
# PREREQUISITS:
#   - SSH sense contrasenya als dos servidors (clau SSH configurada)
#   - PostgreSQL Docker al servidor actual (amg_postgres)
#   - Docker al servidor destí
#   - Variables d'entorn carregades o passat per arguments
#
# FLUX:
#   1. Dump PostgreSQL al servidor origen
#   2. Copiar dump al servidor destí via scp
#   3. Arrancar PostgreSQL al servidor destí
#   4. Restaurar dump
#   5. Verificar connexió
#   6. Aturar PostgreSQL al servidor origen (manual — no ho fem automàticament)
#   7. Actualitzar DATASOURCE_URL al backend (mostra instruccions)

set -euo pipefail

# ── Arguments ─────────────────────────────────────────────────────────────────
SOURCE_HOST=""
DEST_HOST=""
POSTGRES_USER="${POSTGRES_USER:-amg}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
POSTGRES_DB="${POSTGRES_DB:-amg_platform}"
SSH_USER="${SSH_USER:-root}"
BACKUP_FILE="/tmp/amg_migration_$(date +%Y%m%d_%H%M%S).sql.gz"

for arg in "$@"; do
  case $arg in
    --source-host=*) SOURCE_HOST="${arg#*=}" ;;
    --dest-host=*)   DEST_HOST="${arg#*=}" ;;
    --pg-user=*)     POSTGRES_USER="${arg#*=}" ;;
    --pg-db=*)       POSTGRES_DB="${arg#*=}" ;;
    --ssh-user=*)    SSH_USER="${arg#*=}" ;;
  esac
done

if [[ -z "$SOURCE_HOST" || -z "$DEST_HOST" || -z "$POSTGRES_PASSWORD" ]]; then
  echo "ERROR: cal especificar --source-host, --dest-host i POSTGRES_PASSWORD"
  echo "Ús: POSTGRES_PASSWORD=secret ./migrate-db-to-vps.sh --source-host=1.2.3.4 --dest-host=5.6.7.8"
  exit 1
fi

log() { echo "[$(date '+%H:%M:%S')] $*"; }

# ── Pas 1: Dump al servidor origen ────────────────────────────────────────────
log "1/6 Fent pg_dump al servidor origen ($SOURCE_HOST)..."
ssh "${SSH_USER}@${SOURCE_HOST}" \
  "docker exec amg_postgres pg_dump -U ${POSTGRES_USER} ${POSTGRES_DB} | gzip > ${BACKUP_FILE}"
log "   Dump creat a ${SOURCE_HOST}:${BACKUP_FILE}"

# ── Pas 2: Copiar dump al servidor destí ──────────────────────────────────────
log "2/6 Copiant dump al servidor destí ($DEST_HOST)..."
ssh "${SSH_USER}@${SOURCE_HOST}" \
  "scp ${BACKUP_FILE} ${SSH_USER}@${DEST_HOST}:${BACKUP_FILE}"
log "   Dump copiat a ${DEST_HOST}:${BACKUP_FILE}"

# ── Pas 3: Arrancar PostgreSQL al servidor destí ──────────────────────────────
log "3/6 Arrancant PostgreSQL al servidor destí..."
ssh "${SSH_USER}@${DEST_HOST}" bash <<EOF
  docker run -d \
    --name amg_postgres \
    --restart unless-stopped \
    -e POSTGRES_DB=${POSTGRES_DB} \
    -e POSTGRES_USER=${POSTGRES_USER} \
    -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
    -p 5432:5432 \
    -v postgres_data:/var/lib/postgresql/data \
    postgres:16-alpine

  echo "Esperant PostgreSQL..."
  sleep 10
  until docker exec amg_postgres pg_isready -U ${POSTGRES_USER}; do
    sleep 2
  done
  echo "PostgreSQL llest"
EOF

# ── Pas 4: Restaurar dump ─────────────────────────────────────────────────────
log "4/6 Restaurant dump al servidor destí..."
ssh "${SSH_USER}@${DEST_HOST}" \
  "zcat ${BACKUP_FILE} | docker exec -i amg_postgres psql -U ${POSTGRES_USER} ${POSTGRES_DB}"
log "   Restauració completada"

# ── Pas 5: Verificar ──────────────────────────────────────────────────────────
log "5/6 Verificant dades restaurades..."
TABLE_COUNT=$(ssh "${SSH_USER}@${DEST_HOST}" \
  "docker exec amg_postgres psql -U ${POSTGRES_USER} ${POSTGRES_DB} -t -c \"SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';\"" | tr -d ' ')
log "   Taules trobades: ${TABLE_COUNT}"

if [[ "${TABLE_COUNT}" -lt 5 ]]; then
  echo "ERROR: Massa poques taules (${TABLE_COUNT}). La restauració pot haver fallat."
  exit 1
fi

# ── Pas 6 (manual) i 7: Instruccions finals ───────────────────────────────────
log "6/6 Migració completada exitosament!"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  ACCIÓ MANUAL NECESSÀRIA"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "1. Actualitza SPRING_DATASOURCE_URL al servidor origen ($SOURCE_HOST):"
echo "   A l'arxiu .env o Coolify, canvia:"
echo "   SPRING_DATASOURCE_URL=jdbc:postgresql://${DEST_HOST}:5432/${POSTGRES_DB}"
echo ""
echo "2. Reinicia el backend:"
echo "   docker compose restart backend"
echo ""
echo "3. Verifica que l'app funciona correctament"
echo ""
echo "4. Un cop verificat, atura PostgreSQL al servidor origen:"
echo "   ssh ${SSH_USER}@${SOURCE_HOST} 'docker stop amg_postgres && docker rm amg_postgres'"
echo ""
echo "5. Elimina el dump temporal:"
echo "   ssh ${SSH_USER}@${SOURCE_HOST} 'rm ${BACKUP_FILE}'"
echo "   ssh ${SSH_USER}@${DEST_HOST} 'rm ${BACKUP_FILE}'"
echo ""
echo "════════════════════════════════════════════════════════════════"
