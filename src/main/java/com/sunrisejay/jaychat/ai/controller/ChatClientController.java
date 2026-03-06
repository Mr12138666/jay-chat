package com.sunrisejay.jaychat.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: Sunrise_Jay
 * @email: sunrise_jay@yeah.net
 * @date: 2026/3/7 00:59
 */
@RestController
@RequestMapping("/v2/ai")
public class ChatClientController {

    @Resource
    private ChatClient chatClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 普通对话
     * @param message
     * @return
     */
    @GetMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        // 一次性返回结果
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
    /**
     * 流式对话
     * @param message
     * @return
     */
    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "你是谁？") String message
                                       /**@RequestParam(value = "chatId") String chatId**/) {

        // 流式输出，将每个内容包装成 JSON 格式 {"v": "内容"}
        return chatClient.prompt()
                .user(message) // 提示词
               // .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content()
                .map(content -> {
                    // 将内容包装成 JSON 格式，前端期望 {"v": "内容"}
                    String contentStr = content != null ? content : "";
                    Map<String, String> jsonMap = new HashMap<>();
                    jsonMap.put("v", contentStr);
                    try {
                        return objectMapper.writeValueAsString(jsonMap);
                    } catch (JsonProcessingException e) {
                        // 如果 JSON 序列化失败，返回默认格式（手动转义特殊字符）
                        String escaped = contentStr
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        return "{\"v\":\"" + escaped + "\"}";
                    }
                });

    }

}