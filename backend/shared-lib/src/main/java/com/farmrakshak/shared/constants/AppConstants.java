package com.farmrakshak.shared.constants;

public final class AppConstants {
    private AppConstants() {}

    public static final String ROLE_FARMER = "FARMER";
    public static final String ROLE_EXPERT = "EXPERT";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
}
