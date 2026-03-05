package com.sunrisejay.jaychat.config;

import com.sunrisejay.jaychat.common.util.JwtUtil;
import com.sunrisejay.jaychat.service.OnlineUserService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket认证拦截器
 * 用于在WebSocket连接时进行JWT认证
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final OnlineUserService onlineUserService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, OnlineUserService onlineUserService) {
        this.jwtUtil = jwtUtil;
        this.onlineUserService = onlineUserService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            Principal user = accessor.getUser();
            
            if (StompCommand.CONNECT.equals(command)) {
                // 从连接时的token中提取用户信息
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    try {
                        Claims claims = jwtUtil.parseClaims(token);
                        Long userId = claims.get("uid", Long.class);
                        String username = claims.get("username", String.class);
                        
                        // 设置Principal，包含用户ID
                        accessor.setUser(new StompPrincipal(userId, username));
                        logger.info("WebSocket连接认证成功: userId={}, username={}", userId, username);
                    } catch (Exception e) {
                        logger.warn("WebSocket认证失败: token无效", e);
                        throw new RuntimeException("WebSocket认证失败", e);
                    }
                } else {
                    logger.warn("WebSocket认证失败: 未提供Authorization header");
                    throw new RuntimeException("WebSocket认证失败: 未提供认证信息");
                }
            } else if (StompCommand.SUBSCRIBE.equals(command) && user != null) {
                // 用户订阅会话时，更新在线状态
                Long userId = getUserIdFromPrincipal(user);
                if (userId != null) {
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/session.")) {
                        try {
                            // 从 destination 中提取 sessionId: /topic/session.123 -> 123
                            String sessionIdStr = destination.substring("/topic/session.".length());
                            Long sessionId = Long.parseLong(sessionIdStr);
                            onlineUserService.userOnline(sessionId, userId);
                            logger.debug("用户订阅会话，更新在线状态: sessionId={}, userId={}", sessionId, userId);
                        } catch (NumberFormatException e) {
                            logger.warn("无法从destination提取sessionId: {}", destination);
                        }
                    }
                }
            } else if (StompCommand.UNSUBSCRIBE.equals(command) && user != null) {
                // 用户取消订阅会话时，更新在线状态
                Long userId = getUserIdFromPrincipal(user);
                if (userId != null) {
                    // 尝试从订阅ID中获取信息（UNSUBSCRIBE 时通常有 subscription 信息）
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/session.")) {
                        try {
                            String sessionIdStr = destination.substring("/topic/session.".length());
                            Long sessionId = Long.parseLong(sessionIdStr);
                            onlineUserService.userOffline(sessionId, userId);
                            logger.debug("用户取消订阅会话，更新在线状态: sessionId={}, userId={}", sessionId, userId);
                        } catch (NumberFormatException e) {
                            logger.warn("无法从destination提取sessionId: {}", destination);
                        }
                    } else {
                        // 如果没有 destination，记录日志但不处理（DISCONNECT 时会统一清除）
                        logger.debug("用户取消订阅，但无法获取sessionId: userId={}", userId);
                    }
                }
            } else if (StompCommand.DISCONNECT.equals(command) && user != null) {
                // 用户断开连接时，清除所有在线状态
                Long userId = getUserIdFromPrincipal(user);
                if (userId != null) {
                    onlineUserService.userDisconnect(userId);
                    logger.debug("用户断开连接，清除在线状态: userId={}", userId);
                }
            }
        }
        
        return message;
    }
    
    /**
     * 从Principal中获取用户ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        
        if (principal instanceof StompPrincipal) {
            return ((StompPrincipal) principal).getUserId();
        }
        
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * WebSocket Principal实现
     * 用于在WebSocket连接中存储用户信息
     */
    public static class StompPrincipal implements Principal {
        private final Long userId;
        private final String username;

        public StompPrincipal(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        @Override
        public String getName() {
            return String.valueOf(userId);
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
