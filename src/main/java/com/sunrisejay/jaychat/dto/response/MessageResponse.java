package com.sunrisejay.jaychat.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应DTO
 */
@Data
public class MessageResponse {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private String contentType;
    /**
     * 引用的消息ID
     */
    private Long replyToId;
    /**
     * 被引用消息的发送者昵称
     */
    private String replyToNickname;
    /**
     * 被引用消息的内容摘要
     */
    private String replyToContent;
    private LocalDateTime sentAt;
    /**
     * 是否已撤回
     */
    private Boolean recalled;
}
