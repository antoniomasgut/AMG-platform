#!/usr/bin/env bash
# deploy.sh — Desplega amgdl.com des de la màquina local
#
# Ús:
#   ./infra/scripts/deploy.sh                  # deploy complet
#   ./infra/scripts/deploy.sh --no-frontend    # només backend
#   ./infra/scripts/deploy.sh --no-backend     # només frontend
#   ./infra/scripts/deploy.sh --no-push        # no fa git push (ja ho has fet)

set -euo pipefail

# ── Configuració ──────────────────────────────────────────────────────────
PROD_HOST="root@65.108.148.62"
PROD_REPO="/opt/amg"
PROD_ENV="/opt/amg/.env"
API_URL="https://api.amgdl.com"
HEALTH_URL="https://api.amgdl.com/api/v1/ops/health"

# ── Colors ────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
step() { echo -e "\n${BLUE}══ $1 ══${NC}"; }
ok()   { echo -e "  ${GREEN}✓${NC} $1"; }
warn() { echo -e "  ${YELLOW}⚠${NC} $1"; }
fail() { echo -e "  ${RED}✗${NC} $1"; exit 1; }

# ── Flags ─────────────────────────────────────────────────────────────────
BUILD_BACKEND=true
BUILD_FRONTEND=true
DO_PUSH=true
for arg in "$@"; do
  case $arg in
    --no-backend)  BUILD_BACKEND=false  ;;
    --no-frontend) BUILD_FRONTEND=false ;;
    --no-push)     DO_PUSH=false        ;;
  esac
done

echo ""
echo -e "${BLUE}╔══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   AMG Platform — Deploy a producció  ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════╝${NC}"

# ── 1. Verificació local ──────────────────────────────────────────────────
step "1. Verificació local"

if ! command -v git &>/dev/null; then fail "git no trobat"; fi
if ! command -v ssh &>/dev/null; then fail "ssh no trobat"; fi
if ! command -v curl &>/dev/null; then fail "curl no trobat"; fi

BRANCH=$(git rev-parse --abbrev-ref HEAD)
COMMIT=$(git rev-parse --short HEAD)
ok "Branca: ${BRANCH} · Commit: ${COMMIT}"

if ! git diff --quiet HEAD 2>/dev/null; then
  warn "Hi ha canvis sense commitejar — el deploy usarà l'últim commit"
fi

# ── 2. Push al remote ─────────────────────────────────────────────────────
step "2. Push a GitHub"
if $DO_PUSH; then
  git push origin "$BRANCH"
  ok "Push completat → github.com"
else
  ok "Push omès (--no-push)"
fi

# ── 3. Pull a producció ───────────────────────────────────────────────────
step "3. Sincronitzant codi a producció"
ssh "$PROD_HOST" "cd $PROD_REPO && git fetch origin && git reset --hard origin/$BRANCH"
ok "Codi actualitzat a $PROD_HOST"

# ── 4. Build backend ──────────────────────────────────────────────────────
if $BUILD_BACKEND; then
  step "4. Build backend (pot trigar ~3 min)"
  ssh "$PROD_HOST" "
    set -o pipefail
    cd $PROD_REPO/infra
    docker compose -f docker-compose.yml --env-file $PROD_ENV build --no-cache backend 2>&1 | grep -E '#[0-9]+|ERROR|Successfully|Step' | tail -15
  " || fail "Build backend fallit — revisa: ssh $PROD_HOST 'cd $PROD_REPO/infra && docker compose build backend'"
  ok "Imatge backend construïda"
else
  ok "4. Backend skipped (--no-backend)"
fi

# ── 5. Build frontend ─────────────────────────────────────────────────────
if $BUILD_FRONTEND; then
  step "5. Build frontend (pot trigar ~2 min)"
  ssh "$PROD_HOST" "
    set -o pipefail
    cd $PROD_REPO/infra
    docker compose -f docker-compose.yml --env-file $PROD_ENV build frontend 2>&1 | grep -E '#[0-9]+|ERROR|Successfully|Step' | tail -15
  " || fail "Build frontend fallit — revisa: ssh $PROD_HOST 'cd $PROD_REPO/infra && docker compose build frontend'"
  ok "Imatge frontend construïda"
else
  ok "5. Frontend skipped (--no-frontend)"
fi

# ── 6. Deploy ─────────────────────────────────────────────────────────────
step "6. Reiniciant serveis"
ssh "$PROD_HOST" "
  # Elimina contenidors orfes de deploys anteriors per evitar conflictes de nom
  docker stop amg_frontend 2>/dev/null || true
  docker rm   amg_frontend 2>/dev/null || true
  docker stop amg_backend  2>/dev/null || true
  docker rm   amg_backend  2>/dev/null || true
  cd $PROD_REPO/infra
  docker compose -f docker-compose.yml --env-file $PROD_ENV up -d backend frontend 2>&1 | grep -v '^#'
"
ok "Contenidors actualitzats"

# ── 7. Netejar imatges antigues ───────────────────────────────────────────
ssh "$PROD_HOST" "docker image prune -f > /dev/null 2>&1 || true"
ok "Imatges dangling eliminades"

# ── 8. Health check backend ───────────────────────────────────────────────
step "7. Health check"
MAX_RETRIES=48  # 4 minuts
RETRY=0
echo -n "  Esperant backend"
until curl -sf "$HEALTH_URL" > /dev/null 2>&1; do
  RETRY=$((RETRY+1))
  if [ $RETRY -ge $MAX_RETRIES ]; then
    echo ""
    fail "Backend no respon després de $((MAX_RETRIES * 5))s.\n  Comprova els logs: ssh $PROD_HOST 'docker logs amg-backend --tail=50'"
  fi
  echo -n "."
  sleep 5
done
echo ""
ok "Backend sa → $HEALTH_URL"

# Traefik necessita reinici per actualitzar la IP dels contenidors recreats.
# Es fa DESPRÉS que els contenidors estiguin sans — si es reinicia just en
# recrear-los, cacheja la IP antiga i retorna 504 fins a un nou reinici.
ssh "$PROD_HOST" "docker restart coolify-proxy > /dev/null 2>&1 || true"
sleep 8

# ── 9. Health check frontend ──────────────────────────────────────────────
MAX_RETRIES=12
RETRY=0
echo -n "  Esperant frontend"
until curl -sfo /dev/null https://amgdl.com/ca/login 2>/dev/null; do
  RETRY=$((RETRY+1))
  if [ $RETRY -ge $MAX_RETRIES ]; then
    echo ""
    warn "Frontend no respon a /ca/login (pot ser normal si el contenidor encara arrenca)"
    break
  fi
  echo -n "."
  sleep 5
done
echo ""

HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" https://amgdl.com/ca/login 2>/dev/null || echo "0")
if [[ "$HTTP_CODE" == "200" ]]; then
  ok "Frontend sa → amgdl.com/ca/login (200)"
else
  warn "Frontend retorna HTTP $HTTP_CODE"
fi

# ── 10. Resum ──────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Deploy completat ✓  ($COMMIT)       ${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
echo "  Backend:  https://api.amgdl.com/actuator/health"
echo "  Frontend: https://amgdl.com/ca/login"
echo "  Logs:     ssh $PROD_HOST 'docker logs amg-backend --tail=100'"
echo ""
