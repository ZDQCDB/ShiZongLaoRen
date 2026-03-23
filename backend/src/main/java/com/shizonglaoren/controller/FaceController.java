package com.shizonglaoren.controller;

import com.shizonglaoren.dto.ApiResponse;
import com.shizonglaoren.dto.FaceSearchResult;
import com.shizonglaoren.entity.FaceSearchLog;
import com.shizonglaoren.repository.FaceSearchLogRepository;
import com.shizonglaoren.service.FaceRecognitionService;
import com.shizonglaoren.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 人脸识别接口
 *
 * POST /api/face/search - 上传照片在人脸库中搜索匹配的老人
 * 照片以 Base64 形式传给阿里云，识别成功后通过 EntityId（=老人ID）查询数据库
 */
@Slf4j
@RestController
@RequestMapping("/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceRecognitionService faceRecognitionService;
    private final FileService fileService;
    private final FaceSearchLogRepository faceSearchLogRepository;

    /**
     * POST /api/face/search
     * 人脸识别搜索接口
     *
     * @param photo   待识别照片（multipart/form-data，字段名：photo）
     * @param request HTTP请求（获取用户ID和IP）
     */
    @PostMapping("/search")
    public ApiResponse<FaceSearchResult> searchFace(
            @RequestParam("photo") MultipartFile photo,
            HttpServletRequest request) {

        log.info("收到人脸识别请求，文件大小: {} bytes", photo.getSize());

        // 1. 将照片转 Base64
        String imageBase64 = fileService.toBase64(photo);

        // 2. 在阿里云人脸库中搜索
        FaceSearchResult result = faceRecognitionService.searchFaceByBase64(imageBase64);

        // 3. 记录搜索日志
        saveSearchLog(request, result);

        if (result.isFound()) {
            return ApiResponse.success(result.getMessage(), result);
        } else {
            return ApiResponse.success(result.getMessage(), result);
        }
    }

    // =========================================================
    // 私有：保存搜索日志
    // =========================================================
    private void saveSearchLog(HttpServletRequest request, FaceSearchResult result) {
        try {
            FaceSearchLog searchLog = new FaceSearchLog();

            // 尝试获取登录用户ID
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr != null) {
                searchLog.setUserId(Long.valueOf(userIdAttr.toString()));
            }

            if (result.isFound() && result.getElderly() != null) {
                searchLog.setElderlyId(result.getElderly().getId());
                searchLog.setConfidence(result.getConfidence() != null
                        ? result.getConfidence().floatValue() : null);
                searchLog.setResult("matched");
            } else {
                searchLog.setResult("not_found");
            }

            searchLog.setIpAddress(getClientIp(request));
            searchLog.setCreatedAt(LocalDateTime.now());
            faceSearchLogRepository.save(searchLog);
        } catch (Exception e) {
            log.warn("保存人脸搜索日志失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /** 获取客户端真实 IP */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
