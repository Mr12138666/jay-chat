package com.sunrisejay.jaychat.chat.controller;

import com.sunrisejay.jaychat.chat.ChatSession;
import com.sunrisejay.jaychat.chat.dto.MessageRequest;
import com.sunrisejay.jaychat.chat.dto.MessageResponse;
import com.sunrisejay.jaychat.chat.service.ChatService;
import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, JwtUtil jwtUtil, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.jwtUtil = jwtUtil;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 创建或获取默认会话（公共聊天室）
     */
    @PostMapping("/sessions/default")
    public ApiResponse<ChatSession> getOrCreateDefaultSession(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ApiResponse.error(401, "未登录");
            }
            ChatSession session = chatService.getOrCreateDefaultSession(userId);
            return ApiResponse.success(session);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "创建默认会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> getSessions(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
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
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(chatService.getMessages(sessionId, page, pageSize));
    }

    /**
     * HTTP 接口：发送消息（用于测试，实际通过 WebSocket）
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request,
                                                     HttpServletRequest httpRequest) {
        Long senderId = getUserIdFromRequest(httpRequest);
        if (senderId == null) {
            return ApiResponse.error(401, "未登录");
        }

        MessageResponse response = chatService.sendMessage(senderId, request);
        
        // 广播消息到会话的所有成员
        messagingTemplate.convertAndSend("/topic/session." + request.getSessionId(), response);

        return ApiResponse.success(response);
    }

    /**
     * WebSocket 消息处理：接收客户端发送的消息
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request,
                           org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        // 从 Principal 中获取用户ID
        java.security.Principal principal = headerAccessor.getUser();
        Long senderId = null;
        String username = null;
        
        if (principal != null) {
            // 从自定义的 StompPrincipal 中获取用户ID
            try {
                java.lang.reflect.Method getUserIdMethod = principal.getClass().getMethod("getUserId");
                senderId = (Long) getUserIdMethod.invoke(principal);
                
                // 尝试获取用户名
                try {
                    java.lang.reflect.Method getUsernameMethod = principal.getClass().getMethod("getUsername");
                    username = (String) getUsernameMethod.invoke(principal);
                } catch (Exception e) {
                    // 忽略，用户名不是必需的
                }
            } catch (Exception e) {
                // 如果反射失败，尝试从名称获取
                String name = principal.getName();
                try {
                    senderId = Long.parseLong(name);
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("无法获取用户ID", ex);
                }
            }
        }
        
        if (senderId == null) {
            throw new RuntimeException("未找到用户信息");
        }

        System.out.println("收到 WebSocket 消息，发送者ID: " + senderId + ", 用户名: " + username + ", 消息内容: " + request.getContent());

        // 保存消息到数据库
        MessageResponse response = chatService.sendMessage(senderId, request);

        System.out.println("消息已保存，发送者昵称: " + response.getSenderNickname() + ", 发送者ID: " + response.getSenderId());

        // 广播消息到会话的所有成员
        String destination = "/topic/session." + request.getSessionId();
        System.out.println("广播消息到: " + destination + ", 消息内容: " + response.getContent() + ", 发送者: " + response.getSenderNickname());
        messagingTemplate.convertAndSend(destination, response);
    }

    /**
     * 从 HTTP 请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseClaims(token);
            return claims.get("uid", Long.class);
        } catch (Exception e) {
            return null;
        }
    }
}
