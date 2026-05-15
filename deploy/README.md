# Tepinhui Backend 部署方案

## 1. 方案概述

当前仓库的部署链路为：

- GitHub Actions 上传 `source-code.tar.gz`、部署脚本和回滚脚本到服务器
- 服务器端 `deploy.sh` 解压源码后执行 `./mvnw -B clean package -DskipTests`
- 构建成功后切换 `/home/tph/versions/latest -> /home/tph/versions/<version>`
- `systemd` 服务 `tepinhui-backend` 从 `latest/app.jar` 启动
- 服务标准输出和标准错误统一追加到固定文件日志

默认部署目录：

```text
/home/tph/
├── versions/
│   ├── <version>/
│   │   ├── app.jar
│   │   └── ...
│   └── latest -> <version>
├── shared/
│   ├── app.env
│   └── logs/
│       └── tepinhui-backend/
│           └── application.log
├── deploy.sh
└── rollback.sh
```

## 2. 服务管理

服务名称：`tepinhui-backend`

```bash
sudo systemctl status tepinhui-backend
sudo systemctl start tepinhui-backend
sudo systemctl stop tepinhui-backend
sudo systemctl restart tepinhui-backend
```

回滚失败处理服务仍会写入 journald：

```bash
sudo journalctl -u tepinhui-backend-rollback -f
```

说明：

- `tepinhui-backend` 主应用日志已切换为文件输出，不再以 `journalctl -u tepinhui-backend` 作为主查看方式。
- `deploy.sh` 每次部署都会重写 `/etc/systemd/system/tepinhui-backend.service`，所以如需调整日志路径，应通过环境变量和脚本默认值统一管理，不要只手改线上 unit 文件。

## 3. 文件日志说明

### 固定日志路径

- 默认日志文件：`/home/tph/shared/logs/tepinhui-backend/application.log`
- systemd unit 使用：
  - `StandardOutput=append:/home/tph/shared/logs/tepinhui-backend/application.log`
  - `StandardError=append:/home/tph/shared/logs/tepinhui-backend/application.log`
- 部署脚本会自动创建目录并设置：
  - 目录：`/home/tph/shared/logs/tepinhui-backend`
  - 文件权限：`0644`
  - 文件属主：`tph:tph`

### 常用查看命令

```bash
sudo tail -f /home/tph/shared/logs/tepinhui-backend/application.log
sudo tail -n 100 /home/tph/shared/logs/tepinhui-backend/application.log
sudo grep " ERROR " /home/tph/shared/logs/tepinhui-backend/application.log
sudo ls -lh /home/tph/shared/logs/tepinhui-backend/application.log
```

### 日志格式

应用默认主行格式由 `application.properties` 固定为：

```text
yyyy-MM-dd HH:mm:ss.SSS | LEVEL | [thread] | logger | message
```

管理员日志读取接口只保证解析这种“主行”。多行堆栈续行不会合并展示。

## 4. 日志读取接口

- HTTP 方法：`GET`
- 默认实际路径：`/tph/api/v1/admin/logs`
- 相对路径：`/api/v1/admin/logs`
- 权限：仅 `ADMIN` 可访问

支持的查询参数：

| 参数 | 说明 |
|------|------|
| `startTime` | ISO-8601 本地时间，例如 `2026-05-15T09:30:00` |
| `endTime` | ISO-8601 本地时间，例如 `2026-05-15T10:30:00` |
| `level` | `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR` |
| `keyword` | 对日志消息体做大小写敏感包含匹配 |
| `limit` | 返回条数，未传使用默认值，超过最大值直接拒绝 |

说明：

- 接口只读取单一文件 `application.log`。
- 不支持任意路径读取。
- 不支持历史归档文件、轮转文件或多文件聚合。
- 匿名用户和非管理员角色会被拒绝。

## 5. 环境变量

服务器环境变量文件：`/home/tph/shared/app.env`

至少关注以下与本次功能直接相关的配置：

| 变量名 | 说明 | 默认值 |
|------|------|------|
| `APP_LOG_FILE_PATH` | 管理端日志读取接口返回的日志文件路径配置 | `/home/tph/shared/logs/tepinhui-backend/application.log` |
| `APP_LOG_READ_DEFAULT_LIMIT` | 未传 `limit` 时默认返回条数 | `100` |
| `APP_LOG_READ_MAX_LIMIT` | `limit` 可接受最大值 | `500` |

补充说明：

- 当前 systemd unit 的输出路径由部署脚本固定写入 `/home/tph/shared/logs/tepinhui-backend/application.log`。
- 如果只修改 `APP_LOG_FILE_PATH`，而不同时调整 `deploy.sh` / `tepinhui-backend.service` 的输出路径，接口读取路径与实际落盘路径会不一致。
- 因此变更日志路径时，必须同步修改部署脚本默认值、systemd 模板和环境变量，保持写入与读取指向同一文件。

## 6. 部署与回滚

手动部署示例：

```bash
cd /home/tph
chmod +x deploy.sh
./deploy.sh --version 20260515-abc123
```

查看当前状态：

```bash
./rollback.sh --status
```

查看可回滚版本：

```bash
./rollback.sh --list
```

回滚到指定版本：

```bash
./rollback.sh --version 20260514-def456
```

## 7. 健康检查与排障

默认健康检查地址：

```text
http://127.0.0.1:8060/tph/health
```

服务启动失败时建议按这个顺序排查：

1. `sudo systemctl status tepinhui-backend`
2. `sudo tail -n 100 /home/tph/shared/logs/tepinhui-backend/application.log`
3. `curl http://127.0.0.1:8060/tph/health`
4. `ls -la /home/tph/versions/latest`

如果是自动回滚链路异常，再补看：

```bash
sudo journalctl -u tepinhui-backend-rollback -e
```

## 8. 日志轮转与清理策略

当前仓库内尚未提供 `logrotate` 配置文件，首版策略如下：

- 应用只写单一活动文件 `application.log`
- 管理端接口只读当前活动文件，不读历史归档
- 运维侧需要在服务器上额外配置 `logrotate` 或等效清理机制

推荐最小 `logrotate` 策略：

- 按天轮转或按文件大小轮转
- 保留最近 7 到 14 个归档
- 使用 `copytruncate` 或结合服务重启策略验证切割行为

风险：

- 文件持续追加，若未配置轮转，磁盘占用会持续增长
- 首版接口不支持跨轮转文件聚合查询，轮转后只能读取当前活动文件

## 9. 本次验证结论

已执行：

- `.\mvnw.cmd -q "-Dtest=AdminModuleControllerWebMvcTest,AdminLogServiceImplTest" test`
- 结果：通过

未执行：

- 本机为 Windows 开发环境，未提供 Linux `systemd` 运行条件
- 因此“systemd 启动后持续向日志文件追加内容”以及“真实服务接口读取该文件”的联调未在本机完成

待人工验证项：

1. 在 Linux/systemd 服务器部署后执行 `sudo tail -f /home/tph/shared/logs/tepinhui-backend/application.log`
2. 调用 `GET /tph/api/v1/admin/logs`，确认返回内容与文件追加内容一致
3. 如服务器启用了日志轮转，再验证轮转后的主文件仍可持续写入
