package com.sunrisejay.jaychat.controller.base;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.constant.ApiConstants;
import com.sunrisejay.jaychat.common.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 基础控制器
 * 提供公共方法供子类使用
 */
public abstract class BaseController {

    @Autowired
    protected JwtTokenUtil jwtTokenUtil;

    /**
     * 从请求中获取当前用户ID
     * 如果未登录，返回null
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        return jwtTokenUtil.getUserIdFromRequest(request);
    }

    /**
     * 检查用户是否已登录
     * 如果未登录，返回错误响应
     */
    protected ApiResponse<?> checkAuth(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(ApiConstants.ERROR_CODE_UNAUTHORIZED, ApiConstants.ERROR_MESSAGE_UNAUTHORIZED);
        }
        return null; // 已登录，返回null表示继续执行
    }
}
