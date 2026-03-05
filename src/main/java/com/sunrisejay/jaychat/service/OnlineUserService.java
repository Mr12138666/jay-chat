package com.sunrisejay.jaychat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户管理服务
 * 管理每个会话的在线用户列表
 */
@Service
public class OnlineUserService {

    private static final Logger logger = LoggerFactory.getLogger(OnlineUserService.class);

    /**
     * 存储每个会话的在线用户ID集合
     * Key: sessionId, Value: Set<userId>
     */
    private final Map<Long, Set<Long>> onlineUsersBySession = new ConcurrentHashMap<>();

    /**
     * 用户上线（加入会话）
     */
    public void userOnline(Long sessionId, Long userId) {
        onlineUsersBySession.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        logger.debug("用户上线: sessionId={}, userId={}, 当前在线人数={}", 
                sessionId, userId, getOnlineCount(sessionId));
    }

    /**
     * 用户下线（离开会话）
     */
    public void userOffline(Long sessionId, Long userId) {
        Set<Long> onlineUsers = onlineUsersBySession.get(sessionId);
        if (onlineUsers != null) {
            onlineUsers.remove(userId);
            if (onlineUsers.isEmpty()) {
                onlineUsersBySession.remove(sessionId);
            }
        }
        logger.debug("用户下线: sessionId={}, userId={}, 当前在线人数={}", 
                sessionId, userId, getOnlineCount(sessionId));
    }

    /**
     * 用户完全离线（断开WebSocket连接）
     */
    public void userDisconnect(Long userId) {
        // 从所有会话中移除该用户
        onlineUsersBySession.values().forEach(users -> users.remove(userId));
        // 清理空会话
        onlineUsersBySession.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        logger.debug("用户完全离线: userId={}", userId);
    }

    /**
     * 获取会话的在线用户ID列表
     */
    public Set<Long> getOnlineUsers(Long sessionId) {
        return new HashSet<>(onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()));
    }

    /**
     * 获取会话的在线人数
     */
    public Integer getOnlineCount(Long sessionId) {
        return onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()).size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long sessionId, Long userId) {
        return onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()).contains(userId);
    }
}