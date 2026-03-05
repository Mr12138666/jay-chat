package com.sunrisejay.jaychat.common.converter;

import com.sunrisejay.jaychat.dto.response.UserDetailResponse;
import com.sunrisejay.jaychat.entity.User;
import org.springframework.stereotype.Component;

/**
 * 用户转换器
 * 负责 User Entity 到 DTO 的转换
 */
@Component
public class UserConverter {

    /**
     * 将 User 转换为 UserDetailResponse
     */
    public UserDetailResponse toDetailResponse(User user) {
        if (user == null) {
            return null;
        }

        UserDetailResponse response = new UserDetailResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setLastMessageAt(user.getLastMessageAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
