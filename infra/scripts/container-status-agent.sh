#!/bin/sh
# Agent InfraOps — reporta l'estat dels contenidors al backend (docker ps -> POST).
# Corre al HOST via cron. El backend NO toca Docker: només rep aquest report.
#
# Config (variables d'entorn):
#   INFRAOPS_AGENT_TOKEN  Secret compartit (mateix valor que a System Config)
#   INFRAOPS_API_URL      URL del backend (default https://api.amgdl.com)
#
# Instal·lació (exemple, al host):
#   echo 'INFRAOPS_AGENT_TOKEN=<token>' > /etc/infraops-agent.env
#   * * * * * root . /etc/infraops-agent.env && /opt/amg/infra/scripts/container-status-agent.sh

TOKEN="${INFRAOPS_AGENT_TOKEN:-}"
API_URL="${INFRAOPS_API_URL:-https://api.amgdl.com}"

if [ -z "$TOKEN" ]; then
  echo "INFRAOPS_AGENT_TOKEN no configurat" >&2
  exit 1
fi

# Array JSON amb name/state/status de cada contenidor (inclou aturats amb -a)
BODY=$(docker ps -a --format '{{.Names}}	{{.State}}	{{.Status}}' | awk -F'\t' '
BEGIN { printf "[" }
{
  if (NR>1) printf ",";
  gsub(/\\/,"\\\\",$1); gsub(/"/,"\\\"",$1);
  gsub(/\\/,"\\\\",$2); gsub(/"/,"\\\"",$2);
  gsub(/\\/,"\\\\",$3); gsub(/"/,"\\\"",$3);
  printf "{\"name\":\"%s\",\"state\":\"%s\",\"status\":\"%s\"}", $1, $2, $3;
}
END { printf "]" }')

curl -s -m 15 -o /dev/null -w "%{http_code}\n" \
  -X POST "$API_URL/api/v1/infraops/agent/container-status" \
  -H "X-Agent-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BODY"
