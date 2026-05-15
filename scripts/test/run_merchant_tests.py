#!/usr/bin/env python3
"""通过 tph_cli.py 测试商家增删改查接口，并生成 HTML 报告。

设计：
- 每个测试用例 = (分组, 标题, CLI 命令参数列表, 期望函数)
- 通过 subprocess 调用同目录下的 tph_cli.py --json，解析 stdout 得到结构化结果
- 期望函数接收 result 字典，返回 (passed: bool, reason: str)
- 全部跑完后渲染 HTML，包含汇总卡片 + 用例分组表 + 每条用例可展开详情

报告输出路径：scripts/test/merchant_test_report.html
"""

from __future__ import annotations

import datetime as dt
import html as _html
import json
import os
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Callable

HERE = Path(__file__).resolve().parent
CLI = HERE / "tph_cli.py"
REPORT = HERE / "merchant_test_report.html"

BASE_URL = os.environ.get("TPH_BASE_URL", "https://82.156.12.252/tph")

# 测试账号
ACCOUNTS = {
    "admin": ("admin", "admin123"),
    "consumer1": ("consumer1", "test123"),
    "consumer2": ("consumer2", "test123"),
    "merchant1": ("merchant1", "test123"),
    "merchant2": ("merchant2", "test123"),
}


# ---------------------------------------------------------------------------
# CLI 调用
# ---------------------------------------------------------------------------

def run_cli(args: list[str]) -> dict[str, Any]:
    """调用 tph_cli.py，强制 --json 输出，解析得到结构化结果。"""
    cmd = [sys.executable, str(CLI), "--base-url", BASE_URL, "--json", *args]
    start = time.perf_counter()
    env = {**os.environ, "PYTHONIOENCODING": "utf-8"}
    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=30,
            env=env,
        )
        stdout = proc.stdout or ""
        stderr = proc.stderr or ""
        exit_code = proc.returncode
    except subprocess.TimeoutExpired as e:
        return {
            "cli_cmd": cmd,
            "cli_exit_code": -1,
            "cli_stderr": f"timeout: {e}",
            "cli_elapsed_ms": (time.perf_counter() - start) * 1000.0,
            "ok": False,
            "error": "CLI 调用超时",
            "status_code": None,
            "response": None,
            "method": None,
            "url": None,
            "request_body": None,
            "request_params": None,
            "elapsed_ms": 0,
        }

    cli_elapsed = (time.perf_counter() - start) * 1000.0
    parsed: dict[str, Any] = {}
    try:
        last_line = next(
            (ln for ln in reversed(stdout.splitlines()) if ln.strip()),
            "",
        )
        parsed = json.loads(last_line) if last_line else {}
    except json.JSONDecodeError as e:
        parsed = {
            "ok": False,
            "error": f"无法解析 CLI 输出为 JSON: {e}",
        }
    parsed.setdefault("status_code", None)
    parsed.setdefault("response", None)
    parsed.setdefault("method", None)
    parsed.setdefault("url", None)
    parsed.setdefault("request_body", None)
    parsed.setdefault("request_params", None)
    parsed.setdefault("elapsed_ms", 0)
    parsed["cli_cmd"] = cmd
    parsed["cli_exit_code"] = exit_code
    parsed["cli_stderr"] = stderr
    parsed["cli_elapsed_ms"] = round(cli_elapsed, 2)
    return parsed


# ---------------------------------------------------------------------------
# 期望工具
# ---------------------------------------------------------------------------

ExpectFn = Callable[[dict[str, Any]], tuple[bool, str]]


def expect_status(*codes: int) -> ExpectFn:
    """期望 HTTP 状态码属于给定集合。"""
    codes_str = ", ".join(str(c) for c in codes)

    def check(result: dict[str, Any]) -> tuple[bool, str]:
        if not result.get("ok"):
            return False, f"CLI 失败：{result.get('error')}"
        sc = result.get("status_code")
        if sc in codes:
            return True, f"状态码 {sc} ∈ {{{codes_str}}}"
        return False, f"期望状态码 ∈ {{{codes_str}}}, 实际 {sc}"

    return check


