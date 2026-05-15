# 管理员日志读取功能边界说明

本文档固定管理员日志读取功能的唯一日志来源、唯一文件路径、唯一接口契约与首版非范围。T01-T06 必须以本文档为准实现，除非后续任务单独更新本文档。

## 1. 目标范围

- 本功能只读取 `tepinhui-backend` 应用服务日志。
- 本功能的日志来源是 `systemd` 为 `tepinhui-backend` 服务写入的单一应用日志文件。
- 本功能不扩展为任意文件读取、不扩展为整机日志浏览、不扩展为直接执行 `journalctl` 查询。

## 2. 唯一日志来源

- 读取对象固定为 `tepinhui-backend` 服务运行日志。
- 日志采集链路固定为：`systemd` 标准输出/错误输出追加到应用日志文件，接口仅读取该文件。
- 首版实现禁止在接口、Service 或 Repository 中通过系统命令直接读取 journald。
- 首版实现禁止通过请求参数、Header、环境变量透传任意文件路径。

## 3. 唯一日志文件路径

- 固定路径：`/home/tph/shared/logs/tepinhui-backend/application.log`
- 该路径位于共享目录，不能放在 `/home/tph/versions/<version>/` 版本目录内。
- 后续部署脚本、systemd unit、应用配置的默认值必须统一到该路径。
- 首版仅读取这一份文件，不做多文件聚合，不读历史归档文件，不读轮转文件。

## 4. 唯一接口路径

- HTTP 方法：`GET`
- 相对路径：`/api/v1/admin/logs`
- 实际访问路径：`${app.api-prefix}/api/v1/admin/logs`，默认即 `/tph/api/v1/admin/logs`
- 权限要求：仅 `ADMIN` 可访问；匿名用户与非管理员角色不在本需求范围内。

## 5. 首版查询参数契约

仅允许以下五个查询参数：

| 参数名 | 类型 | 规则 |
|------|------|------|
| `startTime` | `LocalDateTime` | ISO-8601 本地时间，例如 `2026-05-15T09:30:00` |
| `endTime` | `LocalDateTime` | ISO-8601 本地时间，例如 `2026-05-15T10:30:00` |
| `level` | `String` | 仅允许 `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR` |
| `keyword` | `String` | 对日志消息体做大小写敏感的包含匹配 |
| `limit` | `Integer` | 具体默认值与最大值由配置项统一约束 |

固定规则：

- `startTime`、`endTime` 都是可选参数；传入时按应用服务器本地时区解释。
- `startTime > endTime` 视为非法请求。
- `level` 必须按大写枚举值传入；不接受逗号拼接、多值数组或正则表达式。
- `keyword` 只用于消息体模糊匹配，不扩展为 logger 名、线程名或原始整行的全文检索。
- `limit` 是可选参数；未传时使用配置默认值，超出配置最大值时按后续实现阶段统一处理为“拒绝”或“裁剪”，但不能绕过配置上限。

## 6. 首版响应结构契约

接口返回必须使用 `Result<T>` 包装，`data` 中至少包含以下结构：

```json
{
  "appliedFilters": {
    "startTime": "2026-05-15T09:30:00",
    "endTime": "2026-05-15T10:30:00",
    "level": "ERROR",
    "keyword": "JWT",
    "limit": 100
  },
  "logFilePath": "/home/tph/shared/logs/tepinhui-backend/application.log",
  "returnedCount": 1,
  "records": [
    {
      "timestamp": "2026-05-15T10:00:01",
      "level": "ERROR",
      "message": "JWT token expired",
      "rawLine": "2026-05-15 10:00:01.123 ERROR ..."
    }
  ]
}
```

字段要求：

- `appliedFilters`：返回实际生效的过滤条件，字段名与请求参数保持一致。
- `logFilePath`：返回本次读取的固定日志文件路径。
- `returnedCount`：返回 `records` 实际条数。
- `records`：日志记录列表。
- `records[].timestamp`：从日志主行解析出的时间。
- `records[].level`：从日志主行解析出的日志等级。
- `records[].message`：日志消息体，不包含前缀元数据。
- `records[].rawLine`：原始日志主行文本。

## 7. 日志解析边界

- 首版只保证解析单文件中的日志主行。
- 首版单条记录的 `rawLine` 仅要求保存主行原文。
- 异常堆栈、多行续行如何归并，由 T02 明确格式后在 T03 实现，但不得因此改变本接口的参数集合、路径或唯一日志来源。

## 8. 首版明确非范围

- 不支持传入任意文件名或任意文件路径。
- 不支持下载整个日志文件。
- 不支持修改、删除、清空日志内容。
- 不支持多文件聚合检索。
- 不支持读取 `journalctl` 输出结果。
- 不支持开放给非管理员角色。
- 不支持新增除 `startTime`、`endTime`、`level`、`keyword`、`limit` 之外的筛选参数。

## 9. 后续任务约束

- T01 必须把 systemd 文件输出默认路径落到本文档第 3 节的固定路径。
- T02 必须新增并收口“日志文件路径、默认条数、最大条数”的应用配置，且默认值与本文档一致。
- T03 只能读取本文档固定路径对应的单一日志文件。
- T04 只能暴露 `GET /api/v1/admin/logs` 一个首版读取接口。
- T05 必须验证匿名、非管理员、管理员三类访问结果与本文档一致。
- T06 的部署文档与运维文档必须引用本文档固定的日志路径和接口路径。
