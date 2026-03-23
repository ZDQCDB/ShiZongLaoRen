package com.shizonglaoren.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class LoginRequest {

    /** 微信登录临时凭证code（由 wx.login() 获取）*/
    @NotBlank(message = "登录code不能为空")
    private String code;

    /** 用户昵称（由 wx.getUserProfile() 获取，可选）*/
    private String nickname;

    /** 用户头像URL（可选）*/
    private String avatarUrl;
}
