#!/usr/bin/env python3
"""特品汇后端通用 CLI。

提供登录与商家相关增删改查接口的命令行调用入口。

典型用法：
    python tph_cli.py --json login --username admin --password admin123
    python tph_cli.py --json merchant apply --user consumer1 \
        --shop-name "赣南脐橙旗舰店" --license-no "91360000MA0000000X"
    python tph_cli.py --json admin pending --user admin
    python tph_cli.py --json admin audit --user admin --id 1 --status approved \
        --remark "资质齐全"

输出说明：
    默认输出人类可读文本，加 --json 后输出统一 JSON：
    {
      "ok": bool,              # CLI 本身是否成功执行（不代表 HTTP 成功）
      "method": "POST",        # 实际 HTTP 方法
      "url": "...",            # 实际请求 URL
      "request_body": {...},   # 实际请求体
      "status_code": 200,      # HTTP 状态码
      "elapsed_ms": 123.4,     # 耗时
      "response": {...},       # 响应 JSON（解析失败则放 raw 文本）
      "error": null            # CLI 层错误信息
    }
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib3
from pathlib import Path
from typing import Any

import requests

# 自签名证书：禁用 SSL 校验并屏蔽警告
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Windows 默认 stdout 是 cp936，强制 UTF-8，便于父进程解析
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except (AttributeError, ValueError):
    pass

DEFAULT_BASE_URL = "https://82.156.12.252/tph"
SESSION_DIR = Path(__file__).resolve().parent / ".sessions"


# ---------------------------------------------------------------------------
# session 管理
# ---------------------------------------------------------------------------

def session_path(user: str) -> Path:
    SESSION_DIR.mkdir(parents=True, exist_ok=True)
    return SESSION_DIR / f"{user}.json"


def save_session(user: str, data: dict[str, Any]) -> None:
    session_path(user).write_text(
        json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def load_session(user: str) -> dict[str, Any] | None:
    p = session_path(user)
    if not p.exists():
        return None
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def get_token(user: str) -> str | None:
    sess = load_session(user)
    if not sess:
        return None
    return sess.get("accessToken")


# ---------------------------------------------------------------------------
# HTTP 请求封装
# ---------------------------------------------------------------------------

def do_request(
    method: str,
    base_url: str,
    path: str,
    *,
    token: str | None = None,
    json_body: dict | None = None,
    params: dict | None = None,
    timeout: float = 15.0,
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
    body: Any = None
    try:
        resp = requests.request(
            method=method,
            url=url,
            headers=headers,
            json=json_body,
            params=params,
            verify=False,
            timeout=timeout,
        )
        status = resp.status_code
        try:
            body = resp.json()
        except ValueError:
            body = {"_raw": resp.text}
    except requests.RequestException as e:
        err = f"{type(e).__name__}: {e}"
    elapsed_ms = (time.perf_counter() - start) * 1000.0

    return {
        "ok": err is None,
        "method": method,
        "url": url,
        "request_body": json_body,
        "request_params": params,
        "status_code": status,
        "elapsed_ms": round(elapsed_ms, 2),
        "response": body,
        "error": err,
    }


# ---------------------------------------------------------------------------
# 业务命令实现
# ---------------------------------------------------------------------------

def cmd_login(args) -> dict[str, Any]:
    result = do_request(
        "POST",
        args.base_url,
        "/auth/login",
        json_body={"username": args.username, "password": args.password},
    )
    if (
        result["ok"]
        and result["status_code"] == 200
        and isinstance(result["response"], dict)
    ):
        data = result["response"].get("data") or {}
        access = data.get("accessToken")
        if access:
            save_session(
                args.username,
                {
                    "username": args.username,
                    "accessToken": access,
                    "refreshToken": data.get("refreshToken"),
                    "userInfo": data.get("userInfo"),
                    "savedAt": time.time(),
                },
            )
    return result


def require_token(user: str) -> str | None:
    """读取已保存的 token，没有就返回 None（让上层决定怎么处理）。"""
    return get_token(user)


def with_token(args, fn):
    """对需要 token 的命令做统一处理。

    如果 --user 指定但 token 不存在（比如登录失败），仍然发请求但不带
    Authorization 头，让后端返回 401/403 —— 这样测试能跑完整链路，报告
    里看到的是真实的 HTTP 行为，而不是 CLI 层错误。
    """
    token = None
    if args.user:
        token = require_token(args.user)
        if not token:
            sys.stderr.write(
                f"[warn] 未找到用户 {args.user} 的 token，"
                f"将以匿名方式发送请求\n"
            )
    return fn(token)


def cmd_merchant_apply(args) -> dict[str, Any]:
    body = {
        "shopName": args.shop_name,
        "licenseNo": args.license_no,
    }
    if args.qualification is not None:
        body["qualification"] = args.qualification

    def runner(token):
        return do_request(
            "POST",
            args.base_url,
            "/api/v1/merchant/apply",
            token=token,
            json_body=body,
        )

    return with_token(args, runner)


def cmd_merchant_profile(args) -> dict[str, Any]:
    return with_token(
        args,
        lambda token: do_request(
            "GET", args.base_url, "/api/v1/merchant/profile", token=token
        ),
    )


def cmd_merchant_stats(args) -> dict[str, Any]:
    return with_token(
        args,
        lambda token: do_request(
            "GET", args.base_url, "/api/v1/merchant/stats", token=token
        ),
    )


def cmd_admin_pending(args) -> dict[str, Any]:
    return with_token(
        args,
        lambda token: do_request(
            "GET",
            args.base_url,
            "/api/v1/admin/merchant/pending",
            token=token,
            params={"page": args.page, "size": args.size},
        ),
    )


def cmd_admin_list(args) -> dict[str, Any]:
    params: dict[str, Any] = {"page": args.page, "size": args.size}
    if args.status:
        params["status"] = args.status
    return with_token(
        args,
        lambda token: do_request(
            "GET",
            args.base_url,
            "/api/v1/admin/merchant/list",
            token=token,
            params=params,
        ),
    )


def cmd_admin_detail(args) -> dict[str, Any]:
    return with_token(
        args,
        lambda token: do_request(
            "GET",
            args.base_url,
            f"/api/v1/admin/merchant/{args.id}",
            token=token,
        ),
    )


def cmd_admin_audit(args) -> dict[str, Any]:
    body: dict[str, Any] = {"status": args.status}
    if args.remark is not None:
        body["auditRemark"] = args.remark
    return with_token(
        args,
        lambda token: do_request(
            "PUT",
            args.base_url,
            f"/api/v1/admin/merchant/{args.id}/audit",
            token=token,
            json_body=body,
        ),
    )


def cmd_whoami(args) -> dict[str, Any]:
    return with_token(
        args,
        lambda token: do_request(
            "GET", args.base_url, "/auth/me", token=token
        ),
    )


# ---------------------------------------------------------------------------
# CLI 解析
# ---------------------------------------------------------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="特品汇后端 CLI 测试工具")
    p.add_argument(
        "--base-url",
        default=os.environ.get("TPH_BASE_URL", DEFAULT_BASE_URL),
        help=f"后端 base url，默认 {DEFAULT_BASE_URL}",
    )
    p.add_argument("--json", action="store_true", help="以 JSON 格式输出结果")
    p.add_argument(
        "--allow-no-token",
        action="store_true",
        help="允许在缺少 token 时也发送请求（用于测试未认证场景）",
    )

    sub = p.add_subparsers(dest="cmd", required=True)

    # login
    sp = sub.add_parser("login", help="登录并缓存 token")
    sp.add_argument("--username", required=True)
    sp.add_argument("--password", required=True)
    sp.set_defaults(func=cmd_login)

    # whoami
    sp = sub.add_parser("whoami", help="查看当前 token 对应用户")
    sp.add_argument("--user", required=True, help="使用哪个用户的 token")
    sp.set_defaults(func=cmd_whoami)

    # merchant 子命令组
    mp = sub.add_parser("merchant", help="商家侧接口")
    msub = mp.add_subparsers(dest="mcmd", required=True)

    sp = msub.add_parser("apply", help="商家入驻申请")
    sp.add_argument("--user", default=None, help="使用哪个用户的 token（不传则匿名）")
    sp.add_argument("--shop-name", required=True)
    sp.add_argument("--license-no", required=True)
    sp.add_argument("--qualification", default=None)
    sp.set_defaults(func=cmd_merchant_apply)

    sp = msub.add_parser("profile", help="获取当前商家资料")
    sp.add_argument("--user", default=None)
    sp.set_defaults(func=cmd_merchant_profile)

    sp = msub.add_parser("stats", help="获取商家经营数据")
    sp.add_argument("--user", default=None)
    sp.set_defaults(func=cmd_merchant_stats)

    # admin 子命令组
    ap = sub.add_parser("admin", help="管理员端接口")
    asub = ap.add_subparsers(dest="acmd", required=True)

    sp = asub.add_parser("pending", help="待审核商家列表")
    sp.add_argument("--user", default=None)
    sp.add_argument("--page", type=int, default=1)
    sp.add_argument("--size", type=int, default=10)
    sp.set_defaults(func=cmd_admin_pending)

    sp = asub.add_parser("list", help="商家列表（可按状态筛选）")
    sp.add_argument("--user", default=None)
    sp.add_argument("--page", type=int, default=1)
    sp.add_argument("--size", type=int, default=10)
    sp.add_argument("--status", default=None,
                    help="过滤状态：pending/approved/rejected（不限制以便测试）")
    sp.set_defaults(func=cmd_admin_list)

    sp = asub.add_parser("detail", help="商家详情")
    sp.add_argument("--user", default=None)
    sp.add_argument("--id", type=int, required=True)
    sp.set_defaults(func=cmd_admin_detail)

    sp = asub.add_parser("audit", help="审核商家")
    sp.add_argument("--user", default=None)
    sp.add_argument("--id", type=int, required=True)
    sp.add_argument("--status", required=True,
                    help="审核状态：approved/rejected（不限制以便测试非法值）")
    sp.add_argument("--remark", default=None)
    sp.set_defaults(func=cmd_admin_audit)

    return p


def render_human(result: dict[str, Any]) -> str:
    lines = []
    lines.append(f"HTTP {result.get('method')} {result.get('url')}")
    if result.get("request_params"):
        lines.append(f"params: {result['request_params']}")
    if result.get("request_body") is not None:
        lines.append(
            "body: "
            + json.dumps(result["request_body"], ensure_ascii=False)
        )
    lines.append(
        f"-> status={result.get('status_code')}  elapsed={result.get('elapsed_ms')}ms"
    )
    if result.get("error"):
        lines.append(f"error: {result['error']}")
    lines.append("response:")
    lines.append(json.dumps(result.get("response"), ensure_ascii=False, indent=2))
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    result = args.func(args)

    if args.json:
        sys.stdout.write(json.dumps(result, ensure_ascii=False))
        sys.stdout.write("\n")
    else:
        sys.stdout.write(render_human(result) + "\n")

    # 退出码：CLI 自身错误返回 2；HTTP 5xx 返回 1；其余 0
    if not result.get("ok"):
        return 2
    status = result.get("status_code") or 0
    if status >= 500:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
