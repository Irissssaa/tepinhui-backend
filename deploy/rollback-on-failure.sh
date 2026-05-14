#!/usr/bin/env bash

set -euo pipefail

# 应用配置（systemd OnFailure 环境最小化，使用保守默认值）
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"
APP_PORT="${APP_PORT:-8060}"
APP_API_PREFIX="${APP_API_PREFIX:-/tph}"
APP_HEALTH_ENDPOINT="${APP_HEALTH_ENDPOINT:-/health}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-5}"

# 路径配置
VERSIONS_DIR="${DEPLOY_PATH}/versions"
LATEST_VERSION="${VERSIONS_DIR}/latest"
LOG_FILE="${DEPLOY_PATH}/logs/rollback.log"
DEPLOY_LOCK_FILE="${DEPLOY_PATH}/shared/deploy-in-progress.lock"
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
  local message="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "${message}" | tee -a "${LOG_FILE}"
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

get_previous_version() {
  local current_version="$1"
  local version
  local found_current=false

  while IFS= read -r version; do
    [[ -n "${version}" ]] || continue
    if [[ "${version}" == "${current_version}" ]]; then
      found_current=true
      continue
    fi
    if [[ "${found_current}" == true ]] && version_has_jar "${version}"; then
      echo "${version}"
      return 0
    fi
  done < <(list_versions_sorted)

  return 1
}

wait_for_healthy() {
  local elapsed=0

  while (( elapsed < HEALTH_TIMEOUT )); do
    if curl --silent --show-error --fail --max-time 5 "${HEALTH_URL}" >/dev/null; then
      return 0
    fi
    sleep "${HEALTH_CHECK_INTERVAL}"
    elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
  done

  return 1
}

mkdir -p "${DEPLOY_PATH}/logs"

main() {
  log "========================================="
  log "ON-FAILURE ROLLBACK TRIGGERED"
  log "========================================="

  if [[ -e "${DEPLOY_LOCK_FILE}" ]]; then
    log "Deployment lock present, skipping rollback to avoid racing an active deployment"
    exit 0
  fi

  local current_version previous_version
  current_version="$(get_current_version)"
  log "Current version: ${current_version}"

  log "Waiting for service to recover (${HEALTH_TIMEOUT}s timeout)..."
  if wait_for_healthy; then
    log "Service recovered successfully; rollback not needed"
    exit 0
  fi

  if ! previous_version="$(get_previous_version "${current_version}")"; then
    error "No previous runnable version available for rollback"
  fi

  log "Rolling back from ${current_version} to ${previous_version}"

  sudo systemctl stop "${SERVICE_NAME}" 2>/dev/null || true
  ln -sfn "$(version_dir "${previous_version}")" "${LATEST_VERSION}"
  sudo systemctl daemon-reload
  sudo systemctl reset-failed "${SERVICE_NAME}" >/dev/null 2>&1 || true
  sudo systemctl start "${SERVICE_NAME}"

  log "Verifying rollback..."
  sleep 10

  if wait_for_healthy; then
    log "Rollback completed successfully"
    log "Service is now running version: ${previous_version}"
    exit 0
  fi

  error "Rollback failed; manual intervention required"
}

main "$@"
