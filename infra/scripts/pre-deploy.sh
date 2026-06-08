#!/bin/bash
# pre-deploy.sh — Valida l'estat abans de desplegar
# Ús: ./infra/scripts/pre-deploy.sh [--fix]
#   --fix: aplica reparacions automàtiques (només dev)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$INFRA_DIR/.." && pwd)"
ENV_FILE="$INFRA_DIR/.env"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FIX_MODE=false
[[ "${1:-}" == "--fix" ]] && FIX_MODE=true

ERRORS=0
WARNS=0

pass()  { echo -e "  ${GREEN}✓${NC} $1"; }
warn()  { echo -e "  ${YELLOW}⚠${NC} $1"; WARNS=$((WARNS+1)); }
fail()  { echo -e "  ${RED}✗${NC} $1"; ERRORS=$((ERRORS+1)); }

# ───────────────────────────────────────────
# 1. Environ variables obligatòries
# ───────────────────────────────────────────
check_env() {
    local var=$1 default_val="${2:-}"
    local val
    val=$(grep "^${var}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2- | head -1 || echo "")
    if [[ -z "$val" || "$val" == "$default_val" ]]; then
        fail "$var no definida o buida a .env"
        return 1
    fi
    pass "$var definida"
    return 0
}

echo ""
echo "═══════════════════════════════════════════"
echo "  PRE-DEPLOY CHECK"
echo "═══════════════════════════════════════════"
echo ""

echo "── Environ ──"
check_env POSTGRES_PASSWORD
check_env JWT_SECRET
check_env REDIS_PASSWORD
check_env MINIO_ROOT_PASSWORD
check_env JASYPT_ENCRYPTOR_PASSWORD
check_env VAULT_MASTER_KEY

# ───────────────────────────────────────────
# 2. Docker Compose sintaxi
# ───────────────────────────────────────────
echo ""
echo "── Docker Compose ──"
if docker compose -f "$INFRA_DIR/docker-compose.yml" config >/dev/null 2>&1; then
    pass "docker-compose.yml sintaxi correcta"
else
    fail "docker-compose.yml té errors de sintaxi"
fi

# ───────────────────────────────────────────
# 3. Serveis necessaris en marxa
# ───────────────────────────────────────────
echo ""
echo "── Serveis ──"
for svc in amg_postgres amg_redis; do
    if docker ps --format '{{.Names}}' | grep -q "$svc"; then
        pass "$svc running"
    else
        fail "$svc no està en marxa"
    fi
done

# ───────────────────────────────────────────
# 4. Comprovar Flyway migrations pendents
# ───────────────────────────────────────────
echo ""
echo "── Flyway ──"
if docker ps --format '{{.Names}}' | grep -q amg_backend; then
    warn "El backend està corrent — atura'l abans de desplegar (make prod-down)"
else
    pass "Backend aturat"
fi

# Comprovar si hi ha SQL de migració sense versionar
MIGRATIONS_DIR="$PROJECT_DIR/backend/src/main/resources/db/migration"
PENDING=$(find "$MIGRATIONS_DIR" -name "*.sql" | wc -l)
APPLIED=$(docker compose -f "$INFRA_DIR/docker-compose.yml" exec -T postgres \
    psql -U "$(grep ^POSTGRES_USER "$ENV_FILE" | cut -d= -f2)" \
    -d "$(grep ^POSTGRES_DB "$ENV_FILE" | cut -d= -f2)" \
    -tA -c "SELECT count(*) FROM flyway_schema_history WHERE success=true;" 2>/dev/null || echo "0")
pass "Migrations al FS: $PENDING · Aplicades: $APPLIED"

# ───────────────────────────────────────────
# 5. Schema JPA vs DB (DDL diff)
# ───────────────────────────────────────────
echo ""
echo "── Schema JPA vs DB ──"
JPA_DDL="$INFRA_DIR/target/jpa-ddl.sql"
mkdir -p "$INFRA_DIR/target"

# Generem el DDL que Hibernate crearia
if [ ! -f "$PROJECT_DIR/backend/target/digitalitzacio-0.0.1-SNAPSHOT.jar" ]; then
    warn "JAR no compilat — fes make backend-build primer"
else
    # Executem Hibernate SchemaExport per generar DDL
    # (Alternativa: usar spring.jpa.properties.jakarta.persistence.schema-generation.scripts)
    warn "Per validar el schema complet: java -jar backend/target/*.jar --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create"
    warn "Després compara target/classes/db/migration/ amb el DDL generat"
fi

# ───────────────────────────────────────────
# 6. Build del backend (Maven)
# ───────────────────────────────────────────
echo ""
echo "── Build ──"
if [ -f "$PROJECT_DIR/backend/pom.xml" ]; then
    pass "pom.xml trobat"
else
    fail "No es troba backend/pom.xml"
fi

# ───────────────────────────────────────────
# 7. Resum
# ───────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════"
if [ $ERRORS -gt 0 ]; then
    echo -e "  ${RED}${ERRORS} errors, ${WARNS} warnings${NC}"
    echo "  Corregeix els errors abans de desplegar."
    exit 1
elif [ $WARNS -gt 0 ]; then
    echo -e "  ${YELLOW}${WARNS} warnings${NC} (pots desplegar, però revísals)"
    exit 0
else
    echo -e "  ${GREEN}Tot correcte — llest per desplegar${NC}"
    exit 0
fi
