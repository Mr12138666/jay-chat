package com.sunrisejay.jaychat.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.util.JwtTokenUtil;
import com.sunrisejay.jaychat.dto.request.LoginRequest;
import com.sunrisejay.jaychat.dto.request.RegisterRequest;
import com.sunrisejay.jaychat.dto.request.UpdateNicknameRequest;
import com.sunrisejay.jaychat.dto.response.LoginResponse;
import com.sunrisejay.jaychat.service.AuthService;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.*;

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

    public AuthController(AuthService authService, JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;
        this.jwtTokenUtil = jwtTokenUtil;
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



































}
