package com.shizonglaoren.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人脸识别记录实体（审计日志）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_search_log")
public class FaceSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人ID（可为空，允许未登录用户扫描） */
    @Column(name = "user_id")
    private Long userId;

    /** 识别到的老人ID（未识别时为 null） */
    @Column(name = "elderly_id")
    private Long elderlyId;

    /** 识别置信度（0~100） */
    @Column(name = "confidence")
    private Float confidence;

    /** 识别结果：matched / not_found / error */
    @Column(name = "result", length = 20)
    private String result;

    /** 请求方IP地址 */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
