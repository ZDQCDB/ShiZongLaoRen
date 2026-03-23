package com.shizonglaoren.service;

import com.shizonglaoren.dto.ElderlyDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 老人信息服务接口
 *
 * 照片策略：
 *  - 每位老人最多上传 5 张照片（photo_count 追踪）
 *  - 第一张照片保存至服务器本地 /data/ShiZongLaoRen/{name}.jpg，用于前端展示
 *  - 所有照片以 Base64 注册到阿里云人脸库，faceToken 存入 elderly_face_tokens 表
 *  - 照片上传后不允许修改
 */
public interface ElderlyService {

    /**
     * 新增老人基本信息（不含照片）
     *
     * @param dto    老人信息（name, gender, address, contactPhone 等）
     * @param userId 录入人用户ID
     * @return 创建后的老人信息（含 id）
     */
    ElderlyDTO addElderly(ElderlyDTO dto, Long userId);

    /**
     * 为已存在的老人上传一张人脸照片（最多5张，不可修改）
     *
     * 处理流程：
     *  1. 校验该老人是否属于当前用户（或当前用户有权限）
     *  2. 校验照片数量是否已达上限（5张）
     *  3. 将图片转换为 Base64 并注册到阿里云人脸库，获取 faceToken
     *  4. 若是第一张（photo_count == 0），将图片保存至本地并更新 photo_url
     *  5. 将 faceToken 存入 elderly_face_tokens 表，photo_count + 1
     *
     * @param elderlyId 老人ID
     * @param userId    操作人用户ID（用于权限校验）
     * @param photo     照片文件
     * @return 更新后的老人信息（包含最新 photo_count、photo_url）
     */
    ElderlyDTO addPhoto(Long elderlyId, Long userId, MultipartFile photo);

    /**
     * 更新老人基本信息（姓名、地址、联系方式、特征描述等，不含照片相关字段）
     *
     * @param id     老人ID
     * @param dto    更新内容
     * @param userId 操作人用户ID
     * @return 更新后的老人信息
     */
    ElderlyDTO updateElderly(Long id, ElderlyDTO dto, Long userId);

    /**
     * 删除老人信息（软删除 + 删除人脸库实体 + 删除本地照片）
     *
     * @param id     老人ID
     * @param userId 操作人用户ID
     */
    void deleteElderly(Long id, Long userId);

    /**
     * 根据ID查询老人信息
     *
     * @param id     老人ID
     * @param userId 操作人用户ID（用于权限校验；管理员可传 null 跳过校验）
     * @return 老人信息DTO
     */
    ElderlyDTO getElderlyById(Long id, Long userId);

    /**
     * 查询当前用户录入的所有老人列表
     *
     * @param userId 用户ID
     * @return 老人列表
     */
    List<ElderlyDTO> getElderlyListByUser(Long userId);

    /**
     * 按姓名模糊搜索老人
     *
     * @param name 姓名关键词
     * @return 匹配的老人列表
     */
    List<ElderlyDTO> searchElderlyByName(String name);
}
