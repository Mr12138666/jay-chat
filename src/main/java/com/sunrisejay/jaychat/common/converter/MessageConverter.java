package com.sunrisejay.jaychat.common.converter;

import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.entity.ChatMessage;
import com.sunrisejay.jaychat.entity.User;
import org.springframework.stereotype.Component;

/**
 * 消息转换器
 * 负责 Entity 到 DTO 的转换
 */
@Component
public class MessageConverter {

    /**
     * 将 ChatMessage 转换为 MessageResponse
     */
    public MessageResponse toResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }

        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setContentType(message.getContentType());
        response.setSentAt(message.getSentAt());
        return response;
    }

    /**
     * 将 ChatMessage 和 User 转换为 MessageResponse（包含发送者信息）
     */
    public MessageResponse toResponse(ChatMessage message, User sender) {
        MessageResponse response = toResponse(message);
        if (response == null) {
            return null;
        }

        if (sender != null) {
            // 如果昵称为空，使用用户名作为备选
            String nickname = sender.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = sender.getUsername();
            }
            response.setSenderNickname(nickname);
        }

        return response;
    }
}
