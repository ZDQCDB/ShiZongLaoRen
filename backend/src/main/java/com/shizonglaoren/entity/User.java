package com.shizonglaoren.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体（家属、社区工作者、警察等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 微信openid（唯一标识） */
    @Column(name = "openid", nullable = false, unique = true, length = 100)
    private String openid;

    /** 微信昵称 */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /** 头像地址 */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** 手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 真实姓名 */
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 角色：family家属 / community社区 / police警察
     */
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "family";

    /** 是否激活：1激活 / 0禁用 */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Integer isActive = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
