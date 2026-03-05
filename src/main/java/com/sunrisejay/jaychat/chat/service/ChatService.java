package com.sunrisejay.jaychat.chat.service;

import com.sunrisejay.jaychat.chat.ChatMessage;
import com.sunrisejay.jaychat.chat.ChatSession;
import com.sunrisejay.jaychat.chat.dto.MessageRequest;
import com.sunrisejay.jaychat.chat.dto.MessageResponse;
import com.sunrisejay.jaychat.chat.mapper.ChatMessageMapper;
import com.sunrisejay.jaychat.chat.mapper.ChatSessionMapper;
import com.sunrisejay.jaychat.chat.mapper.ChatSessionMemberMapper;
import com.sunrisejay.jaychat.user.User;
import com.sunrisejay.jaychat.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final UserMapper userMapper;

    public ChatService(ChatMessageMapper messageMapper,
                       ChatSessionMapper sessionMapper,
                       ChatSessionMemberMapper memberMapper,
                       UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    /**
     * 发送消息
     */
    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        // 验证会话是否存在
        ChatSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        // 验证用户是否在会话中
        if (!memberMapper.exists(request.getSessionId(), senderId)) {
            throw new RuntimeException("您不在该会话中");
        }

        // 保存消息
        ChatMessage message = new ChatMessage();
        message.setSessionId(request.getSessionId());
        message.setSenderId(senderId);
        message.setContent(request.getContent());
        message.setContentType(request.getContentType());
        messageMapper.insert(message);

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
     * 获取会话成员
     */
    public List<Long> getSessionMembers(Long sessionId) {
        return memberMapper.selectUserIdsBySessionId(sessionId);
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
                    throw new RuntimeException("创建会话失败，无法获取会话ID");
                }
            }
            
            // 确保用户在会话中（无论会话是新创建还是已存在）
            if (!memberMapper.exists(defaultSession.getId(), userId)) {
                int memberResult = memberMapper.insert(defaultSession.getId(), userId);
                if (memberResult <= 0) {
                    throw new RuntimeException("添加用户到会话失败");
                }
            }

            return defaultSession;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取或创建默认会话失败: " + e.getMessage(), e);
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
}
