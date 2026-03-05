package com.sunrisejay.jaychat.controller.handler;

import com.sunrisejay.jaychat.common.constant.WebSocketConstants;
import com.sunrisejay.jaychat.config.WebSocketAuthInterceptor;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 消息处理器
 * 专门处理 WebSocket 相关的消息
 */
@Controller
public class WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageHandler(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
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

        // 保存消息到数据库
        MessageResponse response = chatService.sendMessage(senderId, request);

        // 广播消息到会话的所有成员
        String destination = WebSocketConstants.TOPIC_SESSION_PREFIX + request.getSessionId();
        messagingTemplate.convertAndSend(destination, response);
        logger.debug("消息已广播: destination={}, messageId={}", destination, response.getId());
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
