# `log_cli.py` 用法

脚本位置：`scripts/log_cli.py`

内置登录账号：

- 用户名：`admin`
- 密码：`admin123`

## 1. 查看帮助

```bash
python scripts/log_cli.py help
python scripts/log_cli.py help query
```

## 2. 登录

```bash
python scripts/log_cli.py login
```

登录成功后会把 token 保存到本地会话文件：

```text
scripts/.sessions/log_cli_admin.json
```

## 3. 查询日志

按等级和条数查询：

```bash
python scripts/log_cli.py query --level ERROR --limit 20
```

按时间范围查询：

```bash
python scripts/log_cli.py query \
  --start-time 2026-05-15T09:00:00 \
  --end-time 2026-05-15T10:00:00
```

按关键字查询：

```bash
python scripts/log_cli.py query --keyword JWT
```

输出 JSON：

```bash
python scripts/log_cli.py --json query --level WARN --limit 10
```

## 4. 导出可视化 HTML 报告

自动生成到 `scripts/log_reports/`：

```bash
python scripts/log_cli.py query --level ERROR --limit 50 --html
```

指定输出文件：

```bash
python scripts/log_cli.py query --keyword login --html scripts/log_reports/login-log.html
```

## 5. 查看和清理会话

```bash
python scripts/log_cli.py status
python scripts/log_cli.py logout
```

## 6. 常用参数

- `--base-url`：指定接口基础地址，默认 `https://82.156.12.252/tph`
- `--timeout`：HTTP 超时时间，默认 `15`
- `--json`：输出完整 JSON 结果
- `--start-time`：开始时间，格式如 `2026-05-15T09:30:00`
- `--end-time`：结束时间，格式如 `2026-05-15T10:30:00`
- `--level`：`TRACE` / `DEBUG` / `INFO` / `WARN` / `ERROR`
- `--keyword`：按消息体做大小写敏感匹配
- `--limit`：返回条数
- `--html [FILE]`：导出 HTML 报告；不传文件路径时自动生成文件
