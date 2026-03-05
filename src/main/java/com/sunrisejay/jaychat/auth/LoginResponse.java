package com.sunrisejay.jaychat.auth;

import com.sunrisejay.jaychat.user.User;
import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private User user;
}

