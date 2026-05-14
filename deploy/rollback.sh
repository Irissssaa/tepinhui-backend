#!/usr/bin/env bash

set -euo pipefail

# 应用配置
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"

# 服务配置
APP_PORT="${APP_PORT:-8060}"
APP_API_PREFIX="${APP_API_PREFIX:-/tph}"
APP_HEALTH_ENDPOINT="${APP_HEALTH_ENDPOINT:-/health}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-5}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-120}"

# 路径配置
VERSIONS_DIR="${DEPLOY_PATH}/versions"
LATEST_VERSION="${VERSIONS_DIR}/latest"

# systemd 服务名称
SERVICE_NAME="${APP_NAME}"

normalize_path() {
  local value="$1"
  if [[ -z "${value}" || "${value}" == "/" ]]; then
    printf ""
    return
  fi

  value="/${value#/}"
  value="${value%/}"
  printf "%s" "${value}"
}

APP_API_PREFIX="$(normalize_path "${APP_API_PREFIX}")"
APP_HEALTH_ENDPOINT="$(normalize_path "${APP_HEALTH_ENDPOINT}")"
HEALTH_URL="http://127.0.0.1:${APP_PORT}${APP_API_PREFIX}${APP_HEALTH_ENDPOINT}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

error() {
  log "ERROR: $*" >&2
  exit 1
}

version_dir() {
  local version="$1"
  printf '%s/%s' "${VERSIONS_DIR}" "${version}"
}

version_has_jar() {
  local version="$1"
  [[ -n "${version}" && -f "$(version_dir "${version}")/app.jar" ]]
}

list_versions_sorted() {
  local dir version timestamp
  local records=()

  shopt -s nullglob
  for dir in "${VERSIONS_DIR}"/*; do
    [[ -d "${dir}" ]] || continue
    version="$(basename "${dir}")"
    [[ "${version}" == "latest" ]] && continue

    if [[ -f "${dir}/app.jar" ]]; then
      timestamp="$(stat -c '%Y' "${dir}/app.jar")"
    else
      timestamp="$(stat -c '%Y' "${dir}")"
    fi

    records+=("${timestamp}"$'\t'"${version}")
  done
  shopt -u nullglob

  if (( ${#records[@]} == 0 )); then
    return 0
  fi

  printf '%s\n' "${records[@]}" | sort -t $'\t' -k1,1nr -k2,2r | cut -f2
}

get_current_version() {
  if [[ -L "${LATEST_VERSION}" ]]; then
    basename "$(readlink "${LATEST_VERSION}")"
  else
    echo "unknown"
  fi
}

health_check() {
  local elapsed=0

  while (( elapsed < STARTUP_TIMEOUT )); do
    if curl --silent --show-error --fail "${HEALTH_URL}" >/dev/null; then
      return 0
    fi

    if ! sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
      log "Service exited unexpectedly."
      sudo journalctl -u "${SERVICE_NAME}" --no-pager -n 100
      return 1
    fi

    sleep "${HEALTH_CHECK_INTERVAL}"
    elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
  done

  log "Health check timed out after ${STARTUP_TIMEOUT}s"
  sudo journalctl -u "${SERVICE_NAME}" --no-pager -n 100
  return 1
}

list_versions() {
  log "Available versions:"
  log "========================================="

  local current_version count created_time version
  current_version="$(get_current_version)"
  count=0

  while IFS= read -r version; do
    [[ -n "${version}" ]] || continue
    count=$((count + 1))

    if [[ -f "$(version_dir "${version}")/app.jar" ]]; then
      created_time="$(stat -c '%y' "$(version_dir "${version}")/app.jar" 2>/dev/null | cut -d. -f1 || echo 'unknown')"
    else
      created_time="unknown"
    fi

    if [[ "${version}" == "${current_version}" ]]; then
      printf '%s. %s (current) - Created: %s\n' "${count}" "${version}" "${created_time}"
    else
      printf '%s. %s - Created: %s\n' "${count}" "${version}" "${created_time}"
    fi
  done < <(list_versions_sorted)

  log "========================================="
  log "Total versions: ${count}"
}

get_version() {
  local version="$1"

  if [[ ! -d "$(version_dir "${version}")" ]]; then
    error "Version does not exist: ${version}"
  fi

  if ! version_has_jar "${version}"; then
    error "JAR file not found in version: ${version}"
  fi
}

rollback_to_version() {
  local version="$1"

  log "Rolling back to version: ${version}"
  get_version "${version}"

  log "Stopping current service..."
  if sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
    sudo systemctl stop "${SERVICE_NAME}"
    sleep 3
  fi

  log "Updating version symlink..."
  ln -sfn "$(version_dir "${version}")" "${LATEST_VERSION}"

  log "Reloading systemd configuration..."
  sudo systemctl daemon-reload
  sudo systemctl reset-failed "${SERVICE_NAME}" >/dev/null 2>&1 || true

  log "Starting service..."
  sudo systemctl start "${SERVICE_NAME}"

  log "Waiting for service to become healthy..."
  sleep 5

  if health_check; then
    log "Rollback completed successfully!"
    log "Service is running version: ${version}"
    return 0
  fi

  return 1
}

show_current_status() {
  if [[ ! -L "${LATEST_VERSION}" ]]; then
    log "No current version deployed"
    return 1
  fi

  log "Current deployed version: $(get_current_version)"
  log "Service status: $(sudo systemctl is-active "${SERVICE_NAME}" 2>/dev/null || echo 'unknown')"
  log "Health URL: ${HEALTH_URL}"
}

show_help() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Manage deployment versions and perform rollbacks.

Options:
  -l, --list              List all available versions
  -v, --version VERSION   Rollback to specified version
  -s, --status            Show current deployed version status
  -h, --help              Show this help message

Environment Variables:
  APP_NAME                 Application name (default: tepinhui-backend)
  DEPLOY_PATH              Deployment path (default: /home/tph)

Examples:
  $(basename "$0") --list                          # List all versions
  $(basename "$0") --version 20260508-abc123       # Rollback to specific version
  $(basename "$0") --status                        # Show current status
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -l|--list)
      list_versions
      exit 0
      ;;
    -v|--version)
      if [[ -z "${2:-}" ]]; then
        error "Version is required for rollback"
      fi
      rollback_to_version "$2"
      exit $?
      ;;
    -s|--status)
      show_current_status
      exit $?
      ;;
    -h|--help)
      show_help
      exit 0
      ;;
    *)
      error "Unknown option: $1"
      ;;
  esac
done

show_help
