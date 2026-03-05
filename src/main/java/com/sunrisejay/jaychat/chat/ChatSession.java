package com.sunrisejay.jaychat.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话：可以是一对一或群聊
 */
@Data
public class ChatSession {

    private Long id;

    /**
     * single / group
     */
    private String type;

    private String name;

    /**
     * 创建者用户 ID
     */
    private Long ownerId;

    private LocalDateTime createdAt;
}

