package com.sunrisejay.jaychat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 * Spring Boot 3.x 使用 springdoc-openapi 替代 Springfox
 * 访问地址: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("jay-chat 接口文档")
                        .description("jay-chat 大型聊天室项目 API 文档")
                        .version("1.0.0"));
    }
}

