package com.sunrisejay.jaychat.common.constant;

/**
 * 会话相关常量
 */
public final class SessionConstants {

    private SessionConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 默认会话名称
     */
    public static final String DEFAULT_SESSION_NAME = "公共聊天室";

    /**
     * 会话类型：群组
     */
    public static final String SESSION_TYPE_GROUP = "group";

    /**
     * 会话类型：私聊
     */
    public static final String SESSION_TYPE_PRIVATE = "private";
}
