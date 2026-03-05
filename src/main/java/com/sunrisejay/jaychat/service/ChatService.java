package com.sunrisejay.jaychat.service;

import com.sunrisejay.jaychat.common.exception.BusinessException;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberStatsResponse;
import com.sunrisejay.jaychat.entity.ChatMessage;
import com.sunrisejay.jaychat.entity.ChatSession;
import com.sunrisejay.jaychat.entity.User;
import com.sunrisejay.jaychat.mapper.ChatMessageMapper;
import com.sunrisejay.jaychat.mapper.ChatSessionMapper;
import com.sunrisejay.jaychat.mapper.ChatSessionMemberMapper;
import com.sunrisejay.jaychat.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天服务
 * 处理聊天相关的业务逻辑
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final OnlineUserService onlineUserService;

    public ChatService(ChatMessageMapper messageMapper,
                       ChatSessionMapper sessionMapper,
                       ChatSessionMemberMapper memberMapper,
                       UserMapper userMapper,
                       OnlineUserService onlineUserService) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.onlineUserService = onlineUserService;
    }

    /**
     * 发送消息
     */
    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        // 验证会话是否存在
        ChatSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            logger.warn("发送消息失败，会话不存在: sessionId={}, senderId={}", request.getSessionId(), senderId);
            throw new BusinessException("会话不存在");
        }

        // 验证用户是否在会话中
        if (!memberMapper.exists(request.getSessionId(), senderId)) {
            logger.warn("发送消息失败，用户不在会话中: sessionId={}, senderId={}", request.getSessionId(), senderId);
            throw new BusinessException("您不在该会话中");
        }

        // 保存消息
        ChatMessage message = new ChatMessage();
        message.setSessionId(request.getSessionId());
        message.setSenderId(senderId);
        message.setContent(request.getContent());
        message.setContentType(request.getContentType());
        messageMapper.insert(message);
        
        // 更新用户的上次发言时间
        userMapper.updateLastMessageAt(senderId);

        // 构建响应
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setContentType(message.getContentType());
        response.setSentAt(message.getSentAt());

        // 查询发送者信息
        User user = userMapper.selectById(senderId);
        if (user != null) {
            // 如果昵称为空，使用用户名作为备选
            String nickname = user.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = user.getUsername();
            }
            response.setSenderNickname(nickname);
        }

        logger.debug("消息发送成功: messageId={}, sessionId={}, senderId={}", 
                message.getId(), request.getSessionId(), senderId);
        return response;
    }

    /**
     * 获取会话消息历史
     */
    public List<MessageResponse> getMessages(Long sessionId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ChatMessage> messages = messageMapper.selectBySessionId(sessionId, pageSize, offset);

        return messages.stream().map(msg -> {
            MessageResponse response = new MessageResponse();
            response.setId(msg.getId());
            response.setSessionId(msg.getSessionId());
            response.setSenderId(msg.getSenderId());
            response.setContent(msg.getContent());
            response.setContentType(msg.getContentType());
            response.setSentAt(msg.getSentAt());

            // 查询发送者信息
            User user = userMapper.selectById(msg.getSenderId());
            if (user != null) {
                // 如果昵称为空，使用用户名作为备选
                String nickname = user.getNickname();
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = user.getUsername();
                }
                response.setSenderNickname(nickname);
            }

            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户的会话列表
     */
    public List<ChatSession> getSessionsByUserId(Long userId) {
        return sessionMapper.selectByUserId(userId);
    }

    /**
     * 获取会话成员（用户ID列表）
     */
    public List<Long> getSessionMemberIds(Long sessionId) {
        return memberMapper.selectUserIdsBySessionId(sessionId);
    }

    /**
     * 获取会话成员列表（带用户信息）
     */
    public List<SessionMemberResponse> getSessionMembers(Long sessionId) {
        return memberMapper.selectMembersWithUserInfo(sessionId);
    }

    /**
     * 获取或创建默认会话（公共聊天室）
     * 注意：所有用户应该共享同一个"公共聊天室"，而不是每人创建一个
     */
    @Transactional
    public ChatSession getOrCreateDefaultSession(Long userId) {
        try {
            // 先查找全局的"公共聊天室"（不限制用户）
            // 选择最早创建的那个（如果有多个的话）
            ChatSession defaultSession = findGlobalDefaultSession();
            
            if (defaultSession == null) {
                // 如果全局不存在，创建新的公共聊天室
                defaultSession = new ChatSession();
                defaultSession.setType("group");
                defaultSession.setName("公共聊天室");
                defaultSession.setOwnerId(userId);
                
                int result = sessionMapper.insert(defaultSession);
                if (result <= 0 || defaultSession.getId() == null) {
                    logger.error("创建会话失败，无法获取会话ID: userId={}", userId);
                    throw new BusinessException("创建会话失败，无法获取会话ID");
                }
                logger.info("创建默认会话成功: sessionId={}, userId={}", defaultSession.getId(), userId);
            } else {
                logger.debug("使用已存在的默认会话: sessionId={}, userId={}", defaultSession.getId(), userId);
            }
            
            // 确保用户在会话中（无论会话是新创建还是已存在）
            if (!memberMapper.exists(defaultSession.getId(), userId)) {
                int memberResult = memberMapper.insert(defaultSession.getId(), userId);
                if (memberResult <= 0) {
                    logger.error("添加用户到会话失败: sessionId={}, userId={}", defaultSession.getId(), userId);
                    throw new BusinessException("添加用户到会话失败");
                }
                logger.debug("用户加入会话: sessionId={}, userId={}", defaultSession.getId(), userId);
            }

            return defaultSession;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取或创建默认会话失败: userId={}", userId, e);
            throw new BusinessException("获取或创建默认会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找全局的"公共聊天室"会话
     * 如果有多个，选择最早创建的那个
     */
    private ChatSession findGlobalDefaultSession() {
        List<ChatSession> sessions = sessionMapper.selectAllByName("公共聊天室");
        if (sessions != null && !sessions.isEmpty()) {
            // 返回最早创建的那个
            return sessions.get(0);
        }
        return null;
    }

    /**
     * 获取会话成员统计
     */
    public SessionMemberStatsResponse getSessionMemberStats(Long sessionId) {
        Integer totalMembers = memberMapper.countMembers(sessionId);
        Integer onlineMembers = onlineUserService.getOnlineCount(sessionId);

        SessionMemberStatsResponse stats = new SessionMemberStatsResponse();
        stats.setTotalMembers(totalMembers);
        stats.setOnlineMembers(onlineMembers);
        return stats;
    }

    /**
     * 获取在线用户ID列表
     */
    public Set<Long> getOnlineUserIds(Long sessionId) {
        return onlineUserService.getOnlineUsers(sessionId);
    }

}
