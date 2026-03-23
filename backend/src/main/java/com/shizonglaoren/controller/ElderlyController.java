package com.shizonglaoren.controller;

import com.shizonglaoren.dto.ApiResponse;
import com.shizonglaoren.dto.ElderlyDTO;
import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.service.ElderlyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 老人信息管理接口
 *
 * 照片上传策略：
 *  - POST /elderly          → 先创建老人基本信息，获取老人ID
 *  - POST /elderly/{id}/photo → 再逐张上传照片（最多5张，不可修改）
 *  - 照片通过 Base64 注册至阿里云人脸库，第一张额外保存到服务器本地用于展示
 */
@Slf4j
@RestController
@RequestMapping("/elderly")
@RequiredArgsConstructor
public class ElderlyController {

    private final ElderlyService elderlyService;

    // =========================================================
    // 获取用户ID的辅助方法（由 AuthInterceptor 写入 request）
    // =========================================================
    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        return Long.valueOf(userId.toString());
    }

    // =========================================================
    // 1. 创建老人基本信息（不含照片）
    // =========================================================

    /**
     * POST /api/elderly
     * 创建老人基本信息（姓名、性别、地址、联系方式等）。
     * 创建成功后获取老人ID，再分别调用 POST /api/elderly/{id}/photo 上传照片。
     */
    @PostMapping
    public ApiResponse<ElderlyDTO> addElderly(
            @Valid @RequestBody ElderlyDTO dto,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        ElderlyDTO result = elderlyService.addElderly(dto, userId);
        return ApiResponse.success("老人信息创建成功", result);
    }

    // =========================================================
    // 2. 上传老人照片（最多5张，不可修改）
    // =========================================================

    /**
     * POST /api/elderly/{id}/photo
     * 为指定老人上传一张人脸照片（Content-Type: multipart/form-data）。
     *
     * 业务规则：
     *  - 最多上传5张，超出返回错误
     *  - 照片上传后不可修改或删除
     *  - 第一张照片同时保存至服务器本地 /data/ShiZongLaoRen/{name}.jpg
     *  - 所有照片以 Base64 注册到阿里云人脸库，entity_id = 老人ID
     *
     * @param id    老人ID（路径参数）
     * @param photo 照片文件（form字段名：photo，JPG/PNG/WEBP，最大10MB）
     */
    @PostMapping("/{id}/photo")
    public ApiResponse<ElderlyDTO> addPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        ElderlyDTO result = elderlyService.addPhoto(id, userId, photo);
        return ApiResponse.success(
                String.format("照片上传成功（已注册 %d/5 张）", result.getPhotoCount()),
                result);
    }

    // =========================================================
    // 3. 更新老人基本信息（不含照片）
    // =========================================================

    /**
     * PUT /api/elderly/{id}
     * 更新老人基本信息（姓名、地址、联系方式等）。
     * 注意：照片相关字段（photoUrl、photoCount）不可通过此接口修改。
     */
    @PutMapping("/{id}")
    public ApiResponse<ElderlyDTO> updateElderly(
            @PathVariable Long id,
            @RequestBody ElderlyDTO dto,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        ElderlyDTO result = elderlyService.updateElderly(id, dto, userId);
        return ApiResponse.success("老人信息更新成功", result);
    }

    // =========================================================
    // 4. 删除老人信息
    // =========================================================

    /**
     * DELETE /api/elderly/{id}
     * 软删除老人信息，同时：
     *  - 删除阿里云人脸库中的实体及人脸数据
     *  - 删除服务器本地第一张照片文件
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteElderly(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        elderlyService.deleteElderly(id, userId);
        return ApiResponse.success("老人信息已删除", null);
    }

    // =========================================================
    // 5. 查询接口
    // =========================================================

    /**
     * GET /api/elderly/{id}
     * 根据ID查询老人详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<ElderlyDTO> getElderlyById(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        ElderlyDTO result = elderlyService.getElderlyById(id, userId);
        return ApiResponse.success(result);
    }

    /**
     * GET /api/elderly/my
     * 查询当前用户录入的所有老人列表。
     */
    @GetMapping("/my")
    public ApiResponse<List<ElderlyDTO>> getMyElderlyList(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<ElderlyDTO> list = elderlyService.getElderlyListByUser(userId);
        return ApiResponse.success(list);
    }

    /**
     * GET /api/elderly/search?name=xxx
     * 按姓名模糊搜索老人。
     */
    @GetMapping("/search")
    public ApiResponse<List<ElderlyDTO>> searchElderlyByName(
            @RequestParam String name,
            HttpServletRequest request) {
        getUserId(request); // 校验登录
        List<ElderlyDTO> list = elderlyService.searchElderlyByName(name);
        return ApiResponse.success(list);
    }
}
