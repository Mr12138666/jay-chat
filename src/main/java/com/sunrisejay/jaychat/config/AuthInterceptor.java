package com.sunrisejay.jaychat.config;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.constant.ApiConstants;
import com.sunrisejay.jaychat.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 用于统一处理登录验证，检查请求是否携带有效的JWT Token
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取用户ID
        Long userId = jwtUtil.getUserIdFromRequest(request);

        // 如果未登录，返回错误响应
        if (userId == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ApiResponse<?> errorResponse = ApiResponse.error(ApiConstants.ERROR_CODE_UNAUTHORIZED, ApiConstants.ERROR_MESSAGE_UNAUTHORIZED);
            response.getWriter().write(errorResponse.toString());
            return false;
        }

        // 将用户ID存入请求属性，供Controller使用
        request.setAttribute("currentUserId", userId);
        return true;
    }
}
