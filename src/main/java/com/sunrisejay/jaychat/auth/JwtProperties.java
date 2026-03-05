package com.sunrisejay.jaychat.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jaychat.jwt")
public class JwtProperties {

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * 过期时间（秒）
     */
    private long expireSeconds;
}

