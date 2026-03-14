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

    /**
     * 消息类型：文本
     */
    public static final String CONTENT_TYPE_TEXT = "text";

    /**
     * 消息类型：图片
     */
    public static final String CONTENT_TYPE_IMAGE = "image";

    /**
     * AI机器人默认系统提示词
     */
    public static final String AI_BOT_DEFAULT_PROMPT = "你是一个AI助手";
}
