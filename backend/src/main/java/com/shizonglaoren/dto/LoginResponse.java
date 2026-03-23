package com.shizonglaoren.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应（含JWT token及用户信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT令牌 */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 微信昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 角色 */
    private String role;

    /** 是否新用户（首次登录） */
    private Boolean isNewUser;
}
