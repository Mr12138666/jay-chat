package com.sunrisejay.jaychat.service;

import com.sunrisejay.jaychat.common.constant.ApiConstants;
import com.sunrisejay.jaychat.common.constant.SessionConstants;
import com.sunrisejay.jaychat.common.converter.MessageConverter;
import com.sunrisejay.jaychat.common.exception.BusinessException;
import com.sunrisejay.jaychat.common.util.EmojiUtil;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    /**
     * 消息可撤回时间限制（分钟）
     */
    private static final int RECALL_TIME_LIMIT_MINUTES = 5;

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final OnlineUserService onlineUserService;
    private final MessageConverter messageConverter;
    private final EmojiUtil emojiUtil;

    public ChatService(ChatMessageMapper messageMapper,
                       ChatSessionMapper sessionMapper,
                       ChatSessionMemberMapper memberMapper,
                       UserMapper userMapper,
                       OnlineUserService onlineUserService,
                       MessageConverter messageConverter,
                       EmojiUtil emojiUtil) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.onlineUserService = onlineUserService;
        this.messageConverter = messageConverter;
        this.emojiUtil = emojiUtil;
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

        // 转换表情代码为Unicode表情
        String content = emojiUtil.convertEmojiCodes(request.getContent());

        // 保存消息
        ChatMessage message = new ChatMessage();
        message.setSessionId(request.getSessionId());
        message.setSenderId(senderId);
        message.setContent(content);
        message.setContentType(request.getContentType());
        message.setReplyToId(request.getReplyToId());
        messageMapper.insert(message);

        // 更新用户的上次发言时间
        userMapper.updateLastMessageAt(senderId);

        // 查询发送者信息并转换为响应
        User user = userMapper.selectById(senderId);

        // 如果有引用消息，获取被引用消息的信息
        MessageResponse response;
        if (request.getReplyToId() != null) {
            ChatMessage replyToMessage = messageMapper.selectById(request.getReplyToId());
            User replyToUser = replyToMessage != null ? userMapper.selectById(replyToMessage.getSenderId()) : null;
            response = messageConverter.toResponseWithReply(message, user, replyToMessage, replyToUser);
        } else {
            response = messageConverter.toResponse(message, user);
        }

        logger.debug("消息发送成功: messageId={}, sessionId={}, senderId={}",
                message.getId(), request.getSessionId(), senderId);
        return response;
    }

    /**
     * 获取会话消息历史
     */
    public List<MessageResponse> getMessages(Long sessionId, Integer page, Integer pageSize) {
        // 使用默认值
        if (page == null || page < 1) {
            page = ApiConstants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = ApiConstants.DEFAULT_PAGE_SIZE;
        }

        int offset = (page - 1) * pageSize;
        List<ChatMessage> messages = messageMapper.selectBySessionId(sessionId, pageSize, offset);

        return messages.stream()
                .map(msg -> {
                    User user = userMapper.selectById(msg.getSenderId());
                    return messageConverter.toResponse(msg, user);
                })
                .collect(Collectors.toList());
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
                defaultSession.setType(SessionConstants.SESSION_TYPE_GROUP);
                defaultSession.setName(SessionConstants.DEFAULT_SESSION_NAME);
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
        List<ChatSession> sessions = sessionMapper.selectAllByName(SessionConstants.DEFAULT_SESSION_NAME);
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

    /**
     * 获取所有用户（除当前用户外）
     */
    public List<User> getAllUsersExcept(Long userId) {
        return userMapper.selectAllExcept(userId);
    }

    /**
     * 搜索用户
     */
    public List<User> searchUsers(String keyword, Long userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userMapper.selectAllExcept(userId);
        }
        return userMapper.searchByKeyword(keyword.trim(), userId);
    }

    /**
     * 获取或创建私人会话
     */
    @Transactional
    public ChatSession getOrCreatePrivateSession(Long userId, Long targetUserId) {
        // 验证目标用户是否存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 不能与自己聊天
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能与自己聊天");
        }

        // 查找已存在的私人会话
        ChatSession existingSession = sessionMapper.selectSingleSessionBetweenUsers(userId, targetUserId);
        if (existingSession != null) {
            // 确保用户在会话中
            if (!memberMapper.exists(existingSession.getId(), userId)) {
                memberMapper.insert(existingSession.getId(), userId);
            }
            if (!memberMapper.exists(existingSession.getId(), targetUserId)) {
                memberMapper.insert(existingSession.getId(), targetUserId);
            }
            logger.debug("使用已存在的私人会话: sessionId={}, userId={}, targetUserId={}",
                    existingSession.getId(), userId, targetUserId);
            return existingSession;
        }

        // 创建新的私人会话
        ChatSession privateSession = new ChatSession();
        privateSession.setType(SessionConstants.SESSION_TYPE_PRIVATE);
        privateSession.setName(null); // 私人会话不需要名称，会显示对方昵称
        privateSession.setOwnerId(userId);

        int result = sessionMapper.insert(privateSession);
        if (result <= 0 || privateSession.getId() == null) {
            logger.error("创建私人会话失败: userId={}, targetUserId={}", userId, targetUserId);
            throw new BusinessException("创建私人会话失败");
        }

        // 添加双方到会话
        memberMapper.insert(privateSession.getId(), userId);
        memberMapper.insert(privateSession.getId(), targetUserId);

        logger.info("创建私人会话成功: sessionId={}, userId={}, targetUserId={}",
                privateSession.getId(), userId, targetUserId);
        return privateSession;
    }

    /**
     * 获取私人会话的其他成员（除当前用户外）
     */
    public User getPrivateSessionOtherMember(Long sessionId, Long currentUserId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }

        // 只有 single 类型才有"其他成员"的概念
        if (!SessionConstants.SESSION_TYPE_PRIVATE.equals(session.getType())) {
            return null;
        }

        List<Long> memberIds = memberMapper.selectUserIdsBySessionId(sessionId);
        for (Long memberId : memberIds) {
            if (!memberId.equals(currentUserId)) {
                return userMapper.selectById(memberId);
            }
        }
        return null;
    }

    /**
     * 删除会话（退出会话）
     * 如果是群聊，则从群聊中移除用户；如果是私人会话，则删除整个会话
     */
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        if (SessionConstants.SESSION_TYPE_PRIVATE.equals(session.getType())) {
            // 私人会话：删除整个会话
            // 先删除会话成员关系
            memberMapper.deleteBySessionId(sessionId);
            // 再删除会话中的消息
            messageMapper.deleteBySessionId(sessionId);
            // 最后删除会话
            sessionMapper.deleteById(sessionId);
            logger.info("删除私人会话: sessionId={}", sessionId);
        } else {
            // 群聊：只移除用户
            if (!memberMapper.exists(sessionId, userId)) {
                throw new BusinessException("您不在该群聊中");
            }
            memberMapper.delete(sessionId, userId);
            logger.info("退出群聊: sessionId={}, userId={}", sessionId, userId);
        }
    }

    /**
     * 撤回消息
     * @param messageId 消息ID
     * @param userId 当前用户ID
     * @return 被撤回的消息响应
     */
    @Transactional
    public MessageResponse recallMessage(Long messageId, Long userId) {
        // 查询消息
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }

        // 验证是否为自己的消息
        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException("只能撤回自己发送的消息");
        }

        // 检查是否已撤回
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            throw new BusinessException("消息已被撤回");
        }

        // 检查是否在可撤回时间内（5分钟）
        long minutesDiff = ChronoUnit.MINUTES.between(message.getSentAt(), LocalDateTime.now());
        if (minutesDiff > RECALL_TIME_LIMIT_MINUTES) {
            throw new BusinessException("消息已超过" + RECALL_TIME_LIMIT_MINUTES + "分钟，无法撤回");
        }

        // 执行撤回
        messageMapper.updateRecalled(messageId);

        // 获取发送者信息
        User sender = userMapper.selectById(userId);

        // 转换为响应
        MessageResponse response = messageConverter.toResponse(message, sender);
        response.setRecalled(true);

        logger.info("消息撤回成功: messageId={}, userId={}", messageId, userId);
        return response;
    }

}
