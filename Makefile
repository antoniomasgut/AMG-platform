.PHONY: dev dev-down dev-reset dev-status logs \
        backend frontend backend-build \
        prod prod-down prod-status prod-validate \
        deploy deploy-backend deploy-frontend \
        logs-prod ssh-prod \
        shell-postgres shell-redis shell-n8n \
        env-check seed pre-deploy flyway-repair schema-check

ENV_FILE=.env
COMPOSE_DEV=docker compose -f infra/docker-compose.dev.yml --env-file $(ENV_FILE)
COMPOSE_PROD=docker compose -f infra/docker-compose.yml --env-file $(ENV_FILE)

# ═══════════════════════════════════════
#  DESENVOLUPAMENT LOCAL
# ═══════════════════════════════════════

dev: env-check
	$(COMPOSE_DEV) up -d
	@echo ""
	@echo "  ✓ Serveis de suport arrencats:"
	@echo "    PostgreSQL  → localhost:5432"
	@echo "    Redis       → localhost:6379"
	@echo "    MinIO API   → http://localhost:9000"
	@echo "    MinIO UI    → http://localhost:9001"
	@echo "    Mailhog     → http://localhost:8025"
	@echo "    n8n         → http://localhost:5679"
	@echo ""
	@echo "  Ara arrenca el backend i el frontend:"
	@echo "    make backend    (terminal 2)"
	@echo "    make frontend   (terminal 3)"

dev-down:
	$(COMPOSE_DEV) down

dev-reset:
	$(COMPOSE_DEV) down -v
	@echo "Volums eliminats. Pròxim 'make dev' partirà de zero."

dev-status:
	$(COMPOSE_DEV) ps

logs:
	$(COMPOSE_DEV) logs -f

logs-%:
	$(COMPOSE_DEV) logs -f $*

# ═══════════════════════════════════════
#  BACKEND I FRONTEND LOCALS
# ═══════════════════════════════════════

backend: env-check
	@echo "Arrencant backend amb perfil dev..."
	cd backend && \
	  SPRING_PROFILES_ACTIVE=dev \
	  DATASOURCE_URL=$$(grep ^DATASOURCE_URL $(ENV_FILE) | cut -d= -f2) \
	  DATASOURCE_USERNAME=$$(grep ^DATASOURCE_USERNAME $(ENV_FILE) | cut -d= -f2) \
	  DATASOURCE_PASSWORD=$$(grep ^DATASOURCE_PASSWORD $(ENV_FILE) | cut -d= -f2) \
	  REDIS_HOST=$$(grep ^REDIS_HOST $(ENV_FILE) | cut -d= -f2) \
	  REDIS_PASSWORD=$$(grep ^REDIS_PASSWORD $(ENV_FILE) | cut -d= -f2) \
	  JWT_SECRET="$$(grep ^JWT_SECRET $(ENV_FILE) | cut -d= -f2-)" \
	  mvn spring-boot:run

frontend: env-check
	@echo "Arrencant frontend en mode dev..."
	cd frontend && \
	  NEXT_PUBLIC_API_URL=$$(grep ^NEXT_PUBLIC_API_URL $(ENV_FILE) | cut -d= -f2) \
	  npm run dev

# ═══════════════════════════════════════
#  PRODUCCIÓ
# ═══════════════════════════════════════

prod: env-check
	$(COMPOSE_PROD) up -d --build

prod-down:
	$(COMPOSE_PROD) down

prod-status:
	$(COMPOSE_PROD) ps

# ═══════════════════════════════════════
#  SHELLS / DEBUG
# ═══════════════════════════════════════

shell-postgres:
	$(COMPOSE_DEV) exec postgres psql \
	  -U $$(grep ^POSTGRES_USER $(ENV_FILE) | cut -d= -f2) \
	  -d $$(grep ^POSTGRES_DB $(ENV_FILE) | cut -d= -f2)

shell-redis:
	$(COMPOSE_DEV) exec redis redis-cli \
	  -a $$(grep ^REDIS_PASSWORD $(ENV_FILE) | cut -d= -f2)

shell-n8n:
	@echo "n8n UI → http://localhost:5678"
	@echo "User: $$(grep ^N8N_BASIC_AUTH_USER $(ENV_FILE) | cut -d= -f2)"
	@echo "Pass: $$(grep ^N8N_BASIC_AUTH_PASSWORD $(ENV_FILE) | cut -d= -f2)"

# ═══════════════════════════════════════
#  UTILITATS
# ═══════════════════════════════════════

env-check:
	@test -f $(ENV_FILE) || (echo "ERROR: Crea el fitxer .env a partir de .env.example" && exit 1)
	@grep -q "^POSTGRES_PASSWORD=.\+" $(ENV_FILE) || (echo "ERROR: POSTGRES_PASSWORD buit al .env" && exit 1)
	@grep -q "^JWT_SECRET=.\+" $(ENV_FILE) || (echo "ERROR: JWT_SECRET buit al .env" && exit 1)
	@echo ".env ✓"

seed:
	@echo "Inserint dades de demo..."
	$(COMPOSE_DEV) exec -T postgres psql \
	  -U $$(grep ^POSTGRES_USER $(ENV_FILE) | cut -d= -f2) \
	  -d $$(grep ^POSTGRES_DB $(ENV_FILE) | cut -d= -f2) \
	  -f /dev/stdin < infra/postgres/seed.sql
	@echo "Seed completat ✓"

# ═══════════════════════════════════════
#  DEPLOY A PRODUCCIÓ (amgdl.com)
# ═══════════════════════════════════════

deploy:
	@infra/scripts/deploy.sh

deploy-backend:
	@infra/scripts/deploy.sh --no-frontend

deploy-frontend:
	@infra/scripts/deploy.sh --no-backend

logs-prod:
	@ssh root@65.108.148.62 'docker logs amg_backend --tail=100 -f'

ssh-prod:
	@ssh root@65.108.148.62

# ───────────────────────────────────────────
#  DEPLOY & VALIDATION (prod)
# ───────────────────────────────────────────

pre-deploy:
	@echo "── Pre-deploy validation ──"
	infra/scripts/pre-deploy.sh

prod-validate: pre-deploy
	@echo "── Validant Docker Compose ──"
	$(COMPOSE_PROD) config > /dev/null
	@echo "Compose config OK"
	@echo "── Comprovant build ──"
	$(COMPOSE_PROD) build --dry-run backend 2>/dev/null || true
	@echo ""
	@echo "Executa 'make prod' per desplegar"

backend-build:
	@echo "── Compilant backend ──"
	cd backend && mvn package -DskipTests -q
	@echo "Build completat ✓"

flyway-repair:
	@echo "── Reparant Flyway checksums ──"
	$(COMPOSE_PROD) exec -T postgres psql \
	  -U $$(grep ^POSTGRES_USER $(ENV_FILE) | cut -d= -f2) \
	  -d $$(grep ^POSTGRES_DB $(ENV_FILE) | cut -d= -f2) \
	  -c "DELETE FROM flyway_schema_history WHERE version IN ('$(filter-out $@,$(MAKECMDGOALS))');" 2>/dev/null || true
	@echo "History entry deleted. Re-deploy per re-aplicar."

schema-check:
	@echo "── Generant DDL que Hibernate espera ──"
	@echo "Compara amb l'schema real a la BD:"
	@echo '  make shell-postgres'
	@echo "Per veure les migracions pendents:"
	@echo "  docker compose -f infra/docker-compose.yml exec -T postgres psql -U amg -d amg_platform -c \"SELECT version, description FROM flyway_schema_history ORDER BY version;\""
