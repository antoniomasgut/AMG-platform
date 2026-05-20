#!/bin/bash
set -a
source "$(dirname "$0")/infra/.env"
set +a

DB_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB:-amg_platform}"
JAR="$(dirname "$0")/backend/target/digitalitzacio-0.0.1-SNAPSHOT.jar"

exec java -jar "$JAR" \
  "--spring.data.redis.password=${REDIS_PASSWORD}" \
  "--spring.datasource.url=${DB_URL}" \
  "--spring.datasource.username=${POSTGRES_USER:-amg}" \
  "--spring.datasource.password=${POSTGRES_PASSWORD}" \
  "--spring.mail.host=${MAIL_HOST:-localhost}" \
  "--jwt.secret=${JWT_SECRET}" \
  "$@"
