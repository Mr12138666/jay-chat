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
     * 支持两种格式：
     * 1. Authorization: Bearer <token> (标准格式)
     * 2. Authorization: <token> (简化格式，兼容性支持)
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || authHeader.trim().isEmpty()) {
            logger.debug("请求头中未找到Authorization字段");
            return null;
        }
        
        // 如果以 "Bearer " 开头，去掉前缀
        if (authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                logger.warn("Bearer token为空");
                return null;
            }
            return token;
        }
        
        // 如果没有 "Bearer " 前缀，直接当作token使用（兼容性支持）
        String token = authHeader.trim();
        if (token.isEmpty()) {
            logger.warn("Token为空");
            return null;
        }
        
        logger.debug("从Authorization请求头提取token（未使用Bearer前缀）");
        return token;
    }
}
