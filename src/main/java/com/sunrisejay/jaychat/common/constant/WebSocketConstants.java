package com.sunrisejay.jaychat.common.constant;

/**
 * WebSocket 相关常量
 */
public final class WebSocketConstants {

    private WebSocketConstants() {
        // 工具类，禁止实例化
    }

    /**
     * WebSocket 消息映射路径
     */
    public static final String MESSAGE_MAPPING_SEND = "/chat.send";

    /**
     * 会话消息主题前缀
     */
    public static final String TOPIC_SESSION_PREFIX = "/topic/session.";

    /**
     * 用户消息主题前缀
     */
    public static final String TOPIC_USER_PREFIX = "/user/";
}
