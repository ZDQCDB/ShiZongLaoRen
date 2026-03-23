package com.shizonglaoren.service;

import com.shizonglaoren.dto.LoginRequest;
import com.shizonglaoren.dto.LoginResponse;
import com.shizonglaoren.entity.User;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 微信登录（code换openid，创建或返回用户，签发JWT）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据用户ID查询用户
     */
    User getUserById(Long userId);

    /**
     * 更新用户信息（手机号、真实姓名）
     */
    User updateUser(Long userId, String realName, String phone);
}
