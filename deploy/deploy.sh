#!/usr/bin/env bash

set -euo pipefail

# 应用配置
APP_NAME="${APP_NAME:-tepinhui-backend}"
REPO_URL="${REPO_URL:?REPO_URL is required}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/tph}"

# 服务配置
APP_PORT="${APP_PORT:-8060}"
# API 前缀：所有 REST 端点的 URL 都会加上此前缀
# 例如: /tph/auth/login, /tph/products
APP_API_PREFIX="${APP_API_PREFIX:-/tph}"
# 健康检查端点路径（相对于 API 前缀）
# 实际健康检查 URL: http://127.0.0.1:${APP_PORT}${APP_API_PREFIX}${APP_HEALTH_ENDPOINT}
APP_HEALTH_ENDPOINT="${APP_HEALTH_ENDPOINT:-/health}"

# 部署配置
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-120}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-5}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
MVN_BUILD_ARGS="${MVN_BUILD_ARGS:-clean package -DskipTests}"
APP_ENV_FILE="${APP_ENV_FILE:-${DEPLOY_PATH}/shared/app.env}"

REPO_DIR="${DEPLOY_PATH}/repo"
CURRENT_DIR="${DEPLOY_PATH}/current"
LOG_DIR="${DEPLOY_PATH}/logs"
RUN_DIR="${DEPLOY_PATH}/run"
PID_FILE="${RUN_DIR}/${APP_NAME}.pid"
JAR_FILE="${CURRENT_DIR}/${APP_NAME}.jar"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"

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

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "missing required command: $1"
    exit 1
  fi
}

wait_for_exit() {
  local pid="$1"

  for _ in $(seq 1 30); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  kill -9 "${pid}" >/dev/null 2>&1 || true
}

stop_existing_app() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
      log "stopping existing process ${pid}"
      kill "${pid}" >/dev/null 2>&1 || true
      wait_for_exit "${pid}"
    fi
    rm -f "${PID_FILE}"
  fi

  if pgrep -f "${JAR_FILE}" >/dev/null 2>&1; then
    local matched_pid
    matched_pid="$(pgrep -f "${JAR_FILE}" | head -n 1)"
    log "stopping matched process ${matched_pid}"
    kill "${matched_pid}" >/dev/null 2>&1 || true
    wait_for_exit "${matched_pid}"
  fi
}

find_built_jar() {
  find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1
}

require_command git
require_command java
require_command curl

mkdir -p "${DEPLOY_PATH}" "${CURRENT_DIR}" "${LOG_DIR}" "${RUN_DIR}" "${DEPLOY_PATH}/shared"

if [[ ! -d "${REPO_DIR}/.git" ]]; then
  log "cloning repository ${REPO_URL} to ${REPO_DIR}"
  git clone --branch "${DEPLOY_BRANCH}" "${REPO_URL}" "${REPO_DIR}"
fi

cd "${REPO_DIR}"

log "syncing branch ${DEPLOY_BRANCH}"
git fetch --all --prune
git checkout "${DEPLOY_BRANCH}"
git pull --ff-only origin "${DEPLOY_BRANCH}"

if [[ -f "${APP_ENV_FILE}" ]]; then
  log "loading application env from ${APP_ENV_FILE}"
  set -a
  # shellcheck disable=SC1090
  . "${APP_ENV_FILE}"
  set +a
fi

chmod +x mvnw

log "building application"
./mvnw -B ${MVN_BUILD_ARGS}

BUILT_JAR="$(find_built_jar)"
if [[ -z "${BUILT_JAR}" ]]; then
  log "jar file not found under ${REPO_DIR}/target"
  exit 1
fi

cp "${BUILT_JAR}" "${JAR_FILE}"

stop_existing_app

log "starting ${APP_NAME}"
nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" >> "${LOG_FILE}" 2>&1 &
NEW_PID=$!
echo "${NEW_PID}" > "${PID_FILE}"

log "health checking ${HEALTH_URL}"
elapsed=0
while (( elapsed < STARTUP_TIMEOUT )); do
  if curl --silent --show-error --fail "${HEALTH_URL}" >/dev/null; then
    log "deployment succeeded, app is healthy"
    exit 0
  fi

  if ! kill -0 "${NEW_PID}" >/dev/null 2>&1; then
    log "application exited unexpectedly, see ${LOG_FILE}"
    tail -n 100 "${LOG_FILE}" || true
    rm -f "${PID_FILE}"
    exit 1
  fi

  sleep "${HEALTH_CHECK_INTERVAL}"
  elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
done

log "health check timed out after ${STARTUP_TIMEOUT}s"
kill "${NEW_PID}" >/dev/null 2>&1 || true
wait_for_exit "${NEW_PID}"
rm -f "${PID_FILE}"
tail -n 100 "${LOG_FILE}" || true
exit 1
