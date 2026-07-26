#!/usr/bin/env bash
set -Eeuo pipefail

APP_ROOT="${SUPERVISION_APP_ROOT:-/opt/supervision}"
RELEASES_DIR="$APP_ROOT/releases"
CURRENT_LINK="$APP_ROOT/current"
SHARED_ENV="$APP_ROOT/.env"
BACKUP_ROOT="${SUPERVISION_BACKUP_ROOT:-/opt/backups/supervision}"
KEEP_RELEASES="${SUPERVISION_KEEP_RELEASES:-5}"
PROJECT_NAME="supervision"
HEALTH_URL="${SUPERVISION_HEALTH_URL:-http://127.0.0.1:8002/api/health}"
LOCK_FILE="/tmp/supervision-deploy.lock"

log() { printf '[supervision] %s\n' "$*"; }
die() { printf '[supervision] ERROR: %s\n' "$*" >&2; exit 1; }

require_runtime() {
  command -v docker >/dev/null 2>&1 || die "Docker is not installed"
  command -v flock >/dev/null 2>&1 || die "flock is not installed"
  sudo docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is unavailable"
  [[ -f "$SHARED_ENV" ]] || die "Missing $SHARED_ENV; create it from .env.example before deployment"
}

compose() {
  local release_dir="$1"
  shift
  sudo env SUPERVISION_RELEASE="$(basename "$release_dir")" docker compose \
      --project-name "$PROJECT_NAME" \
      --env-file "$SHARED_ENV" \
      --file "$release_dir/docker-compose.yml" "$@"
}

wait_for_health() {
  local release_dir="$1"
  for _attempt in $(seq 1 36); do
    if curl -fsS "$HEALTH_URL" >/dev/null; then
      compose "$release_dir" ps
      return 0
    fi
    sleep 5
  done
  compose "$release_dir" ps || true
  compose "$release_dir" logs --tail=150 api web >&2 || true
  return 1
}

atomic_switch() {
  local release_dir="$1"
  local temporary_link="$APP_ROOT/.current.$$.tmp"
  ln -s "$release_dir" "$temporary_link"
  mv -Tf "$temporary_link" "$CURRENT_LINK"
}

backup_database() {
  local backup_dir="$1"
  if sudo docker ps --format '{{.Names}}' | grep -qx 'supervision-mysql'; then
    log "Backing up MySQL to $backup_dir/database.sql.gz"
    sudo docker exec supervision-mysql sh -c \
      'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers supervision' \
      | gzip > "$backup_dir/database.sql.gz"
  else
    log "MySQL is not running; skipping database backup for initial deployment"
  fi
}

