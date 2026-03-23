package com.shizonglaoren.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件访问接口
 *
 * GET /api/files/photos/{filename} - 获取存储在本地的老人展示照片
 * 文件实际路径：/data/ShiZongLaoRen/{filename}
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.elderly-photo-dir:/data/ShiZongLaoRen}")
    private String elderlyPhotoDir;

    /**
     * 获取老人展示照片
     *
     * @param filename 文件名（URL编码的老人姓名.jpg，如 %E5%BC%A0%E5%A4%A7%E7%88%B7.jpg）
     */
    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> getElderlyPhoto(@PathVariable String filename) {
        try {
            // URL解码文件名（处理中文）
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            // 安全校验：防止路径穿越
            if (decodedFilename.contains("..") || decodedFilename.contains("/")
                    || decodedFilename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(elderlyPhotoDir, decodedFilename);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                log.warn("照片文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // 根据扩展名确定 Content-Type
            MediaType mediaType = MediaType.IMAGE_JPEG;
            String lowerName = decodedFilename.toLowerCase();
            if (lowerName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lowerName.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400") // 缓存1天
                    .body(resource);

        } catch (Exception e) {
            log.error("获取照片文件失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
