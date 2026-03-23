package com.shizonglaoren.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 * 照片存储策略：
 * - 第一张照片保存至服务器本地目录 /data/ShiZongLaoRen/{elderlyName}.jpg，用于前端展示
 * - 所有照片（最多5张）以 Base64 格式注册到阿里云人脸库，不使用 OSS
 */
public interface FileService {

    /**
     * 将第一张照片保存至服务器本地，返回公网访问URL
     *
     * @param file        图片文件
     * @param elderlyName 老人姓名（用作文件名）
     * @return 照片公网访问URL，格式：http://{serverBaseUrl}/api/files/photos/{name}.jpg
     */
    String saveFirstPhoto(MultipartFile file, String elderlyName);

    /**
     * 将图片文件转换为 Base64 字符串，供阿里云人脸注册使用
     *
     * @param file 图片文件
     * @return Base64 字符串（不含 data:image/... 前缀）
     */
    String toBase64(MultipartFile file);

    /**
     * 删除老人的本地第一张照片
     *
     * @param elderlyName 老人姓名
     */
    void deleteElderlyPhoto(String elderlyName);
}
