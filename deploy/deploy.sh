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

  # 清理版本目录中散落的文件（如 source-code.tar.gz）
  log "Cleaning up loose files in versions directory..."
  find "${VERSIONS_DIR}" -maxdepth 1 -type f -name "*.tar.gz" -o -name "*.jar" | while read -r file; do
    log "Removing loose file: ${file}"
    rm -f "${file}"
  done
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

# 检查并设置 Java 环境
setup_java() {
  log "Checking Java version..."

  # 检查当前 java 版本
  if command -v java >/dev/null 2>&1; then
    local current_version
    current_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ "${current_version}" == "${JAVA_VERSION}" ]]; then
      log "Java ${JAVA_VERSION} is already configured"
      return 0
    fi
  fi

  # 查找指定版本的 Java
  local java_path=""
  local search_paths=("/usr/lib/jvm" "/usr/local/lib/jvm" "/opt/java" "/usr/lib/jvm-")

  for search_path in "${search_paths[@]}"; do
    if [[ -d "${search_path}" ]]; then
      java_path=$(find "${search_path}" -name "java" -path "*/bin/java" 2>/dev/null | grep -E "java-${JAVA_VERSION}|jdk-${JAVA_VERSION}|openjdk-${JAVA_VERSION}" | head -1)
      if [[ -n "${java_path}" ]]; then
        break
      fi
    fi
  done

  # 尝试使用 update-alternatives
  if [[ -z "${java_path}" ]] && command -v update-alternatives >/dev/null 2>&1; then
    java_path=$(update-alternatives --list java 2>/dev/null | grep -E "java-${JAVA_VERSION}|jdk-${JAVA_VERSION}|openjdk-${JAVA_VERSION}" | head -1)
  fi

  if [[ -z "${java_path}" ]]; then
    error "Java ${JAVA_VERSION} not found. Please install Java ${JAVA_VERSION}:\n  Ubuntu/Debian: sudo apt install openjdk-${JAVA_VERSION}-jdk\n  CentOS/RHEL: sudo yum install java-${JAVA_VERSION}-openjdk-devel"
  fi

  export JAVA_HOME=$(dirname $(dirname "$java_path"))
  export PATH="${JAVA_HOME}/bin:$PATH"

  log "Using Java: $(java -version 2>&1 | head -n 1)"
}

# 验证部署环境
verify_deployment_environment() {
  log "Verifying deployment environment..."

  # 检查版本目录是否存在
  local version_dir="${VERSIONS_DIR}/${VERSION}"
  if [[ ! -d "${version_dir}" ]]; then
    error "Version directory does not exist: ${version_dir}"
  fi

  # 检查源代码包是否存在
  local tar_file="${version_dir}/source-code.tar.gz"
  if [[ ! -f "${tar_file}" ]]; then
    error "Source code package not found at: ${tar_file}"
  fi

  log "Deployment environment verified successfully"
}

# 构建应用程序
build_application() {
  log "Building application..."

  # 确保 VERSION 目录存在
  local version_dir="${VERSIONS_DIR}/${VERSION}"
  mkdir -p "${version_dir}"

  # 切换到源代码目录
  cd "${version_dir}"

  # 检查 mvnw 是否存在
  if [[ ! -f "./mvnw" ]]; then
    error "mvnw not found in ${version_dir}. Make sure source code is extracted correctly."
  fi

  # 确保 Maven Wrapper 可执行
  chmod +x mvnw

  # 构建项目（跳过测试）
  if ! ./mvnw -B clean package -DskipTests; then
    error "Maven build failed. Check the build logs above for errors."
  fi

  # 检查构建结果
  if [[ ! -f "target/${APP_NAME}.jar" ]]; then
    # 查找所有 JAR 文件
    JAR_FILES=$(find target -name "*.jar" -type f 2>/dev/null | head -n 5)
    if [[ -z "${JAR_FILES}" ]]; then
      error "Build failed - no JAR file found in target directory"
    fi
    # 使用找到的第一个 JAR
    JAR_FILE_PATH=$(echo "$JAR_FILES" | head -n 1)
    log "Using JAR: ${JAR_FILE_PATH}"
  else
    JAR_FILE_PATH="target/${APP_NAME}.jar"
  fi

  # 复制 JAR 到版本目录根目录
  cp "${JAR_FILE_PATH}" "${version_dir}/app.jar"
  log "Build completed successfully"
}

# 解压源代码包
extract_source_code() {
  local version_dir="${VERSIONS_DIR}/${VERSION}"
  local tar_file="${version_dir}/source-code.tar.gz"

  log "Extracting source code..."
  log "Looking for source package at: ${tar_file}"

  # 列出版本目录内容（用于调试）
  if [[ -d "${version_dir}" ]]; then
    log "Version directory contents:"
    ls -lh "${version_dir}" 2>/dev/null || log "Directory is empty"
  else
    error "Version directory does not exist: ${version_dir}"
  fi

  if [[ ! -f "${tar_file}" ]]; then
    error "Source code package not found: ${tar_file}"
  fi

  log "Source package found, size: $(du -h "${tar_file}" | cut -f1)"

  # 确保版本目录存在
  mkdir -p "${version_dir}"

  # 清理解压后可能存在的旧文件
  if [[ -f "${version_dir}/pom.xml" ]]; then
    log "Cleaning up existing source code in version directory..."
    rm -rf "${version_dir}/src" "${version_dir}/pom.xml" "${version_dir}/.mvn" "${version_dir}/mvnw" "${version_dir}/mvnw.cmd"
  fi

  # 解压到版本目录（使用 -C 参数）
  if ! tar xzf "${tar_file}" -C "${version_dir}"; then
    error "Failed to extract source code"
  fi

  # 验证关键文件是否存在
  if [[ ! -f "${version_dir}/pom.xml" ]]; then
    log "Listing version directory after extraction:"
    ls -lhR "${version_dir}" | head -30
    error "pom.xml not found after extraction. Source code may be corrupted."
  fi

  # 删除源代码包（节省空间）
  rm -f "${tar_file}"
  log "Deleted source package to save space"

  log "Source code extracted successfully to ${version_dir}"
}

# 清理版本目录中的散落文件
cleanup_loose_files() {
  log "Cleaning up loose files in versions directory..."

  # 查找并删除散落在版本目录中的文件（不是目录）
  find "${VERSIONS_DIR}" -maxdepth 1 -type f \( -name "*.tar.gz" -o -name "*.jar" -o -name "*.log" \) | while read -r file; do
    log "Removing loose file: ${file}"
    rm -f "${file}"
  done

  log "Loose files cleanup completed"
}

# 主执行流程
main() {
  log "Starting deployment of version: ${VERSION}"

  # 检查必要命令
  check_commands

  # 清理版本目录中的散落文件
  cleanup_loose_files

  # 检查并设置 Java 环境
  setup_java

  # 验证部署环境
  verify_deployment_environment

  # 创建必要的目录
  mkdir -p "${LOG_DIR}" "${VERSIONS_DIR}/${VERSION}"

  # 解压源代码包
  extract_source_code

  # 构建应用程序
  build_application

  # 检查 JAR 文件
  local version_dir="${VERSIONS_DIR}/${VERSION}"
  if [[ ! -f "${version_dir}/app.jar" ]]; then
    error "JAR file not found: ${version_dir}/app.jar"
  fi

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
