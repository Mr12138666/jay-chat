package com.sunrisejay.jaychat.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI机器人实体
 */
@Data
public class AIBot {

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
