package com.shizonglaoren.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 老人信息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "elderly")
public class Elderly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 录入人用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 老人姓名 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 性别：男/女 */
    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    /** 年龄 */
    @Column(name = "age")
    private Integer age;

    /** 身份证号（加密存储） */
    @Column(name = "id_card", length = 100)
    private String idCard;

    /** 家庭住址 */
    @Column(name = "address", nullable = false, length = 300)
    private String address;

    /** 紧急联系电话 */
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    /** 紧急联系人姓名 */
    @Column(name = "contact_name", length = 50)
    private String contactName;

    /** 联系人与老人关系 */
    @Column(name = "relation", length = 50)
    private String relation;

    /**
     * 显示照片URL（第一张照片在服务器本地的访问URL）
     * 格式：http://{serverBaseUrl}/api/files/photos/{name}.jpg
     * 照片文件实际存储于：/data/ShiZongLaoRen/{name}.jpg
     */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /**
     * 阿里云人脸库 EntityId（固定为 String.valueOf(id)，即老人ID字符串）
     * 通过 EntityId 可直接在数据库中定位老人记录
     */
    @Column(name = "face_entity_id", length = 100)
    private String faceEntityId;

    /**
     * 已上传人脸照片数量（0~5）
     * 每张照片的 faceToken 存储于 elderly_face_tokens 表
     */
    @Column(name = "photo_count", nullable = false)
    private Integer photoCount = 0;

    /** 外貌特征描述 */
    @Column(name = "features", length = 200)
    private String features;

    /** 病史/健康状况 */
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    /** 备注信息 */
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    /** 是否有效：1有效/0已删除 */
    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.photoCount == null) {
            this.photoCount = 0;
        }
        if (this.isActive == null) {
            this.isActive = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
