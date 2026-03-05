package com.sunrisejay.jaychat.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: Sunrise_Jay
 * @email: sunrise_jay@yeah.net
 * @date: 2026/3/5 16:21
 */
@Data
public class SessionMemberResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime joinedAt;
}