def expect_status_and(*codes: int, predicate: Callable[[dict], bool] | None = None,
                       desc: str = "") -> ExpectFn:
    base = expect_status(*codes)

    def check(result: dict[str, Any]) -> tuple[bool, str]:
        ok, msg = base(result)
        if not ok:
            return ok, msg
        if predicate is None:
            return True, msg
        try:
            if predicate(result):
                return True, msg + (f"; {desc}" if desc else "")
            return False, msg + f"; 但响应校验失败：{desc}"
        except Exception as e:  # noqa: BLE001
            return False, msg + f"; 响应校验异常：{e}"

    return check


def login_token_present(result: dict[str, Any]) -> bool:
    body = result.get("response") or {}
    data = body.get("data") or {}
    return bool(data.get("accessToken"))


# ---------------------------------------------------------------------------
# 测试用例定义
# ---------------------------------------------------------------------------

Case = tuple[str, str, list[str], ExpectFn]


def build_cases() -> list[Case]:
    cases: list[Case] = []

    # --- 准备：登录 ---
    group = "1. 准备 / 登录"
    for key, (u, p) in ACCOUNTS.items():
        cases.append((
            group,
            f"{key} 登录获取 token",
            ["login", "--username", u, "--password", p],
            expect_status_and(200, predicate=login_token_present,
                              desc="返回中包含 accessToken"),
        ))

    # --- 增（商家入驻申请） ---
    group = "2. 增 / 商家入驻申请"
    cases.append((
        group,
        "consumer1 合法提交入驻申请",
        ["merchant", "apply",
         "--user", "consumer1",
         "--shop-name", "测试店铺-consumer1-2026",
         "--license-no", "91360000MA0000000A",
         "--qualification", "https://example.com/license/c1.pdf"],
        expect_status(200, 201, 501),
    ))
    cases.append((
        group,
        "consumer2 合法提交入驻申请",
        ["merchant", "apply",
         "--user", "consumer2",
         "--shop-name", "测试店铺-consumer2-2026",
         "--license-no", "91360000MA0000000B"],
        expect_status(200, 201, 501),
    ))
    cases.append((
        group,
        "merchant1 再次提交入驻申请（业务允许或拒绝均可，至少不应 5xx）",
        ["merchant", "apply",
         "--user", "merchant1",
         "--shop-name", "测试店铺-merchant1-重复",
         "--license-no", "91360000MA0000000C"],
        expect_status(200, 201, 400, 409, 501),
    ))
    cases.append((
        group,
        "匿名提交入驻申请，应被鉴权拦截",
        ["--allow-no-token", "merchant", "apply",
         "--shop-name", "匿名店铺",
         "--license-no", "00000000000000000X"],
        expect_status(401, 403),
    ))
    cases.append((
        group,
        "shopName 空串提交，应触发参数校验",
        ["merchant", "apply",
         "--user", "consumer1",
         "--shop-name", "",
         "--license-no", "91360000MA0000000D"],
        expect_status(400, 422),
    ))
    cases.append((
        group,
        "shopName 超长（>100）提交，应触发参数校验",
        ["merchant", "apply",
         "--user", "consumer1",
         "--shop-name", "X" * 200,
         "--license-no", "91360000MA0000000E"],
        expect_status(400, 422),
    ))

    # --- 查（管理员） ---
    group = "3. 查 / 管理员视角"
    cases.append((
        group,
        "admin 查询待审核商家列表",
        ["admin", "pending", "--user", "admin", "--page", "1", "--size", "10"],
        expect_status(200, 501),
    ))
    cases.append((
        group,
        "admin 按 status=pending 查询商家列表",
        ["admin", "list", "--user", "admin",
         "--page", "1", "--size", "10", "--status", "pending"],
        expect_status(200, 501),
    ))
    cases.append((
        group,
        "admin 按 status=approved 查询商家列表",
        ["admin", "list", "--user", "admin",
         "--page", "1", "--size", "10", "--status", "approved"],
        expect_status(200, 501),
    ))
    cases.append((
        group,
        "admin 查询商家详情（id=1）",
        ["admin", "detail", "--user", "admin", "--id", "1"],
        expect_status(200, 404, 501),
    ))
    cases.append((
        group,
        "consumer1 调用管理员待审核列表，应被权限拒绝",
        ["admin", "pending", "--user", "consumer1"],
        expect_status(401, 403),
    ))
    cases.append((
        group,
        "merchant1 调用管理员待审核列表，应被权限拒绝",
        ["admin", "pending", "--user", "merchant1"],
        expect_status(401, 403),
    ))
    cases.append((
        group,
        "匿名调用管理员待审核列表，应未认证",
        ["--allow-no-token", "admin", "pending"],
        expect_status(401, 403),
    ))

    # --- 查（商家自己） ---
    group = "4. 查 / 商家视角"
    cases.append((
        group,
        "merchant1 查询自己的商家资料",
        ["merchant", "profile", "--user", "merchant1"],
        expect_status(200, 404, 501),
    ))
    cases.append((
        group,
        "merchant1 查询自己的经营数据",
        ["merchant", "stats", "--user", "merchant1"],
        expect_status(200, 501),
    ))
    cases.append((
        group,
        "admin 也能访问商家 profile 端点",
        ["merchant", "profile", "--user", "admin"],
        expect_status(200, 404, 501),
    ))
    cases.append((
        group,
        "consumer1 访问商家 profile，应被权限拒绝",
        ["merchant", "profile", "--user", "consumer1"],
        expect_status(401, 403),
    ))
    cases.append((
        group,
        "consumer1 访问商家 stats，应被权限拒绝",
        ["merchant", "stats", "--user", "consumer1"],
        expect_status(401, 403),
    ))

    # --- 改（审核） ---
    group = "5. 改 / 商家审核"
    cases.append((
        group,
        "admin 审核 id=1 为 approved",
        ["admin", "audit", "--user", "admin", "--id", "1",
         "--status", "approved", "--remark", "资质齐全，审核通过"],
        expect_status(200, 204, 404, 501),
    ))
    cases.append((
        group,
        "admin 审核 id=1 为 rejected",
        ["admin", "audit", "--user", "admin", "--id", "1",
         "--status", "rejected", "--remark", "资料不全"],
        expect_status(200, 204, 404, 501),
    ))
    cases.append((
        group,
        "admin 传入非法 status=invalid，应触发参数校验",
        ["admin", "audit", "--user", "admin", "--id", "1",
         "--status", "invalid", "--remark", "测试非法值"],
        expect_status(400, 422),
    ))
    cases.append((
        group,
        "consumer1 调用审核接口，应被权限拒绝",
        ["admin", "audit", "--user", "consumer1", "--id", "1",
         "--status", "approved"],
        expect_status(401, 403),
    ))
    cases.append((
        group,
        "merchant1 调用审核接口，应被权限拒绝",
        ["admin", "audit", "--user", "merchant1", "--id", "1",
         "--status", "approved"],
        expect_status(401, 403),
    ))

    return cases


