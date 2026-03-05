package com.sunrisejay.jaychat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置
 */
@Configuration
@MapperScan("com.sunrisejay.jaychat.mapper")
public class MyBatisConfig {
}

