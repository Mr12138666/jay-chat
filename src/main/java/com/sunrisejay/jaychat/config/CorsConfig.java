package com.sunrisejay.jaychat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置
 * 统一配置跨域策略，简化配置
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")  // 匹配所有路径
                .allowedOriginPatterns("*") // 允许所有域名（生产环境应指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // 允许的请求方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许发送 Cookie
                .maxAge(3600); // 预检请求的有效期（秒）
    }
}
