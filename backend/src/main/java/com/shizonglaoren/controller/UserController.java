package com.shizonglaoren.controller;

import com.shizonglaoren.dto.ApiResponse;
import com.shizonglaoren.dto.LoginRequest;
import com.shizonglaoren.dto.LoginResponse;
import com.shizonglaoren.entity.User;
import com.shizonglaoren.interceptor.AuthInterceptor;
import com.shizonglaoren.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口（登录、个人信息）
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/user/login
     * 微信登录（不需要认证）
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * GET /api/user/profile
     * 获取当前用户信息（需要认证）
     */
    @GetMapping("/profile")
    public ApiResponse<User> getProfile() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        User user = userService.getUserById(userId);
        return ApiResponse.success(user);
    }

    /**
     * PUT /api/user/profile
     * 更新个人信息（需要认证）
     */
    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        String realName = body.get("realName");
        String phone = body.get("phone");
        User user = userService.updateUser(userId, realName, phone);
        return ApiResponse.success("更新成功", user);
    }
}
