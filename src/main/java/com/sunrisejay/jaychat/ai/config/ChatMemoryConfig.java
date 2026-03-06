package com.sunrisejay.jaychat.ai.config;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: Sunrise_Jay
 * @email: sunrise_jay@yeah.net
 * @date: 2026/3/7 01:15
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 记忆存储
     */
    @Resource
    private ChatMemoryRepository chatMemoryRepository;

    /**
     * 初始化一个 ChatMemory 实例，并注入到 Spring 容器中
     * @return
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(50) // 最大消息窗口为 50，默认值为 20
                .chatMemoryRepository(chatMemoryRepository) // 记忆存储
                .build();
    }
}