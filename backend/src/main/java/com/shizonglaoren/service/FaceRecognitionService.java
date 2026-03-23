package com.shizonglaoren.service;

import com.shizonglaoren.dto.FaceSearchResult;

/**
 * 人脸识别服务接口
 *
 * 照片存储策略：
 *  - 不使用 OSS，照片直接以 Base64 格式传给阿里云 FaceBody 接口
 *  - EntityId = 老人ID字符串，匹配成功后可直接定位数据库记录
 */
public interface FaceRecognitionService {

    /**
     * 在阿里云人脸库中创建实体（对应一位老人）
     *
     * @param entityId 实体ID（= 老人ID字符串）
     */
    void createFaceEntity(String entityId);

    /**
     * 向阿里云人脸库的指定实体中注册一张人脸（使用 Base64 图像数据）
     *
     * @param entityId    实体ID（= 老人ID字符串）
     * @param imageBase64 图片 Base64 字符串（不含 data: 前缀）
     * @return 阿里云返回的 faceToken（用于后续删除）
     */
    String addFaceByBase64(String entityId, String imageBase64);

    /**
     * 使用 Base64 图像在人脸库中搜索匹配的老人
     *
     * @param imageBase64 图片 Base64 字符串
     * @return 搜索结果（含老人ID、置信度等）
     */
    FaceSearchResult searchFaceByBase64(String imageBase64);

    /**
     * 删除阿里云人脸库中某实体的某张人脸
     *
     * @param entityId  实体ID
     * @param faceToken 要删除的人脸 Token
     */
    void deleteFace(String entityId, String faceToken);

    /**
     * 删除阿里云人脸库中整个实体（老人被删除时调用）
     *
     * @param entityId 实体ID（= 老人ID字符串）
     */
    void deleteFaceEntity(String entityId);
}
