package com.sunrisejay.jaychat.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话机器人关联实体
 */
@Data
public class ChatSessionBot {

    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 机器人ID
     */
    private Long botId;

    /**
     * 添加时间
     */
    private LocalDateTime addedAt;
}
