package com.shizonglaoren.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 老人人脸Token实体
 * 每位老人最多上传5张照片，每张照片注册到阿里云人脸库后返回一个faceToken
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "elderly_face_tokens")
public class ElderlyFaceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的老人ID */
    @Column(name = "elderly_id", nullable = false)
    private Long elderlyId;

    /** 阿里云人脸库返回的 FaceToken */
    @Column(name = "face_token", nullable = false, length = 200)
    private String faceToken;

    /** 第几张照片（1~5） */
    @Column(name = "photo_index", nullable = false)
    private Integer photoIndex;

    /** 上传时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
