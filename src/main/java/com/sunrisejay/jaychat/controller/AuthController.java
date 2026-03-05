package com.sunrisejay.jaychat.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.util.JwtTokenUtil;
import com.sunrisejay.jaychat.dto.request.LoginRequest;
import com.sunrisejay.jaychat.dto.request.RegisterRequest;
import com.sunrisejay.jaychat.dto.request.UpdateNicknameRequest;
import com.sunrisejay.jaychat.dto.response.LoginResponse;
import com.sunrisejay.jaychat.dto.response.UserDetailResponse;
import com.sunrisejay.jaychat.service.AuthService;
import com.sunrisejay.jaychat.service.OssService;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 * 处理用户注册、登录等认证相关接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenUtil jwtTokenUtil;
    private final OssService ossService;

    public AuthController(AuthService authService, JwtTokenUtil jwtTokenUtil, OssService ossService) {
        this.authService = authService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.ossService = ossService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ApiResponse<Claims> me(HttpServletRequest request) {
        Claims claims = jwtTokenUtil.getClaimsFromRequest(request);
        if (claims == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(claims);

    }


    /**
     * 修改昵称
     */
    @PutMapping("/nickname")
    public ApiResponse<Void> updateNickname(@Valid @RequestBody UpdateNicknameRequest request, HttpServletRequest httpRequest) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        authService.updateNickname(userId, request.getNickname());
        return ApiResponse.success(null);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<UserDetailResponse> getUserDetail(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = jwtTokenUtil.getUserIdFromRequest(request);
        if (currentUserId == null) {
            return ApiResponse.error(401, "未登录");
        }

        UserDetailResponse userDetail = authService.getUserDetail(userId);
        return ApiResponse.success(userDetail);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 上传文件到OSS
        String avatarUrl = ossService.uploadFile(file, userId);
        
        // 更新用户头像
        authService.updateAvatar(userId, avatarUrl);
        
        return ApiResponse.success(avatarUrl);
    }



































}
