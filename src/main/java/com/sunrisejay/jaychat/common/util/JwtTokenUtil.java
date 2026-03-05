package com.sunrisejay.jaychat.common.util;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * JWT Token工具类
 * 提供从HTTP请求中提取用户信息的便捷方法
 */
@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtTokenUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 从HTTP请求中获取用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        
        try {
            Claims claims = jwtUtil.parseClaims(token);
            return claims.get("uid", Long.class);
        } catch (Exception e) {
            logger.warn("解析JWT Token失败", e);
            return null;
        }
    }

    /**
     * 从HTTP请求中获取用户名
     */
    public String getUsernameFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        
        try {
            Claims claims = jwtUtil.parseClaims(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            logger.warn("解析JWT Token失败", e);
            return null;
        }
    }

    /**
     * 从HTTP请求中获取Claims
     */
    public Claims getClaimsFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        
        try {
            return jwtUtil.parseClaims(token);
        } catch (Exception e) {
            logger.warn("解析JWT Token失败", e);
            return null;
        }
    }

    /**
     * 从HTTP请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
