package com.sunrisejay.jaychat.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 消息请求DTO
 */
@Data
public class MessageRequest {
    
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String contentType = "text"; // 默认文本消息
}
