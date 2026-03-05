package com.sunrisejay.jaychat.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.util.JwtTokenUtil;
import com.sunrisejay.jaychat.config.WebSocketAuthInterceptor;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.entity.ChatSession;
import com.sunrisejay.jaychat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * 聊天控制器
 * 处理聊天相关的HTTP和WebSocket请求
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final JwtTokenUtil jwtTokenUtil;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, JwtTokenUtil jwtTokenUtil, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 创建或获取默认会话（公共聊天室）
     */
    @PostMapping("/sessions/default")
    public ApiResponse<ChatSession> getOrCreateDefaultSession(HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        ChatSession session = chatService.getOrCreateDefaultSession(userId);
        return ApiResponse.success(session);
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> getSessions(HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(chatService.getSessionsByUserId(userId));
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(chatService.getMessages(sessionId, page, pageSize));
    }

    /**
     * HTTP接口：发送消息（用于测试，实际通过WebSocket）
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request,
                                                     HttpServletRequest httpRequest) {
        Long senderId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
        if (senderId == null) {
            return ApiResponse.error(401, "未登录");
        }

        MessageResponse response = chatService.sendMessage(senderId, request);
        
        // 广播消息到会话的所有成员
        messagingTemplate.convertAndSend("/topic/session." + request.getSessionId(), response);

        return ApiResponse.success(response);
    }

    /**
     * WebSocket消息处理：接收客户端发送的消息
     */
    @MessageMapping("/chat.send")
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
        String destination = "/topic/session." + request.getSessionId();
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
