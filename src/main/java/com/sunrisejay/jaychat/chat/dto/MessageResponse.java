package com.sunrisejay.jaychat.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
