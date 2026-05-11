.PHONY: dev dev-down prod prod-down logs ps shell-postgres shell-redis

ENV_FILE=.env
COMPOSE_DEV=docker compose -f infra/docker-compose.dev.yml --env-file $(ENV_FILE)
COMPOSE_PROD=docker compose -f infra/docker-compose.yml --env-file $(ENV_FILE)

# --- Desenvolupament ---

dev:
	$(COMPOSE_DEV) up -d
	@echo "Serveis disponibles:"
	@echo "  PostgreSQL  → localhost:5432"
	@echo "  Redis       → localhost:6379"
	@echo "  MinIO API   → http://localhost:9000"
	@echo "  MinIO UI    → http://localhost:9001"
	@echo "  Mailhog     → http://localhost:8025"

dev-down:
	$(COMPOSE_DEV) down

dev-reset:
	$(COMPOSE_DEV) down -v

logs:
	$(COMPOSE_DEV) logs -f

# --- Producció ---

prod:
	$(COMPOSE_PROD) up -d --build

prod-down:
	$(COMPOSE_PROD) down

ps:
	$(COMPOSE_PROD) ps

# --- Shells ---

shell-postgres:
	$(COMPOSE_DEV) exec postgres psql -U $$(grep POSTGRES_USER $(ENV_FILE) | cut -d= -f2) -d $$(grep POSTGRES_DB $(ENV_FILE) | cut -d= -f2)

shell-redis:
	$(COMPOSE_DEV) exec redis redis-cli -a $$(grep REDIS_PASSWORD $(ENV_FILE) | cut -d= -f2)

# --- Utilitats ---

env-check:
	@test -f $(ENV_FILE) || (echo "ERROR: Crea el fitxer .env a partir de .env.example" && exit 1)
	@echo ".env trobat ✓"
