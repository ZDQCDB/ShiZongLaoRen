package com.shizonglaoren.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 人脸识别搜索结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceSearchResult {

    /** 是否识别成功找到老人 */
    private boolean found;

    /** 置信度（0~100） */
    private Double confidence;

    /** 识别到的老人信息（found=true 时有值） */
    private ElderlyDTO elderly;

    /** 提示消息 */
    private String message;
}
