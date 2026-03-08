package com.sunrisejay.jaychat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
public class User {

    private Long id;

    private String username;

    /**
     * 密码字段，在JSON序列化时忽略，保护敏感信息
     */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 上次发言时间
     */
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
