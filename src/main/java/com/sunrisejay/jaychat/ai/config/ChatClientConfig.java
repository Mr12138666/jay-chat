package com.sunrisejay.jaychat.ai.config;

import com.sunrisejay.jaychat.ai.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: Sunrise_Jay
 * @email: sunrise_jay@yeah.net
 * @date: 2026/3/7 00:57
 */
@Configuration
public class ChatClientConfig {

    @Resource
    private ChatMemory chatMemory;

    /**
     * 初始化 ChatClient 客户端
     *
     * @param chatModel
     * @return
     */
    @Bean
    public ChatClient chatClient(DeepSeekChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("请你扮演一名犬小哈 Java 项目实战专栏的客服人员")
                .defaultAdvisors(new SimpleLoggerAdvisor(), // 添加 Spring AI 内置的日志记录功能
//                                 new MyLoggerAdvisor(), // 添加自定义的日志打印 Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();


    }


}