# ---------------------------------------------------------------------------
# HTML 报告
# ---------------------------------------------------------------------------

def fmt_json(obj: Any) -> str:
    if obj is None:
        return ""
    try:
        return json.dumps(obj, ensure_ascii=False, indent=2)
    except (TypeError, ValueError):
        return str(obj)


def render_html(results: list[dict[str, Any]], started_at: dt.datetime,
                ended_at: dt.datetime) -> str:
    total = len(results)
    passed = sum(1 for r in results if r["passed"])
    failed = total - passed
    duration_s = (ended_at - started_at).total_seconds()

    groups: dict[str, list[dict[str, Any]]] = {}
    for r in results:
        groups.setdefault(r["group"], []).append(r)

    rows_html_parts: list[str] = []
    case_idx = 0
    for group_name, items in groups.items():
        g_total = len(items)
        g_passed = sum(1 for r in items if r["passed"])
        rows_html_parts.append(
            f'<h2 class="group-title">{_html.escape(group_name)} '
            f'<span class="group-meta">{g_passed}/{g_total} 通过</span></h2>'
        )
        rows_html_parts.append('<table class="cases">')
        rows_html_parts.append(
            "<thead><tr>"
            "<th class=\"col-idx\">#</th>"
            "<th class=\"col-status\">结果</th>"
            "<th>用例</th>"
            "<th class=\"col-http\">HTTP</th>"
            "<th class=\"col-ms\">耗时</th>"
            "<th class=\"col-reason\">期望/原因</th>"
            "</tr></thead><tbody>"
        )
        for r in items:
            case_idx += 1
            badge = "PASS" if r["passed"] else "FAIL"
            badge_class = "pass" if r["passed"] else "fail"
            method = r["result"].get("method") or "-"
            url = r["result"].get("url") or ""
            status = r["result"].get("status_code")
            status_disp = status if status is not None else "-"
            elapsed = r["result"].get("elapsed_ms") or 0
            cli_cmd = " ".join(_quote(x) for x in r["result"].get("cli_cmd", []))
            req_body = fmt_json(r["result"].get("request_body"))
            req_params = fmt_json(r["result"].get("request_params"))
            resp = fmt_json(r["result"].get("response"))
            cli_err = r["result"].get("error") or ""
            cli_stderr = r["result"].get("cli_stderr") or ""

            rows_html_parts.append(
                f'<tr class="case-row {badge_class}-row">'
                f'<td class="col-idx">{case_idx}</td>'
                f'<td class="col-status"><span class="badge {badge_class}">{badge}</span></td>'
                f'<td class="col-title">'
                f'<details><summary>{_html.escape(r["title"])}</summary>'
                f'<div class="detail">'
                f'<div class="kv"><span class="k">CLI</span>'
                f'<code class="cli">{_html.escape(cli_cmd)}</code></div>'
                f'<div class="kv"><span class="k">HTTP</span>'
                f'<code>{_html.escape(method)} {_html.escape(url)}</code></div>'
                + (f'<div class="kv"><span class="k">params</span><pre>{_html.escape(req_params)}</pre></div>' if req_params else "")
                + (f'<div class="kv"><span class="k">body</span><pre>{_html.escape(req_body)}</pre></div>' if req_body else "")
                + (f'<div class="kv"><span class="k">CLI 错误</span><pre class="err">{_html.escape(cli_err)}</pre></div>' if cli_err else "")
                + (f'<div class="kv"><span class="k">CLI stderr</span><pre class="err">{_html.escape(cli_stderr)}</pre></div>' if cli_stderr else "")
                + f'<div class="kv"><span class="k">response</span><pre>{_html.escape(resp)}</pre></div>'
                + '</div></details>'
                f'</td>'
                f'<td class="col-http">{status_disp}</td>'
                f'<td class="col-ms">{elapsed:.0f} ms</td>'
                f'<td class="col-reason">{_html.escape(r["reason"])}</td>'
                f'</tr>'
            )
        rows_html_parts.append("</tbody></table>")

    pass_rate = (passed / total * 100.0) if total else 0.0
    pass_rate_disp = f"{pass_rate:.1f}%"

    template = """<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>特品汇 - 商家接口测试报告</title>
<style>
:root {
  --bg: #0f1115;
  --panel: #161a22;
  --panel-2: #1d2230;
  --text: #e7eaf0;
  --muted: #8a93a6;
  --border: #262c3a;
  --green: #28a745;
  --red: #e5484d;
  --amber: #f5a623;
  --blue: #5e93ff;
}
* { box-sizing: border-box; }
body {
  margin: 0; padding: 24px;
  font-family: -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  background: var(--bg); color: var(--text); line-height: 1.55;
}
h1 { margin: 0 0 4px 0; font-size: 22px; }
.subtitle { color: var(--muted); font-size: 13px; margin-bottom: 20px; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 24px; }
.card { background: var(--panel); border: 1px solid var(--border); border-radius: 8px; padding: 14px; }
.card .label { color: var(--muted); font-size: 12px; }
.card .value { font-size: 24px; font-weight: 600; margin-top: 4px; }
.card.pass .value { color: var(--green); }
.card.fail .value { color: var(--red); }
.card.rate .value { color: var(--blue); }
.group-title { margin: 28px 0 10px 0; font-size: 16px; color: var(--text); border-left: 3px solid var(--blue); padding-left: 10px; }
.group-meta { font-size: 12px; color: var(--muted); margin-left: 8px; font-weight: normal; }
table.cases { width: 100%; border-collapse: collapse; background: var(--panel); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }
table.cases th, table.cases td { text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--border); font-size: 13px; vertical-align: top; }
table.cases th { background: var(--panel-2); color: var(--muted); font-weight: 500; font-size: 12px; }
.col-idx { width: 40px; color: var(--muted); }
.col-status { width: 70px; }
.col-http { width: 70px; }
.col-ms { width: 80px; color: var(--muted); }
.col-reason { width: 30%; color: var(--muted); font-size: 12px; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.badge.pass { background: rgba(40,167,69,0.15); color: var(--green); border: 1px solid rgba(40,167,69,0.35); }
.badge.fail { background: rgba(229,72,77,0.15); color: var(--red); border: 1px solid rgba(229,72,77,0.35); }
.fail-row { background: rgba(229,72,77,0.05); }
.case-row td details { color: var(--text); }
.case-row summary { cursor: pointer; }
.detail { margin-top: 8px; background: var(--panel-2); border: 1px solid var(--border); border-radius: 6px; padding: 10px; }
.kv { display: grid; grid-template-columns: 90px 1fr; gap: 8px; margin-bottom: 8px; }
.kv:last-child { margin-bottom: 0; }
.kv .k { color: var(--muted); font-size: 12px; padding-top: 2px; }
.kv pre, .kv code { background: #0c0f15; border: 1px solid var(--border); border-radius: 4px; padding: 6px 8px; font-size: 12px; color: var(--text); overflow-x: auto; margin: 0; white-space: pre-wrap; word-break: break-word; }
.kv pre.err { color: #ffb4b4; }
.kv code.cli { display: block; }
footer { margin-top: 32px; color: var(--muted); font-size: 12px; text-align: center; }
</style>
</head>
<body>
<h1>特品汇 - 商家接口测试报告</h1>
<div class="subtitle">
  Base URL: <code>__BASE__</code> &nbsp;|&nbsp;
  开始: __START__ &nbsp;|&nbsp; 结束: __END__ &nbsp;|&nbsp; 总耗时: __DUR__ s
</div>
<div class="cards">
  <div class="card"><div class="label">用例总数</div><div class="value">__TOTAL__</div></div>
  <div class="card pass"><div class="label">通过</div><div class="value">__PASSED__</div></div>
  <div class="card fail"><div class="label">失败</div><div class="value">__FAILED__</div></div>
  <div class="card rate"><div class="label">通过率</div><div class="value">__RATE__</div></div>
</div>
__ROWS__
<footer>由 tph_cli.py + run_merchant_tests.py 自动生成</footer>
</body>
</html>
"""

    return (
        template
        .replace("__BASE__", _html.escape(BASE_URL))
        .replace("__START__", started_at.strftime("%Y-%m-%d %H:%M:%S"))
        .replace("__END__", ended_at.strftime("%Y-%m-%d %H:%M:%S"))
        .replace("__DUR__", f"{duration_s:.2f}")
        .replace("__TOTAL__", str(total))
        .replace("__PASSED__", str(passed))
        .replace("__FAILED__", str(failed))
        .replace("__RATE__", pass_rate_disp)
        .replace("__ROWS__", "\n".join(rows_html_parts))
    )


