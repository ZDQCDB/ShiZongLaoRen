package com.shizonglaoren.service.impl;

import com.shizonglaoren.dto.ElderlyDTO;
import com.shizonglaoren.entity.Elderly;
import com.shizonglaoren.entity.ElderlyFaceToken;
import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.repository.ElderlyFaceTokenRepository;
import com.shizonglaoren.repository.ElderlyRepository;
import com.shizonglaoren.service.ElderlyService;
import com.shizonglaoren.service.FaceRecognitionService;
import com.shizonglaoren.service.FileService;
import com.shizonglaoren.util.ElderlyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 老人信息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElderlyServiceImpl implements ElderlyService {

    private final ElderlyRepository elderlyRepository;
    private final ElderlyFaceTokenRepository faceTokenRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final FileService fileService;
    private final ElderlyMapper elderlyMapper;

    /** 每位老人最多注册的照片数量 */
    @Value("${aliyun.face.max-photos-per-elderly:5}")
    private int maxPhotosPerElderly;

    // =========================================================
    // 新增老人基本信息（不含照片）
    // =========================================================

    @Override
    @Transactional
    public ElderlyDTO addElderly(ElderlyDTO dto, Long userId) {
        Elderly elderly = new Elderly();
        elderly.setUserId(userId);
        elderly.setName(dto.getName());
        elderly.setGender(dto.getGender());
        elderly.setAge(dto.getAge());
        elderly.setIdCard(dto.getIdCard());
        elderly.setAddress(dto.getAddress());
        elderly.setContactPhone(dto.getContactPhone());
        elderly.setContactName(dto.getContactName());
        elderly.setRelation(dto.getRelation());
        elderly.setFeatures(dto.getFeatures());
        elderly.setMedicalHistory(dto.getMedicalHistory());
        elderly.setAdditionalInfo(dto.getAdditionalInfo());
        elderly.setPhotoCount(0);
        elderly.setIsActive(1);

        // 先保存获取 ID
        elderly = elderlyRepository.save(elderly);

        // 设置阿里云人脸库 EntityId = 老人ID字符串
        String entityId = String.valueOf(elderly.getId());
        elderly.setFaceEntityId(entityId);
        elderly = elderlyRepository.save(elderly);

        // 在阿里云人脸库中创建对应实体
        try {
            faceRecognitionService.createFaceEntity(entityId);
        } catch (Exception e) {
            log.warn("创建阿里云人脸实体失败（不影响老人信息保存）: {}", e.getMessage());
        }

        log.info("老人信息创建成功，ID: {}, 姓名: {}", elderly.getId(), elderly.getName());
        return elderlyMapper.toDTO(elderly);
    }

    // =========================================================
    // 上传照片（最多5张，不可修改）
    // =========================================================

    @Override
    @Transactional
    public ElderlyDTO addPhoto(Long elderlyId, Long userId, MultipartFile photo) {
        // 1. 查询老人并校验归属
        Elderly elderly = elderlyRepository.findByIdAndIsActive(elderlyId, 1)
                .orElseThrow(() -> new BusinessException("老人信息不存在或已被删除"));

        if (!elderly.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作他人的老人信息");
        }

        // 2. 校验照片数量上限
        int currentCount = elderly.getPhotoCount() != null ? elderly.getPhotoCount() : 0;
        if (currentCount >= maxPhotosPerElderly) {
            throw new BusinessException(
                    String.format("每位老人最多上传 %d 张照片，已达上限", maxPhotosPerElderly));
        }

        // 3. 将图片转换为 Base64
        String imageBase64 = fileService.toBase64(photo);

        // 4. 若是第一张照片，保存至本地并更新 photo_url
        if (currentCount == 0) {
            String photoUrl = fileService.saveFirstPhoto(photo, elderly.getName());
            elderly.setPhotoUrl(photoUrl);
            log.info("老人[{}]第一张照片已保存，URL: {}", elderly.getName(), photoUrl);
        }

        // 5. 注册人脸到阿里云人脸库（使用 Base64）
        String faceToken;
        try {
            faceToken = faceRecognitionService.addFaceByBase64(elderly.getFaceEntityId(), imageBase64);
        } catch (Exception e) {
            log.error("阿里云人脸注册失败，老人ID: {}, 错误: {}", elderlyId, e.getMessage());
            throw new BusinessException("人脸注册失败: " + e.getMessage());
        }

        // 6. 保存 faceToken 到 elderly_face_tokens 表
        ElderlyFaceToken tokenEntity = new ElderlyFaceToken();
        tokenEntity.setElderlyId(elderlyId);
        tokenEntity.setFaceToken(faceToken);
        tokenEntity.setPhotoIndex(currentCount + 1);
        faceTokenRepository.save(tokenEntity);

        // 7. 更新 photo_count
        elderly.setPhotoCount(currentCount + 1);
        elderly = elderlyRepository.save(elderly);

        log.info("老人[{}]第{}张照片注册成功，FaceToken: {}",
                elderly.getName(), currentCount + 1, faceToken);

        return elderlyMapper.toDTO(elderly);
    }

    // =========================================================
    // 更新老人基本信息（不含照片）
    // =========================================================

    @Override
    @Transactional
    public ElderlyDTO updateElderly(Long id, ElderlyDTO dto, Long userId) {
        Elderly elderly = elderlyRepository.findByIdAndIsActive(id, 1)
                .orElseThrow(() -> new BusinessException("老人信息不存在或已被删除"));

        if (!elderly.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改他人的老人信息");
        }

        // 仅允许更新基本信息，照片相关字段（photoUrl、photoCount、faceEntityId）不可改
        if (dto.getName() != null && !dto.getName().isBlank()) {
            elderly.setName(dto.getName());
        }
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            elderly.setGender(dto.getGender());
        }
        if (dto.getAge() != null) {
            elderly.setAge(dto.getAge());
        }
        if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
            elderly.setAddress(dto.getAddress());
        }
        if (dto.getContactPhone() != null && !dto.getContactPhone().isBlank()) {
            elderly.setContactPhone(dto.getContactPhone());
        }
        if (dto.getContactName() != null) {
            elderly.setContactName(dto.getContactName());
        }
        if (dto.getRelation() != null) {
            elderly.setRelation(dto.getRelation());
        }
        if (dto.getFeatures() != null) {
            elderly.setFeatures(dto.getFeatures());
        }
        if (dto.getMedicalHistory() != null) {
            elderly.setMedicalHistory(dto.getMedicalHistory());
        }
        if (dto.getAdditionalInfo() != null) {
            elderly.setAdditionalInfo(dto.getAdditionalInfo());
        }

        elderly = elderlyRepository.save(elderly);
        log.info("老人信息更新成功，ID: {}", id);
        return elderlyMapper.toDTO(elderly);
    }

    // =========================================================
    // 删除老人信息
    // =========================================================

    @Override
    @Transactional
    public void deleteElderly(Long id, Long userId) {
        Elderly elderly = elderlyRepository.findByIdAndIsActive(id, 1)
                .orElseThrow(() -> new BusinessException("老人信息不存在或已被删除"));

        if (!elderly.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除他人的老人信息");
        }

        // 1. 删除阿里云人脸库中的实体（及所有人脸数据）
        if (elderly.getFaceEntityId() != null) {
            faceRecognitionService.deleteFaceEntity(elderly.getFaceEntityId());
        }

        // 2. 删除本地第一张照片文件
        if (elderly.getName() != null) {
            fileService.deleteElderlyPhoto(elderly.getName());
        }

        // 3. 删除 elderly_face_tokens 表中的所有记录
        faceTokenRepository.deleteByElderlyId(id);

        // 4. 软删除老人记录
        elderly.setIsActive(0);
        elderlyRepository.save(elderly);

        log.info("老人信息已删除，ID: {}", id);
    }

    // =========================================================
    // 查询
    // =========================================================

    @Override
    public ElderlyDTO getElderlyById(Long id, Long userId) {
        Elderly elderly = elderlyRepository.findByIdAndIsActive(id, 1)
                .orElseThrow(() -> new BusinessException("老人信息不存在或已被删除"));

        // userId 为 null 时跳过权限校验（管理员查询）
        if (userId != null && !elderly.getUserId().equals(userId)) {
            throw new BusinessException("无权限查看此老人信息");
        }

        return elderlyMapper.toDTO(elderly);
    }

    @Override
    public List<ElderlyDTO> getElderlyListByUser(Long userId) {
        return elderlyRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, 1)
                .stream()
                .map(elderlyMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ElderlyDTO> searchElderlyByName(String name) {
        return elderlyRepository.findByNameContainingAndIsActive(name, 1)
                .stream()
                .map(elderlyMapper::toDTO)
                .collect(Collectors.toList());
    }
}
