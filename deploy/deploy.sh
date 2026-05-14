#!/usr/bin/env bash

set -euo pipefail

# 应用配置
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"
VERSION="${VERSION:-$(date +%Y%m%d-%H%M%S)}"

# Java 版本配置
JAVA_VERSION="${JAVA_VERSION:-17}"

# 服务配置
APP_PORT="${APP_PORT:-8060}"
APP_API_PREFIX="${APP_API_PREFIX:-/tph}"
APP_HEALTH_ENDPOINT="${APP_HEALTH_ENDPOINT:-/health}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-5}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-120}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
APP_ENV_FILE="${APP_ENV_FILE:-${DEPLOY_PATH}/shared/app.env}"

# 路径配置
VERSIONS_DIR="${DEPLOY_PATH}/versions"
LATEST_VERSION="${VERSIONS_DIR}/latest"
JAR_FILE="${LATEST_VERSION}/app.jar"
LOG_DIR="${DEPLOY_PATH}/logs"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
MAX_VERSIONS="${MAX_VERSIONS:-5}"
DEPLOY_LOCK_FILE="${VERSIONS_DIR}/.deploy-in-progress.lock"

# systemd 服务名称
SERVICE_NAME="${APP_NAME}"
ROLLBACK_SERVICE_NAME="${SERVICE_NAME}-rollback"

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

refresh_runtime_config() {
  APP_API_PREFIX="$(normalize_path "${APP_API_PREFIX}")"
  APP_HEALTH_ENDPOINT="$(normalize_path "${APP_HEALTH_ENDPOINT}")"
  HEALTH_URL="http://127.0.0.1:${APP_PORT}${APP_API_PREFIX}${APP_HEALTH_ENDPOINT}"
}

