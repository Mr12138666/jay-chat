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
    private LocalDateTime sentAt;
}
