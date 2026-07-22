#!/usr/bin/env bash
set -Eeuo pipefail

ARCHIVE="${1:?missing archive path}"
APP_DIR="${2:-/opt/supervision}"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/opt/backups/supervision-${STAMP}"

if [[ ! -f "$ARCHIVE" ]]; then
  echo "Release archive not found: $ARCHIVE" >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed" >&2
  exit 1
fi

echo "[remote 1/5] Preparing backup directory: $BACKUP_DIR"
sudo install -d -m 700 -o "$(id -u)" -g "$(id -g)" "$BACKUP_DIR"
if [[ -d "$APP_DIR" ]]; then
  tar -C "$APP_DIR" -czf "$BACKUP_DIR/source.tar.gz" --exclude=.git --exclude=build/web/node_modules --exclude=build/web/dist .
fi

if sudo docker ps --format '{{.Names}}' | grep -qx 'supervision-mysql'; then
  echo "[remote 2/5] Backing up MySQL"
  sudo docker exec supervision-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers supervision' | gzip > "$BACKUP_DIR/database.sql.gz"
else
  echo "MySQL container is not running; refusing deployment without a database backup" >&2
  exit 1
fi

echo "[remote 3/5] Installing release"
mkdir -p "$APP_DIR"
tar -xzf "$ARCHIVE" -C "$APP_DIR"
rm -f "$ARCHIVE"
cd "$APP_DIR"
if [[ ! -f .env ]]; then
  echo "Missing $APP_DIR/.env; refusing to start without production secrets" >&2
  exit 1
fi

echo "[remote 4/5] Building and starting containers"
sudo docker compose build api web
sudo docker compose up -d mysql redis rabbitmq api web

echo "[remote 5/5] Waiting for health endpoint"
for attempt in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8002/api/health >/dev/null; then
    sudo docker compose ps
    echo "Deployment successful. Backup: $BACKUP_DIR"
    rm -f "$0"
    exit 0
  fi
  sleep 5
done

sudo docker compose ps
sudo docker compose logs --tail=120 api web >&2
echo "Health check failed. Backup retained at $BACKUP_DIR" >&2
exit 1