prune_releases() {
  local current_target
  current_target="$(readlink -f "$CURRENT_LINK" 2>/dev/null || true)"
  mapfile -t releases < <(find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' | sort -rn | cut -d' ' -f2-)
  local index=0
  for release_dir in "${releases[@]}"; do
    index=$((index + 1))
    if (( index > KEEP_RELEASES )) && [[ "$release_dir" != "$current_target" ]]; then
      log "Pruning old release $(basename "$release_dir")"
      rm -rf -- "$release_dir"
    fi
  done
}

deploy_release() {
  local archive="${1:?missing release archive}"
  local release_id="${2:?missing release id}"
  [[ "$release_id" =~ ^[A-Za-z0-9._-]{7,64}$ ]] || die "Invalid release id"
  [[ -f "$archive" ]] || die "Release archive not found: $archive"
  sudo install -d -m 750 -o "$(id -u)" -g "$(id -g)" "$APP_ROOT" "$RELEASES_DIR" "$BACKUP_ROOT"
  require_runtime

  local release_dir="$RELEASES_DIR/$release_id"
  local previous_dir
  previous_dir="$(readlink -f "$CURRENT_LINK" 2>/dev/null || true)"
  if [[ -e "$release_dir" ]]; then
    if [[ "$previous_dir" == "$release_dir" ]]; then
      rm -f -- "$archive"
      wait_for_health "$release_dir" || die "Current release $release_id is unhealthy"
      log "Release is already current and healthy: $release_id"
      return
    fi
    log "Replacing incomplete or inactive release $release_id"
    rm -rf -- "$release_dir"
  fi

  mkdir "$release_dir"
  tar -xzf "$archive" -C "$release_dir"
  rm -f -- "$archive"
  [[ -f "$release_dir/docker-compose.yml" ]] || die "Release does not contain docker-compose.yml"
  install -D -m 750 "$release_dir/scripts/deploy-server.sh" "$APP_ROOT/bin/supervision-deploy"

  local backup_dir="$BACKUP_ROOT/$(date +%Y%m%d-%H%M%S)-$release_id"
  install -d -m 700 "$backup_dir"
  printf '%s\n' "${previous_dir:+$(basename "$previous_dir")}" > "$backup_dir/previous-release"
  backup_database "$backup_dir"

  log "Building release $release_id"
  compose "$release_dir" build api web
  atomic_switch "$release_dir"
  compose "$release_dir" up -d mysql redis rabbitmq api web

  if ! wait_for_health "$release_dir"; then
    if [[ -n "$previous_dir" && -d "$previous_dir" ]]; then
      log "Health check failed; switching application back to $(basename "$previous_dir")"
      atomic_switch "$previous_dir"
      compose "$previous_dir" up -d mysql redis rabbitmq api web
      wait_for_health "$previous_dir" || true
    fi
    die "Deployment failed; database backup retained at $backup_dir"
  fi

  prune_releases
  log "Deployment successful: $release_id"
  log "Database backup: $backup_dir"
}

rollback_release() {
  local release_id="${1:?missing release id}"
  [[ "$release_id" =~ ^[A-Za-z0-9._-]{7,64}$ ]] || die "Invalid release id"
  require_runtime
  local release_dir="$RELEASES_DIR/$release_id"
  [[ -d "$release_dir" ]] || die "Unknown release: $release_id"

  local current_dir
  current_dir="$(readlink -f "$CURRENT_LINK" 2>/dev/null || true)"
  [[ "$current_dir" != "$release_dir" ]] || die "Release $release_id is already current"

  local backup_dir="$BACKUP_ROOT/$(date +%Y%m%d-%H%M%S)-before-rollback-$release_id"
  install -d -m 700 "$backup_dir"
  printf '%s\n' "${current_dir:+$(basename "$current_dir")}" > "$backup_dir/previous-release"
  backup_database "$backup_dir"

  log "Rolling application back to $release_id"
  atomic_switch "$release_dir"
  compose "$release_dir" up -d mysql redis rabbitmq api web
  if ! wait_for_health "$release_dir"; then
    if [[ -n "$current_dir" && -d "$current_dir" ]]; then
      log "Rollback health check failed; restoring $(basename "$current_dir")"
      atomic_switch "$current_dir"
      compose "$current_dir" up -d mysql redis rabbitmq api web
      wait_for_health "$current_dir" || true
    fi
    die "Rollback failed; pre-rollback database backup retained at $backup_dir"
  fi
  log "Rollback successful: $release_id"
}

list_releases() {
  local current_target
  current_target="$(readlink -f "$CURRENT_LINK" 2>/dev/null || true)"
  printf '%-42s %-20s %s\n' RELEASE CREATED STATUS
  while IFS= read -r release_dir; do
    local status='available'
    [[ "$release_dir" == "$current_target" ]] && status='current'
    printf '%-42s %-20s %s\n' "$(basename "$release_dir")" "$(date -r "$release_dir" '+%F %T')" "$status"
  done < <(find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null | sort)
}

main() {
  local action="${1:-}"
  shift || true
  exec 9>"$LOCK_FILE"
  flock -n 9 || die "Another deployment or rollback is running"
  case "$action" in
    deploy) deploy_release "$@" ;;
    rollback) rollback_release "$@" ;;
    list) list_releases ;;
    *) die "Usage: $0 {deploy <archive> <release-id>|rollback <release-id>|list}" ;;
  esac
}

main "$@"
