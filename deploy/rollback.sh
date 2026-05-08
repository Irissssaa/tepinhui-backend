#!/usr/bin/env bash

set -euo pipefail

# 应用配置
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"

# 路径配置
VERSIONS_DIR="${DEPLOY_PATH}/versions"
LATEST_VERSION="${VERSIONS_DIR}/latest"

# systemd 服务名称
SERVICE_NAME="${APP_NAME}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

error() {
  log "ERROR: $*" >&2
  exit 1
}

# 列出所有可用版本
list_versions() {
  log "Available versions:"
  log "========================================="

  local versions=()
  for dir in "${VERSIONS_DIR}"/*; do
    if [[ -d "$dir" && "$(basename "$dir")" != "latest" ]]; then
      versions+=("$(basename "$dir")")
    fi
  done

  # 按版本号排序（最新的在前）
  IFS=$'\n' sorted_versions=($(sort -r <<<"${versions[*]}"))
  unset IFS

  local current_version
  if [[ -L "${LATEST_VERSION}" ]]; then
    current_version="$(basename "$(readlink -f "${LATEST_VERSION}")")"
  else
    current_version="unknown"
  fi

  local count=0
  for version in "${sorted_versions[@]}"; do
    count=$((count + 1))
    local marker=""
    if [[ "${version}" == "${current_version}" ]]; then
      marker=" (current)"
    fi

    # 获取目录创建时间
    local created_time
    if [[ -f "${VERSIONS_DIR}/${version}/app.jar" ]]; then
      created_time="$(stat -c '%y' "${VERSIONS_DIR}/${version}/app.jar" 2>/dev/null | cut -d. -f1 || echo 'unknown')"
    else
      created_time="unknown"
    fi

    printf '%s. %s%s - Created: %s\n' "${count}" "${version}" "${marker}" "${created_time}"
  done

  log "========================================="
  log "Total versions: ${count}"
}

# 获取指定版本
get_version() {
  local version="$1"

  # 验证版本存在
  if [[ ! -d "${VERSIONS_DIR}/${version}" ]]; then
    error "Version does not exist: ${version}"
  fi

  if [[ ! -f "${VERSIONS_DIR}/${version}/app.jar" ]]; then
    error "JAR file not found in version: ${version}"
  fi
}

# 执行回滚
rollback_to_version() {
  local version="$1"

  log "Rolling back to version: ${version}"

  # 验证版本
  get_version "${version}"

  # 停止当前服务
  log "Stopping current service..."
  if sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
    sudo systemctl stop "${SERVICE_NAME}"
    sleep 3
  fi

  # 更新 latest 软链接
  log "Updating version symlink..."
  sudo ln -sfn "${VERSIONS_DIR}/${version}" "${LATEST_VERSION}"

  # 重新加载 systemd 配置
  log "Reloading systemd configuration..."
  sudo systemctl daemon-reload

  # 启动服务
  log "Starting service..."
  sudo systemctl start "${SERVICE_NAME}"

  # 等待服务启动
  log "Waiting for service to become healthy..."
  sleep 5

  # 健康检查
  local app_port="${APP_PORT:-8060}"
  local app_api_prefix="${APP_API_PREFIX:-/tph}"
  local app_health_endpoint="${APP_HEALTH_ENDPOINT:-/health}"

  # 正规范化路径
  if [[ -n "${app_api_prefix}" && "${app_api_prefix}" != "/" ]]; then
    app_api_prefix="/${app_api_prefix#/}"
  fi
  app_api_prefix="${app_api_prefix%/}"

  if [[ -n "${app_health_endpoint}" && "${app_health_endpoint}" != "/" ]]; then
    app_health_endpoint="/${app_health_endpoint#/}"
  fi
  app_health_endpoint="${app_health_endpoint%/}"

  local health_url="http://127.0.0.1:${app_port}${app_api_prefix}${app_health_endpoint}"
  local elapsed=0
  local timeout="${STARTUP_TIMEOUT:-120}"
  local interval="${HEALTH_CHECK_INTERVAL:-5}"

  while (( elapsed < timeout )); do
    if curl --silent --show-error --fail "${health_url}" >/dev/null; then
      log "Rollback completed successfully!"
      log "Service is running version: ${version}"
      return 0
    fi

    if ! sudo systemctl is-active "${SERVICE_NAME}" >/dev/null 2>&1; then
      log "Service exited unexpectedly after rollback."
      sudo journalctl -u "${SERVICE_NAME}" --no-pager -n 100
      return 1
    fi

    sleep "${interval}"
    elapsed=$((elapsed + interval))
  done

  log "Health check timed out after ${timeout}s"
  sudo journalctl -u "${SERVICE_NAME}" --no-pager -n 100
  return 1
}

# 显示当前状态
show_current_status() {
  if [[ ! -L "${LATEST_VERSION}" ]]; then
    log "No current version deployed"
    return 1
  fi

  local current_version
  current_version="$(basename "$(readlink -f "${LATEST_VERSION}")")"

  log "Current deployed version: ${current_version}"
  log "Service status: $(sudo systemctl is-active "${SERVICE_NAME}" 2>/dev/null || echo 'unknown')"
}

# 显示帮助信息
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

# 解析命令行参数
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

# 如果没有参数，显示帮助
show_help
