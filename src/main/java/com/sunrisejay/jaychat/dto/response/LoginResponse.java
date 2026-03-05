package com.sunrisejay.jaychat.dto.response;

import com.sunrisejay.jaychat.entity.User;
import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {

    private String token;

    private User user;
}
