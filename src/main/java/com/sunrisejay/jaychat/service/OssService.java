package com.sunrisejay.jaychat.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sunrisejay.jaychat.config.OssProperties;
import com.sunrisejay.jaychat.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * 阿里云OSS服务
 * 用于上传文件到阿里云对象存储
 */
@Service
public class OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssService.class);
    private static final int AVATAR_SIZE_SMALL = 256;
    private static final int AVATAR_SIZE_LARGE = 512;
    private static final int AVATAR_MIN_SIZE = 128;

    private final OssProperties ossProperties;

    public OssService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 上传聊天图片到OSS（保持原图，不裁剪）
     * @param file 图片文件
     * @param userId 用户ID（用于生成文件路径）
     * @return 图片访问URL
     */
    public String uploadChatImage(MultipartFile file, Long userId) {
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

        // 验证文件大小（限制20MB，聊天图片可以比头像大）
        long fileSize = file.getSize();
        if (fileSize > 20 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过20MB");
        }

        OSS ossClient = null;
        try {
            // 验证是否为有效图片
            BufferedImage sourceImage = ImageIO.read(file.getInputStream());
            if (sourceImage == null) {
                throw new BusinessException("文件内容不是有效图片");
            }

            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            // 生成文件名，使用 chat/ 前缀区分聊天图片
            String fileName = generateChatImageFileName(userId, fileExtension);
            String objectName = "chat/" + fileName;

            // 上传原图（不裁剪，不压缩）
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/" + (fileExtension.equals("jpg") ? "jpeg" : fileExtension));
            metadata.setContentLength(file.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    objectName,
                    file.getInputStream()
            );
            putObjectRequest.setMetadata(metadata);
            ossClient.putObject(putObjectRequest);

            String fileUrl = ossProperties.getDomain() + "/" + objectName;
            logger.info("聊天图片上传成功: userId={}, objectName={}", userId, objectName);
            return fileUrl;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("聊天图片上传失败: userId={}", userId, e);
            throw new BusinessException("图片上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件到OSS（头像专用，会裁剪成方形）
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
            BufferedImage sourceImage = ImageIO.read(file.getInputStream());
            if (sourceImage == null) {
                throw new BusinessException("文件内容不是有效图片");
            }
            if (sourceImage.getWidth() < AVATAR_MIN_SIZE || sourceImage.getHeight() < AVATAR_MIN_SIZE) {
                throw new BusinessException("头像分辨率过低，至少需要 " + AVATAR_MIN_SIZE + "x" + AVATAR_MIN_SIZE);
            }

            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            // 统一生成方形头像的两个尺寸，默认返回高清版本URL
            String fileName = generateFileName(userId);
            String smallObjectName = ossProperties.getPathPrefix() + fileName + "_" + AVATAR_SIZE_SMALL + ".jpg";
            String largeObjectName = ossProperties.getPathPrefix() + fileName + "_" + AVATAR_SIZE_LARGE + ".jpg";

            uploadResizedImage(ossClient, sourceImage, smallObjectName, AVATAR_SIZE_SMALL);
            uploadResizedImage(ossClient, sourceImage, largeObjectName, AVATAR_SIZE_LARGE);

            String fileUrl = ossProperties.getDomain() + "/" + largeObjectName;
            logger.info("头像上传成功: userId={}, small={}, large={}", userId, smallObjectName, largeObjectName);
            return fileUrl;

        } catch (BusinessException e) {
            throw e;
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

            // 删除当前头像对象
            try {
                ossClient.deleteObject(ossProperties.getBucketName(), objectName);
                logger.info("文件删除成功: objectName={}", objectName);
            } catch (Exception e) {
                logger.warn("文件删除失败: objectName={}", objectName, e);
            }

            // 如果是标准化头像（xxx_256.jpg / xxx_512.jpg），尝试联动删除另一种尺寸
            if (objectName.endsWith("_" + AVATAR_SIZE_SMALL + ".jpg")) {
                String other = objectName.replace("_" + AVATAR_SIZE_SMALL + ".jpg", "_" + AVATAR_SIZE_LARGE + ".jpg");
                try {
                    ossClient.deleteObject(ossProperties.getBucketName(), other);
                    logger.info("文件删除成功: objectName={}", other);
                } catch (Exception e) {
                    logger.warn("文件删除失败: objectName={}", other, e);
                }
            } else if (objectName.endsWith("_" + AVATAR_SIZE_LARGE + ".jpg")) {
                String other = objectName.replace("_" + AVATAR_SIZE_LARGE + ".jpg", "_" + AVATAR_SIZE_SMALL + ".jpg");
                try {
                    ossClient.deleteObject(ossProperties.getBucketName(), other);
                    logger.info("文件删除成功: objectName={}", other);
                } catch (Exception e) {
                    logger.warn("文件删除失败: objectName={}", other, e);
                }
            }
        } catch (Exception e) {
            logger.error("文件删除失败: objectName={}", objectName, e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


    /**
     * 生成文件名（不含扩展名）- 头像专用
     */
    private String generateFileName(Long userId) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return userId + "/" + uuid;
    }

    /**
     * 生成聊天图片文件名（包含扩展名）
     */
    private String generateChatImageFileName(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return userId + "/" + uuid + "." + extension;
    }

    private void uploadResizedImage(OSS ossClient, BufferedImage source, String objectName, int targetSize) throws Exception {
        BufferedImage square = cropToSquare(source);
        BufferedImage resized = resizeImage(square, targetSize, targetSize);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength(imageBytes.length);

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossProperties.getBucketName(),
                objectName,
                new ByteArrayInputStream(imageBytes)
        );
        putObjectRequest.setMetadata(metadata);
        ossClient.putObject(putObjectRequest);
    }

    private BufferedImage cropToSquare(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = square.createGraphics();
        try {
            g2d.drawImage(source, 0, 0, size, size, x, y, x + size, y + size, null);
        } finally {
            g2d.dispose();
        }
        return square;
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(source, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }
        return resized;
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
