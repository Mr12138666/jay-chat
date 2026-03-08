package com.sunrisejay.jaychat.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
public class ChatMessage {

    private Long id;

    private Long sessionId;

    private Long senderId;

    private String content;

    /**
     * 消息类型：text / image / file 等
     */
    private String contentType;

    /**
     * 引用的消息ID
     */
    private Long replyToId;

    private LocalDateTime sentAt;
}
