package com.shizonglaoren.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 老人信息DTO
 */
@Data
public class ElderlyDTO {

    private Long id;

    /** 录入人用户ID */
    private Long userId;

    /** 录入人姓名（昵称） */
    private String uploaderName;

    /** 老人姓名 */
    @NotBlank(message = "老人姓名不能为空")
    private String name;

    /** 性别：男/女 */
    @NotBlank(message = "性别不能为空")
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 身份证号 */
    private String idCard;

    /** 家庭住址 */
    @NotBlank(message = "家庭住址不能为空")
    private String address;

    /** 紧急联系电话 */
    @NotBlank(message = "紧急联系电话不能为空")
    private String contactPhone;

    /** 紧急联系人姓名 */
    private String contactName;

    /** 联系人与老人关系 */
    private String relation;

    /**
     * 显示照片URL（第一张照片，服务器本地存储的公网访问地址）
     * 上传照片后自动赋值，前端只读，不可修改
     */
    private String photoUrl;

    /**
     * 阿里云人脸库 EntityId（= 老人ID字符串，创建后自动赋值）
     */
    private String faceEntityId;

    /**
     * 已注册人脸照片数量（0~5）
     */
    private Integer photoCount;

    /** 外貌特征描述 */
    private String features;

    /** 病史/健康状况 */
    private String medicalHistory;

    /** 备注信息 */
    private String additionalInfo;

    /** 是否有效 */
    private Integer isActive;

    /** 创建时间（ISO字符串） */
    private String createdAt;

    /** 更新时间（ISO字符串） */
    private String updatedAt;
}
