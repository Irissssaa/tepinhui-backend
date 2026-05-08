#!/usr/bin/env bash

set -euo pipefail

# 应用配置
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"
VERSION="${VERSION:-$(date +%Y%m%d-%H%M%S)}"

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

# 检查必要命令
check_commands() {
  for cmd in systemctl; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
      error "Missing required command: $cmd"
    fi
  done
}

# 创建 systemd 服务单元
create_service_unit() {
  local service_file="/etc/systemd/system/${SERVICE_NAME}.service"

  log "Creating systemd service unit: ${service_file}"
  sudo tee "${service_file}" >/dev/null <<EOF
[Unit]
Description=Tepinhui Backend Application
After=network.target

[Service]
Type=simple
User=tph
WorkingDirectory=${LATEST_VERSION}
EnvironmentFile=${APP_ENV_FILE}
ExecStart=/usr/bin/java ${JAVA_OPTS} -jar ${JAR_FILE}
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

  sudo systemctl daemon-reload
}

# 停止现有服务
stop_service() {
  if sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
    log "Stopping existing service: ${SERVICE_NAME}"
    sudo systemctl stop "${SERVICE_NAME}"
    sleep 3
  fi
}

# 启动服务
start_service() {
  log "Starting service: ${SERVICE_NAME}"
  sudo systemctl start "${SERVICE_NAME}"

  log "Waiting for service to become healthy..."
  sleep 5
}

# 健康检查
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

# 清理旧版本
cleanup_old_versions() {
  log "Cleaning up old versions (keeping ${MAX_VERSIONS})"

  local versions=()
  for dir in "${VERSIONS_DIR}"/*; do
    if [[ -d "$dir" && "$(basename "$dir")" != "latest" ]]; then
      versions+=("$(basename "$dir")")
    fi
  done

  # 按版本号排序（最新的在前）
  IFS=$'\n' sorted_versions=($(sort -r <<<"${versions[*]}"))
  unset IFS

  local count=${#sorted_versions[@]}
  if (( count > MAX_VERSIONS )); then
    for (( i=MAX_VERSIONS; i<count; i++ )); do
      local old_version="${sorted_versions[$i]}"
      log "Removing old version: ${old_version}"
      rm -rf "${VERSIONS_DIR}/${old_version}"
    done
  fi
}

# 显示状态信息
show_status() {
  log "========================================="
  log "Deployment Status"
  log "========================================="
  log "Application: ${APP_NAME}"
  log "Version: ${VERSION}"
  log "Service: ${SERVICE_NAME}"
  log "Status: $(sudo systemctl is-active "${SERVICE_NAME}" 2>/dev/null || echo 'unknown')"
  log "Health URL: ${HEALTH_URL}"
  log "========================================="
}

# 主执行流程
main() {
  log "Starting deployment of version: ${VERSION}"

  # 检查必要命令
  check_commands

  # 检查 JAR 文件
  if [[ ! -f "${JAR_FILE}" ]]; then
    error "JAR file not found: ${JAR_FILE}"
  fi

  # 创建必要的目录
  mkdir -p "${LOG_DIR}"

  # 创建 systemd 服务单元（如果不存在）
  if [[ ! -f "/etc/systemd/system/${SERVICE_NAME}.service" ]]; then
    create_service_unit
  fi

  # 加载环境变量
  if [[ -f "${APP_ENV_FILE}" ]]; then
    log "Loading application environment from ${APP_ENV_FILE}"
    set -a
    . "${APP_ENV_FILE}"
    set +a
  fi

  # 停止现有服务
  stop_service

  # 启动服务
  start_service

  # 健康检查
  if health_check; then
    # 清理旧版本
    cleanup_old_versions

    # 显示状态
    show_status

    log "Deployment completed successfully!"
    exit 0
  else
    error "Deployment failed - health check failed"
  fi
}

# 显示帮助信息
show_help() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Deploy Tepinhui Backend application.

Options:
  -v, --version VERSION    Deployment version (default: auto-generated)
  -p, --path PATH         Deployment path (default: /home/tph)
  -s, --show-status       Show current service status
  -h, --help              Show this help message

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

# 解析命令行参数
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
