package com.sunrisejay.jaychat.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
public class ChatMessage {

    private Long id;

    private Long sessionId;

    private Long senderId;

    private String content;

    /**
     * text / image / file 等
     */
    private String contentType;

    private LocalDateTime sentAt;
}

