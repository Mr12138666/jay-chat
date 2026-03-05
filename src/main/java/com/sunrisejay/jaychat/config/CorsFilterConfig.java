package com.sunrisejay.jaychat.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CORS 过滤器配置
 * 确保所有跨域请求（包括预检请求）都能正确处理
 */
@Configuration
public class CorsFilterConfig {

    /**
     * CORS 过滤器实现
     */
    public static class CorsFilter implements Filter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            // 获取请求的 Origin
            String origin = request.getHeader("Origin");
            
            // 允许的来源列表 - 更宽松的匹配规则
            // 注意：当 Access-Control-Allow-Credentials 为 true 时，不能使用 *
            if (origin != null) {
                // 允许 localhost 和 127.0.0.1 的所有端口
                if (origin.startsWith("http://localhost:") || 
                    origin.startsWith("http://127.0.0.1:") ||
                    origin.startsWith("http://120.53.242.78")) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                } else {
                    // 对于其他来源，也允许但不需要 credentials
                    response.setHeader("Access-Control-Allow-Origin", origin);
                }
            }
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            response.setHeader("Access-Control-Expose-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "3600");

            // 如果是预检请求（OPTIONS），直接返回 200，不继续执行后续过滤器
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            chain.doFilter(req, res);
        }
    }

    /**
     * 注册 CORS 过滤器，确保在 Spring Security 之前执行
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorsFilter());
        registration.addUrlPatterns("/*");
        registration.setName("corsFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
