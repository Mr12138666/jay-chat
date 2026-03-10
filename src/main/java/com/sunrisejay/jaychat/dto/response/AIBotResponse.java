package com.sunrisejay.jaychat.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI机器人响应DTO
 */
@Data
public class AIBotResponse {

    /**
     * 机器人ID
     */
    private Long id;

    /**
     * 创建者用户ID
     */
    private Long userId;

    /**
     * 机器人名称
     */
    private String name;

    /**
     * 机器人头像
     */
    private String avatar;

    /**
     * 机器人描述
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
