package com.sunrisejay.jaychat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * OSS endpoint（地域节点）
     * 例如：https://oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * Bucket名称
     */
    private String bucketName;

    /**
     * 文件访问域名（CDN或OSS域名）
     * 例如：https://your-bucket.oss-cn-hangzhou.aliyuncs.com
     * 或：https://cdn.yourdomain.com
     */
    private String domain;

    /**
     * 文件存储路径前缀
     * 例如：avatar/ 或 images/
     */
    private String pathPrefix = "avatar/";
}
