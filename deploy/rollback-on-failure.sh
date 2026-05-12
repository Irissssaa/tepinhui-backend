#!/usr/bin/env bash

# 当服务启动失败达到次数限制时，systemd 会自动执行此脚本
# 该脚本会回退到上一个稳定版本并重启服务

set -euo pipefail

# 应用配置（使用硬编码默认值，因为 systemd FailureAction 不继承服务环境变量）
APP_NAME="${APP_NAME:-tepinhui-backend}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8060/tph/health}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60}"

# 路径配置
VERSIONS_DIR="${DEPLOY_PATH}/versions"
LATEST_VERSION="${VERSIONS_DIR}/latest"
LOG_FILE="${DEPLOY_PATH}/logs/rollback.log"

log() {
  local message="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "${message}" | tee -a "${LOG_FILE}"
}

error() {
  log "ERROR: $*" >&2
  exit 1
}

# 获取当前版本
get_current_version() {
  if [[ -L "${LATEST_VERSION}" ]]; then
    basename "$(readlink -f "${LATEST_VERSION}")"
  else
    echo "unknown"
  fi
}

# 获取上一个稳定版本
get_previous_version() {
  local current_version="$1"
  local versions=()

  for dir in "${VERSIONS_DIR}"/*; do
    if [[ -d "$dir" && "$(basename "$dir")" != "latest" ]]; then
      versions+=("$(basename "$dir")")
    fi
  done

  # 按版本号排序（最新的在前）
  IFS=$'\n' sorted_versions=($(sort -r <<<"${versions[*]}"))
  unset IFS

  # 找到当前版本的前一个
  local found_current=false
  for version in "${sorted_versions[@]}"; do
    if [[ "${version}" == "${current_version}" ]]; then
      found_current=true
      continue
    fi
    if [[ "${found_current}" == true ]]; then
      echo "${version}"
      return 0
    fi
  done

  return 1
}

# 验证服务健康状态
wait_for_healthy() {
  local url="${HEALTH_URL:-http://127.0.0.1:8060/tph/health}"
  local timeout="${HEALTH_TIMEOUT:-60}"
  local interval=5
  local elapsed=0

  while (( elapsed < timeout )); do
    if curl --silent --show-error --fail --max-time 5 "${url}" >/dev/null; then
      return 0
    fi
    sleep "${interval}"
    elapsed=$((elapsed + interval))
  done

  return 1
}

# 确保日志目录存在
mkdir -p "${DEPLOY_PATH}/logs"

# 主逻辑
main() {
  log "========================================="
  log "FAILURE ACTION TRIGGERED"
  log "========================================="

  local current_version
  current_version="$(get_current_version)"

  log "Current version: ${current_version}"

  # 等待服务启动（给它一个机会）
  log "Waiting for service to become healthy (60s timeout)..."
  if wait_for_healthy; then
    log "Service recovered successfully!"
    exit 0
  fi

  log "Service failed to become healthy. Starting rollback..."

  # 获取上一个版本
  local previous_version
  if ! previous_version="$(get_previous_version "${current_version}")"; then
    error "No previous version available for rollback"
  fi

  log "Rolling back from ${current_version} to ${previous_version}"

  # 停止当前服务
  log "Stopping current service..."
  sudo systemctl stop "${APP_NAME}" 2>/dev/null || true

  # 更新软链接
  log "Updating version symlink..."
  sudo ln -sfn "${VERSIONS_DIR}/${previous_version}" "${LATEST_VERSION}"

  # 重新加载 systemd
  sudo systemctl daemon-reload

  # 启动服务
  log "Starting service with version ${previous_version}..."
  sudo systemctl start "${APP_NAME}"

  # 验证回退
  log "Verifying rollback..."
  sleep 10

  if wait_for_healthy; then
    log "Rollback completed successfully!"
    log "Service is now running version: ${previous_version}"
  else
    log "CRITICAL: Rollback failed! Manual intervention required."
    exit 1
  fi
}

main "$@"