parse_env_file() {
  local env_file="$1"
  local line key value quote

  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"

    [[ -z "${line//[[:space:]]/}" ]] && continue
    [[ "${line}" =~ ^[[:space:]]*# ]] && continue

    if [[ "${line}" =~ ^[[:space:]]*(export[[:space:]]+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      key="${BASH_REMATCH[2]}"
      value="${BASH_REMATCH[3]}"

      if [[ ${#value} -ge 2 ]]; then
        quote="${value:0:1}"
        if [[ ( "${quote}" == '"' || "${quote}" == "'" ) && "${value: -1}" == "${quote}" ]]; then
          value="${value:1:${#value}-2}"
        fi
      fi

      printf -v "${key}" '%s' "${value}"
      export "${key}"
    fi
  done < "${env_file}"
}

load_app_env() {
  if [[ -f "${APP_ENV_FILE}" ]]; then
    log "Loading application environment from ${APP_ENV_FILE}"
    parse_env_file "${APP_ENV_FILE}"
  fi

  refresh_runtime_config
}

refresh_runtime_config

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

error() {
  log "ERROR: $*" >&2
  exit 1
}

check_commands() {
  local required=(curl find sort stat systemctl tar)
  local cmd
  for cmd in "${required[@]}"; do
    if ! command -v "${cmd}" >/dev/null 2>&1; then
      error "Missing required command: ${cmd}"
    fi
  done
}

get_current_version() {
  if [[ -L "${LATEST_VERSION}" ]]; then
    basename "$(readlink "${LATEST_VERSION}")"
  fi
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

cleanup_deploy_lock() {
  rm -f "${DEPLOY_LOCK_FILE}"
}

acquire_deploy_lock() {
  mkdir -p "${VERSIONS_DIR}"
  if [[ -e "${DEPLOY_LOCK_FILE}" ]]; then
    error "Another deployment appears to be in progress: ${DEPLOY_LOCK_FILE}"
  fi

  printf '%s\n' "${VERSION}" > "${DEPLOY_LOCK_FILE}"
  trap cleanup_deploy_lock EXIT
}

create_service_unit() {
  local service_file="/etc/systemd/system/${SERVICE_NAME}.service"
  local rollback_service_file="/etc/systemd/system/${ROLLBACK_SERVICE_NAME}.service"

  log "Writing systemd service unit: ${service_file}"
  sudo tee "${service_file}" >/dev/null <<EOF
[Unit]
Description=Tepinhui Backend Application
After=network.target
StartLimitIntervalSec=60
StartLimitBurst=3
OnFailure=${ROLLBACK_SERVICE_NAME}.service

[Service]
Type=simple
User=tph
WorkingDirectory=${LATEST_VERSION}
Environment="JAVA_OPTS=${JAVA_OPTS}"
EnvironmentFile=-${APP_ENV_FILE}
ExecStart=/bin/bash -lc 'exec /usr/bin/java \$JAVA_OPTS -jar ${JAR_FILE}'
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

  log "Writing rollback unit: ${rollback_service_file}"
  sudo tee "${rollback_service_file}" >/dev/null <<EOF
[Unit]
Description=Tepinhui Backend automatic rollback
After=network.target

[Service]
Type=oneshot
ExecStart=${DEPLOY_PATH}/scripts/rollback-on-failure.sh
StandardOutput=journal
StandardError=journal
EOF

  sudo systemctl daemon-reload
  sudo systemctl enable "${SERVICE_NAME}" >/dev/null 2>&1 || true
}

stop_service() {
  if sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
    log "Stopping existing service: ${SERVICE_NAME}"
    sudo systemctl stop "${SERVICE_NAME}"
    sleep 3
  fi
}

start_service() {
  log "Starting service: ${SERVICE_NAME}"
  sudo systemctl reset-failed "${SERVICE_NAME}" >/dev/null 2>&1 || true
  sudo systemctl start "${SERVICE_NAME}"

  log "Waiting for service to become healthy..."
  sleep 5
}

health_check() {
  local elapsed=0

  while (( elapsed < STARTUP_TIMEOUT )); do
    if curl --silent --show-error --fail "${HEALTH_URL}" >/dev/null; then
      log "Service is healthy and ready"
      return 0
    fi

    if ! sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
      log "Service exited unexpectedly. Checking logs..."
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

switch_latest_symlink() {
  local version="$1"
  log "Updating latest symlink to version: ${version}"
  ln -sfn "$(version_dir "${version}")" "${LATEST_VERSION}"
}

cleanup_old_versions() {
  log "Cleaning up old versions (keeping ${MAX_VERSIONS})"

  local keep_count=0
  local current_version
  local version
  local preserved=()

  current_version="$(get_current_version || true)"
  preserved+=("${VERSION}")
  if [[ -n "${current_version}" && "${current_version}" != "${VERSION}" ]]; then
    preserved+=("${current_version}")
  fi

  while IFS= read -r version; do
    [[ -n "${version}" ]] || continue

    if printf '%s\n' "${preserved[@]}" | grep -Fxq "${version}"; then
      keep_count=$((keep_count + 1))
      continue
    fi

    if (( keep_count < MAX_VERSIONS )); then
      keep_count=$((keep_count + 1))
      continue
    fi

    log "Removing old version: ${version}"
    rm -rf "$(version_dir "${version}")"
  done < <(list_versions_sorted)
}

show_status() {
  log "========================================="
  log "Deployment Status"
  log "========================================="
  log "Application: ${APP_NAME}"
  log "Version: ${VERSION}"
  log "Current version: $(get_current_version || echo 'none')"
  log "Service: ${SERVICE_NAME}"
  log "Status: $(sudo systemctl is-active "${SERVICE_NAME}" 2>/dev/null || echo 'unknown')"
  log "Health URL: ${HEALTH_URL}"
  log "========================================="
}

verify_rollback_scripts() {
  local scripts_dir="${DEPLOY_PATH}/scripts"
  local rollback_script="${scripts_dir}/rollback-on-failure.sh"
  local manual_rollback_script="${DEPLOY_PATH}/rollback.sh"

  log "Verifying rollback scripts..."

  if [[ ! -f "${rollback_script}" ]]; then
    error "Rollback script not found: ${rollback_script}. Please check if GitHub Actions uploaded it correctly."
  fi

  if [[ ! -f "${manual_rollback_script}" ]]; then
    error "Manual rollback script not found: ${manual_rollback_script}. Please check if GitHub Actions uploaded it correctly."
  fi

  chmod +x "${rollback_script}" "${manual_rollback_script}"

  log "Rollback scripts verified successfully"
}

setup_java() {
  log "Checking Java version..."

  if command -v java >/dev/null 2>&1; then
    local current_version
    current_version="$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)"
    if [[ "${current_version}" == "${JAVA_VERSION}" ]]; then
      log "Java ${JAVA_VERSION} is already configured"
      return 0
    fi
  fi

  local java_path=""
  local search_paths=("/usr/lib/jvm" "/usr/local/lib/jvm" "/opt/java" "/usr/lib/jvm-")
  local search_path

  for search_path in "${search_paths[@]}"; do
    if [[ -d "${search_path}" ]]; then
      java_path="$(find "${search_path}" -name "java" -path "*/bin/java" 2>/dev/null | grep -E "java-${JAVA_VERSION}|jdk-${JAVA_VERSION}|openjdk-${JAVA_VERSION}" | head -1 || true)"
      if [[ -n "${java_path}" ]]; then
        break
      fi
    fi
  done

  if [[ -z "${java_path}" ]] && command -v update-alternatives >/dev/null 2>&1; then
    java_path="$(update-alternatives --list java 2>/dev/null | grep -E "java-${JAVA_VERSION}|jdk-${JAVA_VERSION}|openjdk-${JAVA_VERSION}" | head -1 || true)"
  fi

  if [[ -z "${java_path}" ]]; then
    error "Java ${JAVA_VERSION} not found. Please install Java ${JAVA_VERSION}:\n  Ubuntu/Debian: sudo apt install openjdk-${JAVA_VERSION}-jdk\n  CentOS/RHEL: sudo yum install java-${JAVA_VERSION}-openjdk-devel"
  fi

  export JAVA_HOME
  JAVA_HOME="$(dirname "$(dirname "${java_path}")")"
  export PATH="${JAVA_HOME}/bin:${PATH}"

  log "Using Java: $(java -version 2>&1 | head -n 1)"
}

verify_deployment_environment() {
  log "Verifying deployment environment..."

  local version_dir_path
  version_dir_path="$(version_dir "${VERSION}")"
  if [[ ! -d "${version_dir_path}" ]]; then
    error "Version directory does not exist: ${version_dir_path}"
  fi

  local tar_file="${version_dir_path}/source-code.tar.gz"
  if [[ ! -f "${tar_file}" ]]; then
    error "Source code package not found at: ${tar_file}"
  fi

  log "Deployment environment verified successfully"
}

build_application() {
  log "Building application..."

  local version_dir_path
  local jar_file_path
  version_dir_path="$(version_dir "${VERSION}")"

  mkdir -p "${version_dir_path}"
  cd "${version_dir_path}"

  if [[ ! -f "./mvnw" ]]; then
    error "mvnw not found in ${version_dir_path}. Make sure source code is extracted correctly."
  fi

  chmod +x mvnw

  if ! ./mvnw -B clean package -DskipTests; then
    error "Maven build failed. Check the build logs above for errors."
  fi

  if [[ -f "target/${APP_NAME}.jar" ]]; then
    jar_file_path="target/${APP_NAME}.jar"
  else
    jar_file_path="$(find target -type f -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -n 1 || true)"
    if [[ -z "${jar_file_path}" ]]; then
      error "Build failed - no runnable JAR file found in target directory"
    fi
    log "Using JAR: ${jar_file_path}"
  fi

  cp "${jar_file_path}" "${version_dir_path}/app.jar"
  log "Build completed successfully"
}

extract_source_code() {
  local version_dir_path
  local tar_file
  version_dir_path="$(version_dir "${VERSION}")"
  tar_file="${version_dir_path}/source-code.tar.gz"

  log "Extracting source code..."
  log "Looking for source package at: ${tar_file}"

  if [[ -d "${version_dir_path}" ]]; then
    log "Version directory contents:"
    ls -lh "${version_dir_path}" 2>/dev/null || log "Directory is empty"
  else
    error "Version directory does not exist: ${version_dir_path}"
  fi

  if [[ ! -f "${tar_file}" ]]; then
    error "Source code package not found: ${tar_file}"
  fi

  log "Source package found, size: $(du -h "${tar_file}" | cut -f1)"

  mkdir -p "${version_dir_path}"

  if [[ -f "${version_dir_path}/pom.xml" ]]; then
    log "Cleaning up existing source code in version directory..."
    rm -rf \
      "${version_dir_path}/src" \
      "${version_dir_path}/pom.xml" \
      "${version_dir_path}/.mvn" \
      "${version_dir_path}/mvnw" \
      "${version_dir_path}/mvnw.cmd"
  fi

  if ! tar xzf "${tar_file}" -C "${version_dir_path}"; then
    error "Failed to extract source code"
  fi

  if [[ ! -f "${version_dir_path}/pom.xml" ]]; then
    log "Listing version directory after extraction:"
    ls -lhR "${version_dir_path}" | head -30
    error "pom.xml not found after extraction. Source code may be corrupted."
  fi

  rm -f "${tar_file}"
  log "Deleted source package to save space"

  log "Source code extracted successfully to ${version_dir_path}"
}

cleanup_loose_files() {
  log "Cleaning up loose files in versions directory..."

  find "${VERSIONS_DIR}" -maxdepth 1 -type f \( -name "*.tar.gz" -o -name "*.jar" -o -name "*.log" \) | while read -r file; do
    log "Removing loose file: ${file}"
    rm -f "${file}"
  done

  log "Loose files cleanup completed"
}

rollback_failed_deployment() {
  local previous_version="$1"

  log "Deployment health check failed for version: ${VERSION}"

  if [[ -z "${previous_version}" || "${previous_version}" == "${VERSION}" ]]; then
    error "Deployment failed and no previous healthy version is available for rollback"
  fi

  if ! version_has_jar "${previous_version}"; then
    error "Deployment failed and previous version is not runnable: ${previous_version}"
  fi

  log "Rolling back to previous version: ${previous_version}"
  sudo systemctl stop "${SERVICE_NAME}" >/dev/null 2>&1 || true
  switch_latest_symlink "${previous_version}"
  start_service

  if health_check; then
    error "Deployment failed and was rolled back to ${previous_version}"
  fi

  error "Deployment failed and automatic rollback to ${previous_version} also failed"
}

main() {
  log "Starting deployment of version: ${VERSION}"

  check_commands
  acquire_deploy_lock
  cleanup_loose_files
  setup_java
  verify_deployment_environment

  mkdir -p "${LOG_DIR}" "$(version_dir "${VERSION}")"

  extract_source_code
  build_application
  create_service_unit
  verify_rollback_scripts

  load_app_env

  local previous_version=""
  previous_version="$(get_current_version || true)"

  stop_service
  switch_latest_symlink "${VERSION}"
  start_service

  if health_check; then
    cleanup_old_versions
    show_status
    log "Deployment completed successfully!"
    exit 0
  fi

  rollback_failed_deployment "${previous_version}"
}

show_help() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Deploy Tepinhui Backend application.

Options:
  -v, --version VERSION    Deployment version (default: auto-generated)
  -p, --path PATH          Deployment path (default: /home/tph)
  -s, --show-status        Show current service status
  -h, --help               Show this help message

Environment Variables:
  APP_NAME                 Application name (default: tepinhui-backend)
  APP_PORT                 Application port (default: 8060)
  JAVA_OPTS                Java JVM options (default: -Xms256m -Xmx512m)
  MAX_VERSIONS             Maximum number of versions to keep (default: 5)

Examples:
  $(basename "$0")                              # Deploy with auto version
  $(basename "$0") --version 20260508-abc123    # Deploy specific version
  $(basename "$0") --show-status                # Show service status
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      VERSION="$2"
      shift 2
      ;;
    -p|--path)
      DEPLOY_PATH="$2"
      VERSIONS_DIR="${DEPLOY_PATH}/versions"
      LATEST_VERSION="${VERSIONS_DIR}/latest"
      JAR_FILE="${LATEST_VERSION}/app.jar"
      LOG_DIR="${DEPLOY_PATH}/logs"
      LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
      APP_ENV_FILE="${APP_ENV_FILE:-${DEPLOY_PATH}/shared/app.env}"
      DEPLOY_LOCK_FILE="${VERSIONS_DIR}/.deploy-in-progress.lock"
      shift 2
      ;;
    -s|--show-status)
      show_status
      exit 0
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

main
