package com.shizonglaoren.service.impl;

import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Set;

/**
 * 文件服务实现
 * 照片存储策略：
 *  - 第一张照片以 JPEG 格式保存至服务器本地 /data/ShiZongLaoRen/{name}.jpg，供前端展示
 *  - 所有照片转换为 Base64 注册到阿里云人脸库（不使用 OSS）
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 照片存储目录（如 /data/ShiZongLaoRen） */
    @Value("${file.elderly-photo-dir:/data/ShiZongLaoRen}")
    private String elderlyPhotoDir;

    /** 服务器公网基础URL（如 http://38.207.179.218:8080） */
    @Value("${file.server-base-url:http://38.207.179.218:8080}")
    private String serverBaseUrl;

    /** 允许的图片 MIME 类型 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /** 单张图片最大大小：10 MB */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    // =========================================================
    // 公开接口实现
    // =========================================================

    @Override
    public String saveFirstPhoto(MultipartFile file, String elderlyName) {
        validateImage(file);

        // 确保目录存在
        ensureDirectoryExists();

        // 文件名：老人姓名.jpg（按需求）
        String filename = sanitizeFilename(elderlyName) + ".jpg";
        Path destPath = Paths.get(elderlyPhotoDir, filename);

        try {
            // 直接保存文件（覆盖同名旧文件）
            file.transferTo(destPath.toFile());
            log.info("老人[{}]第一张照片已保存至: {}", elderlyName, destPath);
        } catch (IOException e) {
            log.error("保存老人照片失败，老人姓名: {}, 路径: {}", elderlyName, destPath, e);
            throw new BusinessException("照片保存失败，请重试");
        }

        // 返回公网访问URL：/api/files/photos/{name}.jpg
        return serverBaseUrl + "/api/files/photos/" + encodeFilename(filename);
    }

    @Override
    public String toBase64(MultipartFile file) {
        validateImage(file);
        try {
            byte[] bytes = file.getBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("图片转Base64失败: {}", e.getMessage(), e);
            throw new BusinessException("图片读取失败，请重试");
        }
    }

    @Override
    public void deleteElderlyPhoto(String elderlyName) {
        String filename = sanitizeFilename(elderlyName) + ".jpg";
        Path photoPath = Paths.get(elderlyPhotoDir, filename);
        File file = photoPath.toFile();
        if (file.exists()) {
            if (file.delete()) {
                log.info("已删除老人[{}]的本地照片: {}", elderlyName, photoPath);
            } else {
                log.warn("删除老人[{}]本地照片失败: {}", elderlyName, photoPath);
            }
        }
    }

    // =========================================================
    // 私有辅助方法
    // =========================================================

    /** 校验图片合法性 */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("图片文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("仅支持 JPG / PNG / WEBP 格式的图片");
        }
    }

    /** 确保本地存储目录存在 */
    private void ensureDirectoryExists() {
        File dir = new File(elderlyPhotoDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("已创建照片存储目录: {}", elderlyPhotoDir);
            } else {
                log.error("无法创建照片存储目录: {}", elderlyPhotoDir);
                throw new BusinessException("服务器存储目录初始化失败");
            }
        }
    }

    /**
     * 清洗文件名中的非法字符，防止路径穿越攻击
     * 保留中文、字母、数字、下划线、连字符
     */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("老人姓名不能为空");
        }
        // 移除文件系统非法字符，保留中文/字母/数字/空格/下划线/连字符
        return name.replaceAll("[/\\\\:*?\"<>|]", "").trim();
    }

    /**
     * URL 编码文件名（处理中文等特殊字符）
     */
    private String encodeFilename(String filename) {
        try {
            return java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return filename;
        }
    }
}
