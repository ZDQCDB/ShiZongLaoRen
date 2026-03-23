package com.shizonglaoren.util;

import com.shizonglaoren.dto.ElderlyDTO;
import com.shizonglaoren.entity.Elderly;
import com.shizonglaoren.entity.User;
import com.shizonglaoren.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

/**
 * 老人信息 DTO 转换器（独立组件，避免循环依赖）
 */
@Component
@RequiredArgsConstructor
public class ElderlyMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final EncryptUtil encryptUtil;

    public ElderlyDTO toDTO(Elderly elderly) {
        if (elderly == null) return null;

        // 查询录入人昵称
        String uploaderName = null;
        try {
            User uploader = userRepository.findById(elderly.getUserId()).orElse(null);
            if (uploader != null) {
                uploaderName = StringUtils.hasText(uploader.getRealName())
                        ? uploader.getRealName() : uploader.getNickname();
            }
        } catch (Exception ignored) {}

        ElderlyDTO dto = new ElderlyDTO();
        dto.setId(elderly.getId());
        dto.setUserId(elderly.getUserId());
        dto.setUploaderName(uploaderName);
        dto.setName(elderly.getName());
        dto.setGender(elderly.getGender());
        dto.setAge(elderly.getAge());

        // 身份证脱敏返回
        if (StringUtils.hasText(elderly.getIdCard())) {
            try {
                dto.setIdCard(encryptUtil.maskIdCard(encryptUtil.decrypt(elderly.getIdCard())));
            } catch (Exception e) {
                dto.setIdCard("****");
            }
        }

        dto.setAddress(elderly.getAddress());
        dto.setContactPhone(elderly.getContactPhone());
        dto.setContactName(elderly.getContactName());
        dto.setRelation(elderly.getRelation());
        dto.setPhotoUrl(elderly.getPhotoUrl());
        dto.setFaceEntityId(elderly.getFaceEntityId());
        dto.setPhotoCount(elderly.getPhotoCount() != null ? elderly.getPhotoCount() : 0);
        dto.setFeatures(elderly.getFeatures());
        dto.setMedicalHistory(elderly.getMedicalHistory());
        dto.setAdditionalInfo(elderly.getAdditionalInfo());
        dto.setIsActive(elderly.getIsActive());

        if (elderly.getCreatedAt() != null) {
            dto.setCreatedAt(elderly.getCreatedAt().format(FORMATTER));
        }
        if (elderly.getUpdatedAt() != null) {
            dto.setUpdatedAt(elderly.getUpdatedAt().format(FORMATTER));
        }
        return dto;
    }
}
