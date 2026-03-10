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
import com.sunrisejay.jaychat.entity.AIBot;
import com.sunrisejay.jaychat.entity.ChatMessage;
import com.sunrisejay.jaychat.entity.ChatSession;
import com.sunrisejay.jaychat.entity.User;
import com.sunrisejay.jaychat.mapper.AIBotMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final AIBotMapper aiBotMapper;
    private final OnlineUserService onlineUserService;
    private final MessageConverter messageConverter;
    private final EmojiUtil emojiUtil;

    public ChatService(ChatMessageMapper messageMapper,
                       ChatSessionMapper sessionMapper,
                       ChatSessionMemberMapper memberMapper,
                       UserMapper userMapper,
                       AIBotMapper aiBotMapper,
                       OnlineUserService onlineUserService,
                       MessageConverter messageConverter,
                       EmojiUtil emojiUtil) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.aiBotMapper = aiBotMapper;
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
     * 保存机器人最终回复为标准消息
     */
    @Transactional
    public MessageResponse saveBotFinalMessage(Long sessionId, Long botId, String content) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        // 复用 senderId 字段承载 bot 标识，botId 字段用于前端明确区分机器人消息
        message.setSenderId(botId);
        message.setBotId(botId);
        message.setContent(content);
        message.setContentType("text");
        message.setReplyToId(null);
        messageMapper.insert(message);

        MessageResponse response = messageConverter.toResponse(message);
        response.setSenderNickname(bot.getName());
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

        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户和机器人信息，避免 N+1 查询
        List<Long> userIds = messages.stream()
                .filter(msg -> msg.getBotId() == null)
                .map(ChatMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> botIds = messages.stream()
                .filter(msg -> msg.getBotId() != null)
                .map(ChatMessage::getBotId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, AIBot> botMap = botIds.isEmpty() ? Collections.emptyMap() :
                aiBotMapper.selectByIds(botIds).stream()
                        .collect(Collectors.toMap(AIBot::getId, b -> b));

        return messages.stream()
                .map(msg -> {
                    if (msg.getBotId() != null) {
                        AIBot bot = botMap.get(msg.getBotId());
                        MessageResponse response = messageConverter.toResponse(msg);
                        response.setSenderNickname(bot != null ? bot.getName() : "AI 助手");
                        return response;
                    }

                    User user = userMap.get(msg.getSenderId());
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

    /**
     * 创建群聊
     */
    @Transactional
    public ChatSession createGroup(Long userId, String groupName, List<Long> memberIds) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new BusinessException("群名称不能为空");
        }

        // 创建群聊
        ChatSession group = new ChatSession();
        group.setType(SessionConstants.SESSION_TYPE_GROUP);
        group.setName(groupName.trim());
        group.setOwnerId(userId);

        sessionMapper.insert(group);

        // 添加群主和成员
        memberMapper.insert(group.getId(), userId);
        for (Long memberId : memberIds) {
            if (!memberId.equals(userId)) {
                memberMapper.insert(group.getId(), memberId);
            }
        }

        logger.info("创建群聊成功: groupId={}, groupName={}, ownerId={}", group.getId(), groupName, userId);
        return group;
    }

    /**
     * 邀请用户加入群聊
     */
    @Transactional
    public void inviteMember(Long sessionId, Long inviterId, List<Long> targetUserIds) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能邀请成员");
        }

        // 检查邀请者是否是群成员
        if (!memberMapper.exists(sessionId, inviterId)) {
            throw new BusinessException("您不在该群聊中");
        }

        // 添加新成员
        for (Long userId : targetUserIds) {
            if (!memberMapper.exists(sessionId, userId)) {
                memberMapper.insert(sessionId, userId);
                logger.info("邀请成员入群: sessionId={}, inviterId={}, targetUserId={}", sessionId, inviterId, userId);
            }
        }
    }

    /**
     * 移除群成员（群主或管理员操作）
     */
    @Transactional
    public void removeMember(Long sessionId, Long operatorId, Long targetUserId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能移除成员");
        }

        // 检查操作者是否是群主
        if (!session.getOwnerId().equals(operatorId)) {
            throw new BusinessException("只有群主才能移除成员");
        }

        // 不能移除群主自己
        if (session.getOwnerId().equals(targetUserId)) {
            throw new BusinessException("不能移除群主");
        }

        // 检查目标用户是否在群中
        if (!memberMapper.exists(sessionId, targetUserId)) {
            throw new BusinessException("该用户不在群聊中");
        }

        memberMapper.delete(sessionId, targetUserId);
        logger.info("移除群成员: sessionId={}, operatorId={}, targetUserId={}", sessionId, operatorId, targetUserId);
    }

    /**
     * 退群
     */
    @Transactional
    public void leaveGroup(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能退群");
        }

        if (!memberMapper.exists(sessionId, userId)) {
            throw new BusinessException("您不在该群聊中");
        }

        // 如果是群主，不能直接退群（需要解散或转让）
        if (session.getOwnerId().equals(userId)) {
            throw new BusinessException("群主不能退群，请先解散群聊或转让群主");
        }

        memberMapper.delete(sessionId, userId);
        logger.info("用户退群: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 解散群聊（群主操作）
     */
    @Transactional
    public void dissolveGroup(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能解散");
        }

        // 只有群主可以解散
        if (!session.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主才能解散群聊");
        }

        // 删除群成员、消息和群
        memberMapper.deleteBySessionId(sessionId);
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);

        logger.info("解散群聊: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 转让群主
     */
    @Transactional
    public void transferOwner(Long sessionId, Long currentOwnerId, Long newOwnerId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能转让群主");
        }

        // 只有群主可以转让
        if (!session.getOwnerId().equals(currentOwnerId)) {
            throw new BusinessException("只有群主才能转让群主");
        }

        // 检查新群主是否是群成员
        if (!memberMapper.exists(sessionId, newOwnerId)) {
            throw new BusinessException("新群主必须是群成员");
        }

        sessionMapper.updateOwner(sessionId, newOwnerId);
        logger.info("转让群主: sessionId={}, oldOwnerId={}, newOwnerId={}", sessionId, currentOwnerId, newOwnerId);
    }

    /**
     * 更新群信息（群主操作）
     */
    @Transactional
    public void updateGroupInfo(Long sessionId, Long userId, String groupName, String notice) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("群聊不存在");
        }

        if (!SessionConstants.SESSION_TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException("只有群聊才能修改群信息");
        }

        // 只有群主可以修改
        if (!session.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主才能修改群信息");
        }

        sessionMapper.updateGroupInfo(sessionId, groupName.trim(), notice);
        logger.info("更新群信息: sessionId={}, groupName={}", sessionId, groupName);
    }

    /**
     * 获取群信息
     */
    public ChatSession getGroupInfo(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 判断用户是否是群主
     */
    public boolean isGroupOwner(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        return session.getOwnerId() != null && session.getOwnerId().equals(userId);
    }

}
