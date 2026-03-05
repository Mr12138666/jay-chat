package com.sunrisejay.jaychat.config;

import com.sunrisejay.jaychat.common.util.JwtUtil;
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

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
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
        }
        
        return message;
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
