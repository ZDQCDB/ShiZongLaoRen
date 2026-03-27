package com.shizonglaoren.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 中医问诊记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tcm_records")
public class TcmRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 问诊用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 适用人群：elder=老人 / child=小孩 */
    @Column(name = "user_type", nullable = false, length = 10)
    private String userType;

    /** 用户描述的症状 */
    @Column(name = "symptom", nullable = false, length = 500)
    private String symptom;

    /** AI 返回的中医建议 */
    @Column(name = "advice", columnDefinition = "TEXT")
    private String advice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
