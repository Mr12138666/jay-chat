package com.sunrisejay.jaychat.common.constant;

/**
 * API 相关常量
 */
public final class ApiConstants {

    private ApiConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 未登录错误码
     */
    public static final int ERROR_CODE_UNAUTHORIZED = 401;

    /**
     * 未登录错误消息
     */
    public static final String ERROR_MESSAGE_UNAUTHORIZED = "未登录";
}
