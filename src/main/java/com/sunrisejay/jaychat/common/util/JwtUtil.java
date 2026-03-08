package com.sunrisejay.jaychat.common.util;

import com.sunrisejay.jaychat.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 * 用于生成和解析JWT Token，提供从HTTP请求中提取用户信息的方法
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties properties;

    private SecretKey key;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // 使用HMAC SHA-256算法生成密钥
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpireSeconds() * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("uid", userId)
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT Token
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
            Claims claims = parseClaims(token);
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
            Claims claims = parseClaims(token);
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
            return parseClaims(token);
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
