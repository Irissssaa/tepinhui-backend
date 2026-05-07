# tepinhui-backend

## API 路径说明

所有 REST 端点的 URL 都会加上前缀 `/tph`：
- 认证: `POST /tph/auth/login`, `POST /tph/auth/register`
- 商品: `GET /tph/products`
- 健康检查: `GET /tph/health`

可通过环境变量 `APP_API_PREFIX` 自定义前缀（如 `/api` 或 `/`）。

## Health Check

- Default backend prefix: `app.api-prefix=/tph`
- Default health endpoint: `app.health-endpoint=/health`
- Full default URL: `GET /tph/health`
- Response: HTTP `200` when the service process is ready to receive requests.

**健康检查返回示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "tepinhui-backend",
    "timestamp": "2026-05-07T10:30:00Z",
    "port": 8060,
    "context-path": "/tph",
    "health-path": "/tph/health"
  }
}
```

## Auto Deploy

Repository includes:

- `deploy/deploy.sh`: server-side deploy script. It clones or pulls the repo, builds with Maven, restarts the app process, and waits for health check success.
- `.github/workflows/deploy.yml`: deploys automatically when `main` receives a push.
- `deploy/app.env.example`: example runtime environment file for server-side application secrets.

Simplified configuration split:

- GitHub `Secrets` only stores `SSH_PRIVATE_KEY`.
- GitHub `Variables` stores server host, user, port, deploy path, repo URL and application runtime defaults.

### Server prerequisites

Install these on the deployment server:

- `git`
- `java` 17
- `curl`

Create the runtime env file on the server, for example:

```bash
mkdir -p /home/tph/shared
cat > /home/tph/shared/app.env <<'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_PASSWORD=replace-me
REDIS_PASSWORD=
JWT_SECRET=replace-me-with-a-long-random-secret
EOF
```

### GitHub Secrets

Configure this repository secret:

- `SSH_PRIVATE_KEY`: private key used by GitHub Actions to connect to the server.

If `REPO_URL` points to a private repository, the server itself must already have permission to `git clone` and `git pull` it, for example through an SSH deploy key or a PAT-backed HTTPS URL.

### GitHub Variables

Recommended repository variables:

**服务器配置：**
- `SERVER_HOST`: server IP or domain.
- `SERVER_USER`: SSH username.
- `SERVER_PORT`: defaults to `22`.
- `DEPLOY_PATH`: defaults to `/home/tph`.
- `REPO_URL`: repository SSH or HTTPS clone URL.
- `APP_ENV_FILE`: defaults to `/home/tph/shared/app.env`.

**应用配置：**
- `JAVA_VERSION`: defaults to `17`.
- `DEPLOY_BRANCH`: defaults to `main`.
- `APP_NAME`: defaults to `tepinhui-backend`.
- `APP_PORT`: defaults to `8060`.
- `APP_API_PREFIX`: defaults to `/tph` (所有端点的 URL 前缀，例如 `/tph/auth/login`)
- `APP_HEALTH_ENDPOINT`: defaults to `/health` (相对于 API 前缀的健康检查路径，实际: `/tph/health`)
- `STARTUP_TIMEOUT`: defaults to `120`.
- `HEALTH_CHECK_INTERVAL`: defaults to `5`.
- `JAVA_OPTS`: defaults to `-Xms256m -Xmx512m`.
- `MVN_BUILD_ARGS`: defaults to `clean package -DskipTests`.

### Deployment flow

1. Push code to `main`.
2. GitHub Actions runs `./mvnw -B test`.
3. Workflow uploads `deploy/deploy.sh` to the server over SSH.
4. Remote script pulls the latest `main`, builds the JAR, restarts the process, and checks `http://127.0.0.1:8060/tph/health`.
5. Workflow succeeds only when health check returns HTTP `200`.

### 本地测试

```bash
# 启动应用后测试健康检查
curl -X GET http://localhost:8060/tph/health

# 预期响应:
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "tepinhui-backend",
    "timestamp": "...",
    "port": 8060,
    "context-path": "/tph",
    "health-path": "/tph/health"
  }
}
```
