.PHONY: dev dev-down dev-reset dev-status logs \
        backend frontend \
        prod prod-down prod-status \
        shell-postgres shell-redis shell-n8n \
        env-check seed

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
