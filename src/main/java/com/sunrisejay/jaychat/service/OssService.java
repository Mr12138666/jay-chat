package com.sunrisejay.jaychat.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.sunrisejay.jaychat.config.OssProperties;
import com.sunrisejay.jaychat.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS服务
 * 用于上传文件到阿里云对象存储
 */
@Service
public class OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssService.class);

    private final OssProperties ossProperties;

    public OssService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文件到OSS
     * @param file 文件
     * @param userId 用户ID（用于生成文件路径）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件类型（只允许图片）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名不能为空");
        }

        String fileExtension = getFileExtension(originalFilename);
        if (!isImageFile(fileExtension)) {
            throw new BusinessException("只支持图片格式：jpg, jpeg, png, gif, webp");
        }

        // 验证文件大小（限制5MB）
        long fileSize = file.getSize();
        if (fileSize > 5 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            // 生成文件路径：avatar/userId/uuid.extension
            String fileName = generateFileName(userId, fileExtension);
            String objectName = ossProperties.getPathPrefix() + fileName;

            // 上传文件
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    objectName,
                    inputStream
            );
            ossClient.putObject(putObjectRequest);

            // 生成文件访问URL
            String fileUrl = ossProperties.getDomain() + "/" + objectName;
            logger.info("文件上传成功: userId={}, fileName={}, url={}", userId, fileName, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            logger.error("文件上传失败: userId={}", userId, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 删除OSS文件
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 从完整URL中提取objectName
        String objectName = extractObjectNameFromUrl(fileUrl);
        if (objectName == null) {
            logger.warn("无法从URL提取objectName: {}", fileUrl);
            return;
        }

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
            logger.info("文件删除成功: objectName={}", objectName);
        } catch (Exception e) {
            logger.error("文件删除失败: objectName={}", objectName, e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return userId + "/" + uuid + "." + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String extension) {
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从URL中提取objectName
     */
    private String extractObjectNameFromUrl(String url) {
        try {
            // 如果URL包含domain，提取domain后的部分
            if (url.contains(ossProperties.getDomain())) {
                return url.substring(ossProperties.getDomain().length() + 1);
            }
            // 如果URL是相对路径，直接返回
            if (url.startsWith(ossProperties.getPathPrefix())) {
                return url;
            }
            // 尝试从完整URL中提取
            int index = url.indexOf(ossProperties.getPathPrefix());
            if (index != -1) {
                return url.substring(index);
            }
        } catch (Exception e) {
            logger.warn("提取objectName失败: {}", url, e);
        }
        return null;
    }
}
