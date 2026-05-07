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
