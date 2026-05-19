# 04 — Infraestructura Hetzner i Desplegament

## Arquitectura de servidors

```
Internet
    ↓
CX22 — Nginx (reverse proxy + SSL)          ~€4/mes
    ↓ Xarxa privada Hetzner
CX42 — Spring Boot App                      ~€16/mes
CX22 — Panel Admin (React/Next.js)          ~€4/mes
    ↓ Xarxa privada Hetzner
CX32 — PostgreSQL 16                        ~€7/mes
CX22 — Redis 7                              ~€4/mes
─────────────────────────────────────────────────────
Total infraestructura:                      ~€35/mes
```

---

## Fase 1 — Arrancar (0-50 clients)

Un sol servidor CX32 amb tot:

```bash
# Crear servidor CX32 a Hetzner Cloud
# Sistema operatiu: Ubuntu 24.04 LTS
# Ubicació: Falkenstein (Alemanya) — RGPD

# Tot en un servidor:
# - Spring Boot (port 8080)
# - PostgreSQL (port 5432 intern)
# - Redis (port 6379 intern)
# - Nginx (ports 80/443 públics)
```

### Docker Compose producció (Fase 1)

```yaml
version: '3.8'
services:

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - app

  app:
    image: nexelocal/app:latest
    expose:
      - "8080"
    env_file: .env.prod
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: nexelocal
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

---

## Configuració Nginx

```nginx
# /etc/nginx/nginx.conf

events { worker_connections 1024; }

