#!/usr/bin/env bash
# Crea i publica les 3 demos de Salut/Bellesa a producció.
# Ús: ./deploy.sh <ADMIN_JWT_TOKEN>
#
# Per obtenir el token: inicia sessió al portal com a SUPER_ADMIN
# i copia el JWT des de DevTools → Application → localStorage → amg_token

set -euo pipefail

TOKEN="${1:-}"
if [ -z "$TOKEN" ]; then
  echo "Ús: $0 <JWT_TOKEN>"
  exit 1
fi

API="https://api.amgdl.com/api/v1"
TENANT_ID="05003ce9-b624-4430-9988-55eec1e9cc93"
SERVICE_ID="725c5ad5-41c5-4075-82f4-3385cdb9d6e6"
DIR="$(cd "$(dirname "$0")" && pwd)"

create_demo() {
  local FILE="$1"
  local SLUG
  SLUG=$(python3 -c "import json,sys; d=json.load(open('$FILE')); print(d['meta']['slug'])")
  local TITLE
  TITLE=$(python3 -c "import json,sys; d=json.load(open('$FILE')); print(d['meta']['title'])")
  local META_DESC
  META_DESC=$(python3 -c "import json,sys; d=json.load(open('$FILE')); print(d['meta']['metaDescription'])")

  echo ""
  echo "▶ Creant: $TITLE"

  # 1. Crear landing
  LANDING=$(curl -s -X POST "$API/engine/tenants/$TENANT_ID/landings" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"$TITLE\",\"slug\":\"$SLUG\",\"metaDescription\":\"$META_DESC\",\"serviceId\":\"$SERVICE_ID\"}")

  LANDING_ID=$(echo "$LANDING" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")

  if [ -z "$LANDING_ID" ]; then
    echo "  ✗ Error creant la landing. Resposta: $LANDING"
    return 1
  fi

  echo "  ✓ Landing creada: $LANDING_ID"

  # 2. Crear versió amb contingut
  CONTENT=$(python3 -c "import json,sys; d=json.load(open('$FILE')); print(json.dumps({'content': d['content'], 'styles': d['styles']}))")

  VERSION=$(curl -s -X POST "$API/engine/landings/$LANDING_ID/versions" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$CONTENT")

  VERSION_ID=$(echo "$VERSION" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")

  if [ -z "$VERSION_ID" ]; then
    echo "  ✗ Error creant la versió. Resposta: $VERSION"
    return 1
  fi

  echo "  ✓ Versió creada: $VERSION_ID"

  # 3. Publicar
  PUB=$(curl -s -X POST "$API/engine/landings/$LANDING_ID/publish" \
    -H "Authorization: Bearer $TOKEN")

  echo "  ✓ Publicada: $(echo "$PUB" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('publicUrl','(sense domini)') or '(sense domini)')" 2>/dev/null)"
  echo "  → Preview: $API/../${SLUG}"
}

echo "=== Desplegant demos Salut/Bellesa ==="
create_demo "$DIR/estetica-mireia.json"
create_demo "$DIR/fisio-llevant.json"
create_demo "$DIR/nutrició-sa-salut.json"
echo ""
echo "=== Fet! ==="
