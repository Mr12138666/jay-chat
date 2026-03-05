package com.sunrisejay.jaychat.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户详情响应DTO
 */
@Data
public class UserDetailResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
