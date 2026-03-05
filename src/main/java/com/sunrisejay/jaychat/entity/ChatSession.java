package com.sunrisejay.jaychat.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体
 * 可以是一对一或群聊
 */
@Data
public class ChatSession {

    private Long id;

    /**
     * 会话类型：single / group
     */
    private String type;

    private String name;

    /**
     * 创建者用户ID
     */
    private Long ownerId;

    private LocalDateTime createdAt;
}