def _quote(s: str) -> str:
    if not s:
        return '""'
    if any(c in s for c in (" ", "\t", '"')):
        return '"' + s.replace('"', '\\"') + '"'
    return s


# ---------------------------------------------------------------------------
# 主入口
# ---------------------------------------------------------------------------

def main() -> int:
    print(f"[run_merchant_tests] base url = {BASE_URL}")
    print(f"[run_merchant_tests] CLI = {CLI}")
    started = dt.datetime.now()

    cases = build_cases()
    results: list[dict[str, Any]] = []

    for idx, (group, title, args, expect) in enumerate(cases, 1):
        print(f"  [{idx}/{len(cases)}] {group} -> {title}")
        result = run_cli(args)
        passed, reason = expect(result)
        status = "OK  " if passed else "FAIL"
        sc = result.get("status_code")
        print(f"      {status} http={sc} elapsed={result.get('elapsed_ms')}ms  {reason}")
        results.append({
            "group": group,
            "title": title,
            "passed": passed,
            "reason": reason,
            "result": result,
        })

    ended = dt.datetime.now()
    REPORT.write_text(render_html(results, started, ended), encoding="utf-8")

    passed_total = sum(1 for r in results if r["passed"])
    failed_total = len(results) - passed_total
    print()
    print(f"Total: {len(results)}  Passed: {passed_total}  Failed: {failed_total}")
    print(f"Report: {REPORT}")

    return 0 if failed_total == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
