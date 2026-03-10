package com.sunrisejay.jaychat.dto.request;

import lombok.Data;

/**
 * AI机器人请求DTO
 */
@Data
public class AIBotRequest {

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
}
