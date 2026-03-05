package com.sunrisejay.jaychat.chat.config;

import com.sunrisejay.jaychat.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从连接时的 token 中提取用户信息
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    Claims claims = jwtUtil.parseClaims(token);
                    Long userId = claims.get("uid", Long.class);
                    String username = claims.get("username", String.class);
                    
                    // 设置 Principal，包含用户ID
                    accessor.setUser(new StompPrincipal(userId, username));
                } catch (Exception e) {
                    // Token 无效，拒绝连接
                    throw new RuntimeException("WebSocket 认证失败", e);
                }
            }
        }
        
        return message;
    }

    // 简单的 Principal 实现
    private static class StompPrincipal implements Principal {
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
