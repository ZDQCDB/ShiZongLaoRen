package com.shizonglaoren.service.impl;

import com.aliyun.facebody20191230.Client;
import com.aliyun.facebody20191230.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.shizonglaoren.dto.FaceSearchResult;
import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.repository.ElderlyRepository;
import com.shizonglaoren.service.FaceRecognitionService;
import com.shizonglaoren.util.ElderlyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

/**
 * 人脸识别服务实现（Tea SDK 3.0.8）
 *
 * 照片通过 InputStream 方式传递（Base64 解码后），无需 OSS。
 * EntityId = 老人 ID 字符串，识别成功后可直接用 entityId 查询数据库。
 *
 * 命名说明：
 *   阿里云 API 返回的标识符是 "faceId"，在本系统中对应 ElderlyFaceToken 表的
 *   face_token 列存储，两者等价，命名以数据库列名为准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    /** Tea SDK Client（由 AliCloudConfig 注入） */
    private final Client faceBodyClient;
    private final ElderlyRepository elderlyRepository;
    private final ElderlyMapper elderlyMapper;

    @Value("${aliyun.face.db-name:ShiZongLaoRen}")
    private String dbName;

    @Value("${aliyun.face.confidence-threshold:80.0}")
    private double confidenceThreshold;

    // =========================================================
    // 创建人脸库 + 实体（对应一位老人）
    // =========================================================

    @Override
    public void createFaceEntity(String entityId) {
        // 1. 尝试创建人脸库（已存在时忽略异常）
        try {
            CreateFaceDbRequest dbRequest = new CreateFaceDbRequest()
                    .setName(dbName);
            faceBodyClient.createFaceDb(dbRequest);
            log.info("人脸库[{}]创建/确认成功", dbName);
        } catch (Exception e) {
            log.debug("人脸库已存在或创建时异常（忽略）: {}", e.getMessage());
        }

        // 2. 在人脸库中创建实体（对应一位老人）
        try {
            AddFaceEntityRequest entityRequest = new AddFaceEntityRequest()
                    .setDbName(dbName)
                    .setEntityId(entityId);
            faceBodyClient.addFaceEntity(entityRequest);
            log.info("已在人脸库中创建实体，EntityId: {}", entityId);
        } catch (Exception e) {
            log.error("创建人脸实体失败，EntityId: {}, 错误: {}", entityId, e.getMessage());
            throw new BusinessException("创建人脸实体失败: " + e.getMessage());
        }
    }

    // =========================================================
    // 注册人脸（Base64 → InputStream）
    // =========================================================

    @Override
    public String addFaceByBase64(String entityId, String imageBase64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(
                    stripBase64Prefix(imageBase64));

            AddFaceAdvanceRequest request = new AddFaceAdvanceRequest()
                    .setDbName(dbName)
                    .setEntityId(entityId)
                    .setImageUrlObject(new ByteArrayInputStream(imageBytes));

            AddFaceResponse response = faceBodyClient.addFaceAdvance(
                    request, new RuntimeOptions());

            if (response == null || response.getBody() == null
                    || response.getBody().getData() == null) {
                throw new BusinessException("阿里云人脸注册响应为空");
            }

            String faceId = response.getBody().getData().getFaceId();
            log.info("人脸注册成功，EntityId: {}, FaceId: {}", entityId, faceId);
            return faceId;   // faceId 存入 elderly_face_tokens.face_token 列

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("人脸注册失败，EntityId: {}, 错误: {}", entityId, e.getMessage());
            throw new BusinessException("人脸注册失败: " + e.getMessage());
        }
    }

    // =========================================================
    // 人脸搜索（Base64 → InputStream）
    // =========================================================

    @Override
    public FaceSearchResult searchFaceByBase64(String imageBase64) {
        FaceSearchResult result = new FaceSearchResult();
        try {
            byte[] imageBytes = Base64.getDecoder().decode(
                    stripBase64Prefix(imageBase64));

            SearchFaceAdvanceRequest request = new SearchFaceAdvanceRequest()
                    .setDbName(dbName)
                    .setLimit(1)
                    .setImageUrlObject(new ByteArrayInputStream(imageBytes));

            SearchFaceResponse response = faceBodyClient.searchFaceAdvance(
                    request, new RuntimeOptions());

            if (response == null || response.getBody() == null
                    || response.getBody().getData() == null) {
                result.setFound(false);
                result.setMessage("未找到匹配的老人信息");
                return result;
            }

            List<SearchFaceResponseBody.SearchFaceResponseBodyDataMatchList> matchList =
                    response.getBody().getData().getMatchList();

            if (matchList == null || matchList.isEmpty()) {
                result.setFound(false);
                result.setMessage("未找到匹配的老人信息");
                return result;
            }

            // 第一个 matchList 条目对应查询图中检测到的第一张人脸
            List<SearchFaceResponseBody.SearchFaceResponseBodyDataMatchListFaceItems> faceItems =
                    matchList.get(0).getFaceItems();

            if (faceItems == null || faceItems.isEmpty()) {
                result.setFound(false);
                result.setMessage("未找到匹配的老人信息");
                return result;
            }

            // faceItems 已按相似度降序排列，取第一条
            SearchFaceResponseBody.SearchFaceResponseBodyDataMatchListFaceItems topFace =
                    faceItems.get(0);

            double confidence = topFace.getScore() != null
                    ? topFace.getScore().doubleValue() : 0.0;

            if (confidence < confidenceThreshold) {
                result.setFound(false);
                result.setConfidence(confidence);
                result.setMessage(String.format(
                        "识别置信度(%.1f%%)低于阈值(%.1f%%)，未能确认身份",
                        confidence * 100, confidenceThreshold * 100));
                return result;
            }

            // EntityId = 老人 ID 字符串
            String entityId = topFace.getEntityId();
            Long elderlyId = Long.parseLong(entityId);

            return elderlyRepository.findByIdAndIsActive(elderlyId, 1)
                    .map(elderly -> {
                        FaceSearchResult r = new FaceSearchResult();
                        r.setFound(true);
                        r.setConfidence(confidence);
                        r.setMessage(String.format(
                                "已找到匹配老人，置信度: %.1f%%", confidence * 100));
                        r.setElderly(elderlyMapper.toDTO(elderly));
                        return r;
                    })
                    .orElseGet(() -> {
                        FaceSearchResult r = new FaceSearchResult();
                        r.setFound(false);
                        r.setMessage("人脸库中有记录但数据库中未找到对应老人信息");
                        r.setConfidence(confidence);
                        return r;
                    });

        } catch (NumberFormatException e) {
            log.error("EntityId 格式异常，无法转换为老人 ID: {}", e.getMessage());
            result.setFound(false);
            result.setMessage("系统内部错误，请联系管理员");
            return result;
        } catch (Exception e) {
            log.error("人脸搜索失败，错误: {}", e.getMessage());
            result.setFound(false);
            result.setMessage("人脸识别服务异常: " + e.getMessage());
            return result;
        }
    }

    // =========================================================
    // 删除单张人脸（face_token 列中存储的是 faceId）
    // =========================================================

    @Override
    public void deleteFace(String entityId, String faceToken) {
        try {
            DeleteFaceRequest request = new DeleteFaceRequest()
                    .setDbName(dbName)
                    .setFaceId(faceToken);   // faceToken 字段实际存储的是 faceId
            faceBodyClient.deleteFace(request);
            log.info("已删除人脸，EntityId: {}, FaceId: {}", entityId, faceToken);
        } catch (Exception e) {
            log.warn("删除人脸失败（继续流程），EntityId: {}, FaceId: {}, 错误: {}",
                    entityId, faceToken, e.getMessage());
        }
    }

    // =========================================================
    // 删除整个实体（老人删除时调用）
    // =========================================================

    @Override
    public void deleteFaceEntity(String entityId) {
        try {
            DeleteFaceEntityRequest request = new DeleteFaceEntityRequest()
                    .setDbName(dbName)
                    .setEntityId(entityId);
            faceBodyClient.deleteFaceEntity(request);
            log.info("已删除人脸实体，EntityId: {}", entityId);
        } catch (Exception e) {
            log.warn("删除人脸实体失败（继续流程），EntityId: {}, 错误: {}",
                    entityId, e.getMessage());
        }
    }

    // =========================================================
    // 工具：去除 Base64 前缀（如 data:image/jpeg;base64,）
    // =========================================================

    private String stripBase64Prefix(String base64) {
        if (base64 == null) return "";
        int commaIndex = base64.indexOf(',');
        return commaIndex >= 0 ? base64.substring(commaIndex + 1) : base64;
    }
}
