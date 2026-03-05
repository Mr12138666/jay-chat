package com.sunrisejay.jaychat.auth;

import com.sunrisejay.jaychat.user.User;
import com.sunrisejay.jaychat.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        User existing = userMapper.findByUsername(req.getUsername());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        // 如果昵称为空，使用用户名作为昵称
        String nickname = req.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = req.getUsername();
        }
        user.setNickname(nickname);
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        // 为了安全，返回前可以把密码字段置空
        user.setPassword(null);
        resp.setUser(user);
        return resp;
    }
}

