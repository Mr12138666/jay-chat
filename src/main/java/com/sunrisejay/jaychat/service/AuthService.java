package com.sunrisejay.jaychat.service;

import com.sunrisejay.jaychat.common.converter.UserConverter;
import com.sunrisejay.jaychat.common.exception.BusinessException;
import com.sunrisejay.jaychat.common.util.JwtUtil;
import com.sunrisejay.jaychat.dto.request.LoginRequest;
import com.sunrisejay.jaychat.dto.request.RegisterRequest;
import com.sunrisejay.jaychat.dto.response.LoginResponse;
import com.sunrisejay.jaychat.dto.response.UserDetailResponse;
import com.sunrisejay.jaychat.entity.User;
import com.sunrisejay.jaychat.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;

/**
 * 认证服务
 * 处理用户注册、登录等业务逻辑
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final OssService ossService;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, OssService ossService, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.ossService = ossService;
        this.userConverter = userConverter;
    }

    /**
     * 用户注册
     */
    public void register(RegisterRequest req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new BusinessException("用户名和密码不能为空");
        }
        
        User existing = userMapper.findByUsername(req.getUsername());
        if (existing != null) {
            logger.warn("注册失败，用户名已存在: {}", req.getUsername());
            throw new BusinessException("用户名已存在");
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
        logger.info("用户注册成功: {}", req.getUsername());
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest req) {
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            logger.warn("登录失败，用户不存在: {}", req.getUsername());
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            logger.warn("登录失败，密码错误: {}", req.getUsername());
            throw new BusinessException("密码错误");
        }
        
        // 更新上次登录时间
        userMapper.updateLastLoginAt(user.getId());
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        
        // 为了安全，返回前把密码字段置空
        user.setPassword(null);
        resp.setUser(user);
        
        logger.info("用户登录成功: {}", req.getUsername());
        return resp;
    }

    public void updateNickname(Long userId, @NotBlank(message = "昵称不能为空") String nickname) {
        if(!StringUtils.hasText(nickname)) {
            throw new BusinessException("昵称不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.warn("更新昵称失败，用户不存在: userId={}", userId);
            throw new BusinessException("用户不存在");
        }
        int result = userMapper.updateNickname(userId, nickname);
        if (result == 0) {
            logger.warn("更新昵称失败，数据库操作未成功: userId={}", userId);
            throw new BusinessException("更新昵称失败");
        }
        logger.info("用户昵称更新成功: userId={}, newNickname={}", userId, nickname);
    }

    /**
     * 更新用户头像
     */
    public void updateAvatar(Long userId, String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            throw new BusinessException("头像URL不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.warn("更新头像失败，用户不存在: userId={}", userId);
            throw new BusinessException("用户不存在");
        }
        
        // 如果用户已有头像，删除旧头像（可选，节省存储空间）
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                ossService.deleteFile(user.getAvatar());
            } catch (Exception e) {
                logger.warn("删除旧头像失败: userId={}, avatar={}", userId, user.getAvatar(), e);
                // 继续执行，不因为删除失败而中断
            }
        }
        
        int result = userMapper.updateAvatar(userId, avatarUrl);
        if (result == 0) {
            logger.warn("更新头像失败，数据库操作未成功: userId={}", userId);
            throw new BusinessException("更新头像失败");
        }
        logger.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
    }

    /**
     * 获取用户详情
     */
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return userConverter.toDetailResponse(user);
    }
}
