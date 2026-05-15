package com.tepinhui.tepinhui_backend.common;

public final class Constants {

    private Constants() {}

    public static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    public static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    public static final int LOGIN_MAX_ATTEMPTS = 5;
    public static final long LOGIN_LOCK_DURATION_MINUTES = 15;

    public static final String REGISTER_CODE_PREFIX = "auth:register:code:";
    public static final String REGISTER_CODE_SEND_LOCK_PREFIX = "auth:register:lock:";

    public static final String PASSWORD_RESET_CODE_PREFIX = "auth:password:code:";
    public static final String PASSWORD_RESET_CODE_SEND_LOCK_PREFIX = "auth:password:lock:";
}