http {
    # Rate limiting global
    limit_req_zone $binary_remote_addr zone=webhooks:10m rate=30r/m;

    # Redirecció HTTP → HTTPS
    server {
        listen 80;
        server_name api.nexelocal.cat nexelocal.cat;
        return 301 https://$host$request_uri;
    }

    # API principal
    server {
        listen 443 ssl;
        server_name api.nexelocal.cat;

        ssl_certificate /etc/letsencrypt/live/nexelocal.cat/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/nexelocal.cat/privkey.pem;

        # Webhooks amb rate limiting
        location /webhook/ {
            limit_req zone=webhooks burst=10 nodelay;
            proxy_pass http://app:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_read_timeout 30s;
        }

        # API REST
        location /api/ {
            proxy_pass http://app:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

---

## SSL gratuït amb Let's Encrypt

```bash
# Instal·lar Certbot
apt install certbot python3-certbot-nginx -y

# Obtenir certificat
certbot --nginx -d nexelocal.cat -d api.nexelocal.cat

# Renovació automàtica (ja configurada per Certbot)
# Verifica amb:
certbot renew --dry-run
```

---

## Desplegament inicial

### 1. Crear servidor a Hetzner

```bash
# Des del panell Hetzner Cloud:
# - Tipus: CX32 (Fase 1) o CX42 (Fase 2)
# - Imatge: Ubuntu 24.04
# - Ubicació: Falkenstein (FSN1)
# - SSH key: afegir la teva clau pública
# - Xarxa privada: crear "nexelocal-network"
```

### 2. Configuració inicial del servidor

```bash
# Connectar per SSH
ssh root@IP_SERVIDOR

# Actualitzar sistema
apt update && apt upgrade -y

# Instal·lar Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker

# Instal·lar Docker Compose
apt install docker-compose-plugin -y

# Crear usuari no-root
adduser nexelocal
usermod -aG docker nexelocal
usermod -aG sudo nexelocal

# Configurar firewall
ufw allow ssh
ufw allow 80
ufw allow 443
ufw enable

# Crear directoris
mkdir -p /opt/nexelocal
chown nexelocal:nexelocal /opt/nexelocal
```

### 3. Fitxer de variables d'entorn

```bash
# /opt/nexelocal/.env.prod
DB_URL=jdbc:postgresql://postgres:5432/nexelocal
DB_USER=nexelocal
DB_PASSWORD=PASSWORD_SEGURA_AQUI

REDIS_HOST=redis
REDIS_PASSWORD=PASSWORD_REDIS_AQUI

ANTHROPIC_API_KEY=sk-ant-xxxx
MISTRAL_API_KEY=xxxx

TWILIO_ACCOUNT_SID=ACxxxx
TWILIO_AUTH_TOKEN=xxxx
TWILIO_WHATSAPP_FROM=+34600000000

RESEND_API_KEY=re_xxxx
RESEND_FROM_DOMAIN=nexelocal.cat

JWT_SECRET=SECRET_MOLT_LLARG_AQUI
ENVIRONMENT=production
```

### 4. Desplegament de l'aplicació

```bash
# Des de la màquina de desenvolupament
# Build de la imatge Docker
./mvnw clean package -DskipTests
docker build -t nexelocal/app:latest .

# Pujar la imatge (Docker Hub o GitHub Registry)
docker push nexelocal/app:latest

# Al servidor
ssh nexelocal@IP_SERVIDOR
cd /opt/nexelocal
docker compose pull
docker compose up -d

# Verificar logs
docker compose logs -f app
```

### 5. Dockerfile de l'aplicació

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/nexelocal-*.jar app.jar

# Usuari no-root per seguretat
RUN addgroup -S nexelocal && adduser -S nexelocal -G nexelocal
USER nexelocal

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx2g", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
```

---

## CI/CD amb GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy NexeLocal

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build JAR
        run: ./mvnw clean package -DskipTests

      - name: Build & Push Docker image
        run: |
          docker build -t nexelocal/app:${{ github.sha }} .
          docker tag nexelocal/app:${{ github.sha }} nexelocal/app:latest
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u nexelocal --password-stdin
          docker push nexelocal/app:latest

      - name: Deploy to Hetzner
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.HETZNER_IP }}
          username: nexelocal
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/nexelocal
            docker compose pull app
            docker compose up -d --no-deps app
            docker compose logs --tail=50 app
```

---

## Monitorització i alertes

### Health check de l'aplicació

```bash
# Endpoint de salut (Spring Boot Actuator)
curl https://api.nexelocal.cat/actuator/health

# Resposta esperada:
{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

### Script de monitorització (cron cada 5 minuts)

```bash
#!/bin/bash
# /opt/nexelocal/monitor.sh

HEALTH_URL="https://api.nexelocal.cat/actuator/health"
OWNER_PHONE="+34600000000"
TWILIO_SID="ACxxxx"
TWILIO_TOKEN="xxxx"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ "$STATUS" != "200" ]; then
    # Enviar WhatsApp d'alerta al dueño
    curl -X POST "https://api.twilio.com/2010-04-01/Accounts/$TWILIO_SID/Messages.json" \
        -u "$TWILIO_SID:$TWILIO_TOKEN" \
        --data-urlencode "From=whatsapp:+14155238886" \
        --data-urlencode "To=whatsapp:$OWNER_PHONE" \
        --data-urlencode "Body=⚠️ NexeLocal: El servidor no respon! Status: $STATUS"

    # Intentar reiniciar
    cd /opt/nexelocal && docker compose restart app
fi
```

```bash
# Afegir al crontab
crontab -e
*/5 * * * * /opt/nexelocal/monitor.sh >> /var/log/nexelocal-monitor.log 2>&1
```

### Backups automàtics de PostgreSQL

```bash
#!/bin/bash
# /opt/nexelocal/backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/nexelocal/backups"
DB_CONTAINER="nexelocal-postgres-1"

mkdir -p $BACKUP_DIR

# Dump de la BD
docker exec $DB_CONTAINER pg_dump -U nexelocal nexelocal | \
    gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# Eliminar backups de més de 30 dies
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completat: backup_$DATE.sql.gz"
```

```bash
# Backup diari a les 3:00 AM
0 3 * * * /opt/nexelocal/backup.sh >> /var/log/nexelocal-backup.log 2>&1
```

---

## Escalat per fases

### Fase 1 → Fase 2 (quan superes 50 clients)

```bash
# 1. Crear servidor PostgreSQL dedicat (CX32)
# 2. Migrar dades
pg_dump nexelocal | ssh nexelocal@IP_POSTGRES psql nexelocal

# 3. Actualitzar .env.prod
DB_URL=jdbc:postgresql://IP_POSTGRES_PRIVADA:5432/nexelocal

# 4. Reiniciar app
docker compose restart app
```

### Fase 2 → Fase 3 (quan superes 200 clients)

```bash
# Upgrade del servidor app: CX42 → CX52
# Des del panell Hetzner: Servers → Resize
# Temps de inactivitat: ~2 minuts

# Upgrade BD: CX32 → CX42
# Igual, des del panell Hetzner
```

---

## Costs per fase

| Fase | Clients | Servidors | Cost infraestructura |
|---|---|---|---|
| 1 | 0-50 | CX32 (tot en un) | ~€7/mes |
| 2 | 50-200 | Nginx+App+Panel+PG+Redis separats | ~€35/mes |
| 3 | 200-500 | Servidors més potents | ~€70/mes |
| 4 | 500-1.000 | CX52 App + CX42 PG | ~€100/mes |
