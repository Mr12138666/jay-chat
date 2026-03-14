package com.sunrisejay.jaychat.controller.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunrisejay.jaychat.common.constant.WebSocketConstants;
import com.sunrisejay.jaychat.config.WebSocketAuthInterceptor;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.entity.AIBot;
import com.sunrisejay.jaychat.service.AIBotService;
import com.sunrisejay.jaychat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 消息处理器
 * 专门处理 WebSocket 相关的消息
 */
@Controller
public class WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private final ChatService chatService;
    private final AIBotService aiBotService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketMessageHandler(ChatService chatService,
                                   AIBotService aiBotService,
                                   SimpMessagingTemplate messagingTemplate,
                                   ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.aiBotService = aiBotService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * WebSocket消息处理：接收客户端发送的消息
     */
    @MessageMapping(WebSocketConstants.MESSAGE_MAPPING_SEND)
    public void sendMessage(@Payload MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long senderId = getUserIdFromPrincipal(headerAccessor.getUser());
        if (senderId == null) {
            logger.error("WebSocket消息处理失败：未找到用户信息");
            throw new RuntimeException("未找到用户信息");
        }

        logger.debug("收到WebSocket消息: senderId={}, sessionId={}, content={}",
                senderId, request.getSessionId(), request.getContent());

        // 检查是否@了机器人
        List<AIBot> mentionedBots = aiBotService.extractMentionedBots(request.getContent(), request.getSessionId());

        // 保存消息到数据库
        MessageResponse response = chatService.sendMessage(senderId, request);

        // 广播消息到会话的所有成员
        String destination = WebSocketConstants.TOPIC_SESSION_PREFIX + request.getSessionId();
        messagingTemplate.convertAndSend(destination, response);
        logger.debug("消息已广播: destination={}, messageId={}", destination, response.getId());

        // 如果有@机器人，处理机器人回复
        if (!mentionedBots.isEmpty()) {
            for (AIBot bot : mentionedBots) {
                // 提取去掉@机器人后的内容
                String messageContent = aiBotService.extractMessageContent(request.getContent(), bot.getName());
                if (messageContent.isEmpty()) {
                    messageContent = "你好"; // 默认问候语
                }
                // 异步处理机器人回复
                handleBotReply(bot, request.getSessionId(), destination, messageContent);
            }
        }
    }

    /**
     * 处理机器人回复
     */
    private void handleBotReply(AIBot bot, Long sessionId, String destination, String messageContent) {
        final String requestId = UUID.randomUUID().toString();
        final StringBuilder finalBuffer = new StringBuilder();

        try {
            // 使用流式输出发送机器人回复
            Flux<String> botResponseStream = aiBotService.chatWithBotStream(bot.getId(), messageContent);

            botResponseStream.subscribe(
                    content -> {
                        // 发送流式消息片段
                        Map<String, Object> botMessage = Map.of(
                                "type", "bot_message",
                                "requestId", requestId,
                                "botId", bot.getId(),
                                "botName", bot.getName(),
                                "sessionId", sessionId,
                                "content", content
                        );
                        messagingTemplate.convertAndSend(destination, botMessage);

                        String chunk = extractChunkText(content);
                        if (!chunk.isEmpty()) {
                            finalBuffer.append(chunk);
                        }
                    },
                    error -> logger.error("机器人回复出错: botId={}, requestId={}, error={}", bot.getId(), requestId, error.getMessage(), error),
                    () -> {
                        // 流式输出完成
                        Map<String, Object> botMessageComplete = Map.of(
                                "type", "bot_message_complete",
                                "requestId", requestId,
                                "botId", bot.getId(),
                                "botName", bot.getName(),
                                "sessionId", sessionId
                        );
                        messagingTemplate.convertAndSend(destination, botMessageComplete);

                        // 机器人最终消息入库后，发送专用 final 事件（不再走通用消息广播，避免前端出现第二条）
                        String finalContent = finalBuffer.toString().trim();
                        if (!finalContent.isEmpty()) {
                            MessageResponse finalMessage = chatService.saveBotFinalMessage(sessionId, bot.getId(), finalContent);
                            Map<String, Object> finalEvent = new HashMap<>();
                            finalEvent.put("type", "bot_message_final");
                            finalEvent.put("requestId", requestId);
                            finalEvent.put("botId", bot.getId());
                            finalEvent.put("botName", bot.getName());
                            finalEvent.put("sessionId", sessionId);
                            finalEvent.put("id", finalMessage.getId());
                            finalEvent.put("senderId", finalMessage.getSenderId());
                            finalEvent.put("senderNickname", finalMessage.getSenderNickname());
                            finalEvent.put("content", finalMessage.getContent());
                            finalEvent.put("contentType", finalMessage.getContentType());
                            finalEvent.put("replyToId", finalMessage.getReplyToId());
                            finalEvent.put("replyToNickname", finalMessage.getReplyToNickname());
                            finalEvent.put("replyToContent", finalMessage.getReplyToContent());
                            finalEvent.put("sentAt", finalMessage.getSentAt());
                            finalEvent.put("recalled", finalMessage.getRecalled());
                            messagingTemplate.convertAndSend(destination, finalEvent);
                        }

                        logger.info("机器人回复完成: botId={}, sessionId={}, requestId={}", bot.getId(), sessionId, requestId);
                    }
            );
        } catch (Exception e) {
            logger.error("处理机器人回复失败: botId={}, requestId={}, error={}", bot.getId(), requestId, e.getMessage(), e);
            // 发送错误消息
            Map<String, Object> errorMessage = Map.of(
                    "type", "bot_message_error",
                    "requestId", requestId,
                    "botId", bot.getId(),
                    "botName", bot.getName(),
                    "sessionId", sessionId,
                    "error", "AI回复失败，请稍后重试"
            );
            messagingTemplate.convertAndSend(destination, errorMessage);
        }
    }

    /**
     * 兼容流式分片格式：{"v":"..."} 或纯文本
     */
    private String extractChunkText(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                JsonNode value = node.get("v");
                return value != null && !value.isNull() ? value.asText("") : "";
            } catch (Exception ignore) {
                // 非 JSON 分片，按原始文本处理
            }
        }

        return content;
    }

    /**
     * 从Principal中获取用户ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        
        // 如果是StompPrincipal，直接获取用户ID
        if (principal instanceof WebSocketAuthInterceptor.StompPrincipal) {
            return ((WebSocketAuthInterceptor.StompPrincipal) principal).getUserId();
        }
        
        // 否则尝试从名称解析
        try {
            String name = principal.getName();
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            logger.warn("无法从Principal获取用户ID: {}", principal.getClass().getName());
            return null;
        }
    }
}
