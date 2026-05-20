#!/usr/bin/env python3
"""管理员日志查询 CLI。

独立于 scripts/test/tph_cli.py，专门用于管理员日志读取接口：
    GET /api/v1/admin/logs

内置固定账号：
    username=admin
    password=admin123

典型用法：
    python scripts/log_cli.py help
    python scripts/log_cli.py login
    python scripts/log_cli.py query --level ERROR --limit 20
    python scripts/log_cli.py query --start-time 2026-05-15T09:00:00 --end-time 2026-05-15T10:00:00
    python scripts/log_cli.py logout
"""

from __future__ import annotations

import argparse
import html
import json
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except (AttributeError, ValueError):
    pass

DEFAULT_BASE_URL = "https://82.156.12.252/tph"
DEFAULT_TIMEOUT = 15.0
LOGIN_USERNAME = "admin"
LOGIN_PASSWORD = "admin123"
SESSION_DIR = Path(__file__).resolve().parent / ".sessions"
SESSION_FILE = SESSION_DIR / "log_cli_admin.json"
REPORT_DIR = Path(__file__).resolve().parent / "log_reports"


def ensure_session_dir() -> None:
    SESSION_DIR.mkdir(parents=True, exist_ok=True)


def save_session(data: dict[str, Any]) -> None:
    ensure_session_dir()
    SESSION_FILE.write_text(
        json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def load_session() -> dict[str, Any] | None:
    if not SESSION_FILE.exists():
        return None
    try:
        return json.loads(SESSION_FILE.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def clear_session() -> bool:
    if SESSION_FILE.exists():
        SESSION_FILE.unlink()
        return True
    return False


def get_access_token() -> str | None:
    session = load_session()
    if not session:
        return None
    return session.get("accessToken")


def do_request(
    method: str,
    base_url: str,
    path: str,
    *,
    token: str | None = None,
    params: dict[str, Any] | None = None,
    json_body: dict[str, Any] | None = None,
    timeout: float = DEFAULT_TIMEOUT,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + path
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if json_body is not None:
        headers["Content-Type"] = "application/json"

    start = time.perf_counter()
    err: str | None = None
    status: int | None = None
    response_body: Any = None
    try:
        response = requests.request(
            method=method,
            url=url,
            headers=headers,
            params=params,
            json=json_body,
            verify=False,
            timeout=timeout,
        )
        status = response.status_code
        try:
            response_body = response.json()
        except ValueError:
            response_body = {"_raw": response.text}
    except requests.RequestException as exc:
        err = f"{type(exc).__name__}: {exc}"

    elapsed_ms = round((time.perf_counter() - start) * 1000.0, 2)
    return {
        "ok": err is None,
        "method": method,
        "url": url,
        "request_params": params,
        "request_body": json_body,
        "status_code": status,
        "elapsed_ms": elapsed_ms,
        "response": response_body,
        "error": err,
    }


def resolve_html_output_path(raw_value: str | None) -> Path:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    if not raw_value or raw_value == "auto":
        stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        return REPORT_DIR / f"log-report-{stamp}.html"
    return Path(raw_value).expanduser().resolve()


def build_html_report(result: dict[str, Any]) -> str:
    response = result.get("response") or {}
    data = response.get("data") if isinstance(response, dict) else None
    filters = data.get("appliedFilters") if isinstance(data, dict) else {}
    records = data.get("records") if isinstance(data, dict) else []
    returned_count = data.get("returnedCount") if isinstance(data, dict) else 0
    log_file_path = data.get("logFilePath") if isinstance(data, dict) else ""

    def esc(value: Any) -> str:
        if value is None:
            return ""
        return html.escape(str(value))

    def level_class(level: Any) -> str:
        text = str(level or "").upper()
        mapping = {
            "TRACE": "trace",
            "DEBUG": "debug",
            "INFO": "info",
            "WARN": "warn",
            "ERROR": "error",
        }
        return mapping.get(text, "default")

    filter_pills = "".join(
        f"<span class='pill soft'><strong>{esc(key)}</strong> {esc(value) if value not in (None, '') else '未设置'}</span>"
        for key, value in [
            ("startTime", filters.get("startTime")),
            ("endTime", filters.get("endTime")),
            ("level", filters.get("level")),
            ("keyword", filters.get("keyword")),
            ("limit", filters.get("limit")),
        ]
    )

    record_rows = "".join(
        (
            "<tr class='log-row'"
            f" data-level='{esc(str(record.get('level') or '').upper())}'"
            f" data-timestamp='{esc(record.get('timestamp'))}'"
            f" data-message='{esc(record.get('message'))}'"
            f" data-stacktrace='{esc(record.get('stackTrace'))}'>"
            f"<td>{esc(record.get('timestamp'))}</td>"
            f"<td><span class='badge {level_class(record.get('level'))}'>{esc(record.get('level'))}</span></td>"
            f"<td class='message'>{esc(record.get('message'))}</td>"
            f"<td class='stacktrace-cell'>"
            + (
                f"<details class='stacktrace-toggle'>"
                f"<summary>查看堆栈 ({len(record.get('stackTrace', '').split(chr(10)))} 行)</summary>"
                f"<pre class='stacktrace-content'>{esc(record.get('stackTrace'))}</pre>"
                f"</details>"
            if record.get('stackTrace') else "<span class='no-stacktrace'>-</span>")
            + "</td>"
            "</tr>"
        )
        for record in records
    )

    if not record_rows:
        record_rows = (
            "<tr><td colspan='5' class='empty'>没有匹配到日志记录</td></tr>"
        )

    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>管理员日志查询报告</title>
  <style>
    :root {{
      color-scheme: light;
      --bg: #f4f6f8;
      --panel: #ffffff;
      --text: #1f2933;
      --muted: #52606d;
      --line: #d9e2ec;
      --accent: #0f766e;
      --trace: #64748b;
      --debug: #2563eb;
      --info: #0f766e;
      --warn: #b45309;
      --error: #b91c1c;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
      background: linear-gradient(180deg, #eef2f6 0%, #f8fafc 100%);
      color: var(--text);
    }}
    .wrap {{
      max-width: 1280px;
      margin: 0 auto;
      padding: 24px;
    }}
    .hero {{
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 16px;
      padding: 24px;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
      margin-bottom: 20px;
    }}
    h1 {{
      margin: 0 0 8px;
      font-size: 28px;
    }}
    .meta {{
      color: var(--muted);
      font-size: 14px;
      line-height: 1.7;
    }}
    .card {{
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 16px;
      padding: 18px;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
    }}
    .card h2 {{
      margin: 0 0 14px;
      font-size: 18px;
    }}
    table {{
      width: 100%;
      border-collapse: collapse;
    }}
    th, td {{
      border-bottom: 1px solid var(--line);
      padding: 10px 12px;
      text-align: left;
      vertical-align: top;
      font-size: 14px;
    }}
    th {{
      color: var(--muted);
      width: 110px;
      font-weight: 600;
    }}
    .log-table th {{
      width: auto;
      background: #f8fafc;
      position: sticky;
      top: 0;
      z-index: 1;
    }}
    .badge {{
      display: inline-block;
      min-width: 58px;
      text-align: center;
      border-radius: 999px;
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 700;
      color: #fff;
    }}
    .badge.trace {{ background: var(--trace); }}
    .badge.debug {{ background: var(--debug); }}
    .badge.info {{ background: var(--info); }}
    .badge.warn {{ background: var(--warn); }}
    .badge.error {{ background: var(--error); }}
    .badge.default {{ background: #475569; }}
    .message {{
      min-width: 260px;
      font-weight: 600;
      line-height: 1.6;
    }}
    .empty {{
      color: var(--muted);
      text-align: center;
      padding: 32px 12px;
    }}
    .stacktrace-cell {{
      max-width: 400px;
    }}
    .stacktrace-toggle {{
      cursor: pointer;
    }}
    .stacktrace-toggle summary {{
      color: var(--error);
      font-weight: 600;
      font-size: 13px;
      padding: 4px 0;
    }}
    .stacktrace-content {{
      background: #1f2937;
      color: #f0fdf4;
      padding: 12px;
      border-radius: 8px;
      overflow-x: auto;
      font-size: 12px;
      line-height: 1.5;
      max-height: 300px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-word;
      margin-top: 8px;
    }}
    .no-stacktrace {{
      color: var(--muted);
      font-style: italic;
    }}
    .summary {{
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-top: 16px;
    }}
    .pill {{
      background: #ecfeff;
      color: var(--accent);
      border: 1px solid #99f6e4;
      border-radius: 999px;
      padding: 6px 12px;
      font-size: 13px;
      font-weight: 600;
    }}
    .pill.soft {{
      background: #f8fafc;
      color: #334155;
      border-color: var(--line);
      font-weight: 500;
    }}
    .filter-strip {{
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 14px;
    }}
    .live-filter {{
      display: grid;
      grid-template-columns: 180px 1fr 180px;
      gap: 12px;
      align-items: end;
      margin-bottom: 16px;
    }}
    .field label {{
      display: block;
      margin-bottom: 6px;
      font-size: 12px;
      color: var(--muted);
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }}
    .field input,
    .field select {{
      width: 100%;
      border: 1px solid var(--line);
      border-radius: 10px;
      padding: 10px 12px;
      font-size: 14px;
      background: #fff;
      color: var(--text);
    }}
    .field input:focus,
    .field select:focus {{
      outline: 2px solid rgba(15, 118, 110, 0.15);
      border-color: var(--accent);
    }}
    .toolbar-note {{
      font-size: 13px;
      color: var(--muted);
      margin-bottom: 12px;
    }}
    .toolbar-summary {{
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 10px;
    }}
    @media (max-width: 960px) {{
      .live-filter {{
        grid-template-columns: 1fr;
      }}
      .wrap {{
        padding: 14px;
      }}
    }}
  </style>
</head>
<body>
  <div class="wrap">
    <section class="hero">
      <h1>管理员日志查询报告</h1>
      <div class="meta">
        <div>生成时间：{esc(datetime.now().strftime("%Y-%m-%d %H:%M:%S"))}</div>
        <div>请求地址：{esc(result.get("url"))}</div>
        <div>日志文件：{esc(log_file_path)}</div>
      </div>
      <div class="summary">
        <span class="pill">HTTP 状态：{esc(result.get("status_code"))}</span>
        <span class="pill">返回条数：{esc(returned_count)}</span>
        <span class="pill">耗时：{esc(result.get("elapsed_ms"))} ms</span>
      </div>
      <div class="filter-strip">
        {filter_pills}
      </div>
    </section>
    <section class="card">
        <h2>日志记录</h2>
        <div class="toolbar-note">下面的筛选只在当前 HTML 页面内生效，不会重新请求接口。</div>
        <div class="live-filter">
          <div class="field">
            <label for="client-level">页面内等级过滤</label>
            <select id="client-level">
              <option value="">全部等级</option>
              <option value="TRACE">TRACE</option>
              <option value="DEBUG">DEBUG</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>
          </div>
          <div class="field">
            <label for="client-keyword">页面内关键字过滤</label>
            <input id="client-keyword" type="text" placeholder="按消息内容或堆栈跟踪筛选">
          </div>
          <div class="field">
            <label for="client-time">页面内时间包含</label>
            <input id="client-time" type="text" placeholder="例如 2026-05-15 10">
          </div>
        </div>
        <div class="toolbar-summary">
          <span class="pill soft">原始返回：<strong id="server-count">{esc(returned_count)}</strong></span>
          <span class="pill soft">当前显示：<strong id="visible-count">{esc(returned_count)}</strong></span>
        </div>
        <table class="log-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>等级</th>
              <th>消息</th>
              <th>堆栈跟踪</th>
            </tr>
          </thead>
          <tbody id="log-table-body">
            {record_rows}
          </tbody>
        </table>
    </section>
  </div>
  <script>
    (function () {{
      const levelInput = document.getElementById('client-level');
      const keywordInput = document.getElementById('client-keyword');
      const timeInput = document.getElementById('client-time');
      const rows = Array.from(document.querySelectorAll('.log-row'));
      const visibleCount = document.getElementById('visible-count');

      function normalize(value) {{
        return String(value || '').toLowerCase();
      }}

      function applyFilters() {{
        const level = normalize(levelInput.value);
        const keyword = normalize(keywordInput.value.trim());
        const timeText = normalize(timeInput.value.trim());
        let count = 0;

        rows.forEach((row) => {{
          const rowLevel = normalize(row.dataset.level);
          const rowMessage = normalize(row.dataset.message);
          const rowTimestamp = normalize(row.dataset.timestamp);

          const matchesLevel = !level || rowLevel === level;
          const matchesKeyword = !keyword || rowMessage.includes(keyword) || normalize(row.dataset.stacktrace).includes(keyword);
          const matchesTime = !timeText || rowTimestamp.includes(timeText);
          const visible = matchesLevel && matchesKeyword && matchesTime;

          row.style.display = visible ? '' : 'none';
          if (visible) {{
            count += 1;
          }}
        }});

        visibleCount.textContent = String(count);
      }}

      [levelInput, keywordInput, timeInput].forEach((el) => {{
        el.addEventListener('input', applyFilters);
        el.addEventListener('change', applyFilters);
      }});
    }})();
  </script>
</body>
</html>
"""


def export_html_report(result: dict[str, Any], raw_value: str | None) -> None:
    if not result.get("ok") or result.get("status_code") != 200:
        result["html_output"] = None
        result["html_error"] = "请求未成功，未生成 HTML 报告"
        return

    response = result.get("response")
    if not isinstance(response, dict) or not isinstance(response.get("data"), dict):
        result["html_output"] = None
        result["html_error"] = "响应结构不是预期的 Result<T>，未生成 HTML 报告"
        return

    output_path = resolve_html_output_path(raw_value)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(build_html_report(result), encoding="utf-8")
    result["html_output"] = str(output_path)
    result["html_error"] = None


def print_result(result: dict[str, Any], as_json: bool) -> int:
    if as_json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result.get("ok") else 1

    print(f"[{result['method']}] {result['url']}")
    if result.get("request_params"):
        print(f"params: {json.dumps(result['request_params'], ensure_ascii=False)}")
    if result.get("request_body"):
        print(f"body: {json.dumps(result['request_body'], ensure_ascii=False)}")
    print(f"status: {result.get('status_code')}")
    print(f"elapsed_ms: {result.get('elapsed_ms')}")

    if result.get("error"):
        print(f"error: {result['error']}", file=sys.stderr)
        return 1

    if result.get("html_output"):
        print(f"html_output: {result['html_output']}")
    if result.get("html_error"):
        print(f"html_error: {result['html_error']}")

    response = result.get("response")
    if isinstance(response, dict):
        print(json.dumps(response, ensure_ascii=False, indent=2))
    else:
        print(response)
    return 0


def cmd_login(args: argparse.Namespace) -> dict[str, Any]:
    result = do_request(
        "POST",
        args.base_url,
        "/auth/login",
        json_body={"username": LOGIN_USERNAME, "password": LOGIN_PASSWORD},
        timeout=args.timeout,
    )

    if (
        result["ok"]
        and result["status_code"] == 200
        and isinstance(result["response"], dict)
    ):
        payload = result["response"].get("data") or {}
        access_token = payload.get("accessToken")
        if access_token:
            save_session(
                {
                    "username": LOGIN_USERNAME,
                    "accessToken": access_token,
                    "refreshToken": payload.get("refreshToken"),
                    "userInfo": payload.get("userInfo"),
                    "savedAt": time.time(),
                    "baseUrl": args.base_url,
                }
            )
    return result


def cmd_query(args: argparse.Namespace) -> dict[str, Any]:
    token = get_access_token()
    if not token:
        return {
            "ok": False,
            "method": "GET",
            "url": args.base_url.rstrip("/") + "/api/v1/admin/logs",
            "request_params": None,
            "request_body": None,
            "status_code": None,
            "elapsed_ms": 0.0,
            "response": None,
            "error": "未找到已登录会话，请先执行 login",
        }

    params: dict[str, Any] = {}
    if args.start_time:
        params["startTime"] = args.start_time
    if args.end_time:
        params["endTime"] = args.end_time
    if args.level:
        params["level"] = args.level
    if args.keyword:
        params["keyword"] = args.keyword
    if args.limit is not None:
        params["limit"] = args.limit

    result = do_request(
        "GET",
        args.base_url,
        "/api/v1/admin/logs",
        token=token,
        params=params or None,
        timeout=args.timeout,
    )
    if args.html is not None:
        export_html_report(result, args.html)
    return result


def cmd_logout(args: argparse.Namespace) -> dict[str, Any]:
    removed = clear_session()
    return {
        "ok": True,
        "method": "LOCAL",
        "url": str(SESSION_FILE),
        "request_params": None,
        "request_body": None,
        "status_code": 200,
        "elapsed_ms": 0.0,
        "response": {
            "message": "已清理本地登录会话" if removed else "当前没有本地登录会话",
            "sessionFile": str(SESSION_FILE),
        },
        "error": None,
    }


def cmd_status(args: argparse.Namespace) -> dict[str, Any]:
    session = load_session()
    return {
        "ok": True,
        "method": "LOCAL",
        "url": str(SESSION_FILE),
        "request_params": None,
        "request_body": None,
        "status_code": 200,
        "elapsed_ms": 0.0,
        "response": {
            "loggedIn": session is not None,
            "username": session.get("username") if session else None,
            "savedAt": session.get("savedAt") if session else None,
            "baseUrl": session.get("baseUrl") if session else None,
            "sessionFile": str(SESSION_FILE),
        },
        "error": None,
    }


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="log_cli.py",
        description="管理员日志读取独立 CLI（内置 admin/admin123）",
        formatter_class=argparse.RawTextHelpFormatter,
    )
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=f"API 基础地址，默认：{DEFAULT_BASE_URL}",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=DEFAULT_TIMEOUT,
        help=f"HTTP 超时时间（秒），默认：{DEFAULT_TIMEOUT}",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="输出完整 JSON 结果，便于脚本调用",
    )

    subparsers = parser.add_subparsers(dest="command")

    login_parser = subparsers.add_parser("login", help="使用内置 admin/admin123 登录并保存 token")
    login_parser.set_defaults(handler=cmd_login)

    query_parser = subparsers.add_parser("query", help="查询管理员日志")
    query_parser.add_argument("--start-time", help="开始时间，例如 2026-05-15T09:30:00")
    query_parser.add_argument("--end-time", help="结束时间，例如 2026-05-15T10:30:00")
    query_parser.add_argument(
        "--level",
        choices=["TRACE", "DEBUG", "INFO", "WARN", "ERROR"],
        help="日志等级",
    )
    query_parser.add_argument("--keyword", help="消息体关键字，大小写敏感")
    query_parser.add_argument("--limit", type=int, help="返回条数")
    query_parser.add_argument(
        "--html",
        nargs="?",
        const="auto",
        metavar="FILE",
        help="输出可视化 HTML 报告；不传文件路径时自动写入 scripts/log_reports/",
    )
    query_parser.set_defaults(handler=cmd_query)

    logout_parser = subparsers.add_parser("logout", help="清理本地登录会话")
    logout_parser.set_defaults(handler=cmd_logout)

    status_parser = subparsers.add_parser("status", help="查看本地会话状态")
    status_parser.set_defaults(handler=cmd_status)

    help_parser = subparsers.add_parser("help", help="查看总帮助或某个子命令帮助")
    help_parser.add_argument("topic", nargs="?", help="可选：login/query/logout/status")
    help_parser.set_defaults(handler=None)

    return parser


def run_help(parser: argparse.ArgumentParser, topic: str | None) -> int:
    if not topic:
        parser.print_help()
        return 0

    for action in parser._actions:
        if isinstance(action, argparse._SubParsersAction):
            if topic in action.choices:
                action.choices[topic].print_help()
                return 0
            break

    print(f"未知帮助主题: {topic}", file=sys.stderr)
    return 1


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.command == "help":
        return run_help(parser, args.topic)

    if not getattr(args, "handler", None):
        parser.print_help()
        return 1

    result = args.handler(args)
    return print_result(result, args.json)


if __name__ == "__main__":
    raise SystemExit(main())
