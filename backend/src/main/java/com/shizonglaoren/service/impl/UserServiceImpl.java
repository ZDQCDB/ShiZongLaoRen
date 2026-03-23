package com.shizonglaoren.service.impl;

import com.shizonglaoren.dto.LoginRequest;
import com.shizonglaoren.dto.LoginResponse;
import com.shizonglaoren.entity.User;
import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.repository.UserRepository;
import com.shizonglaoren.service.UserService;
import com.shizonglaoren.util.JwtUtil;
import com.shizonglaoren.util.WeChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 用户业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WeChatUtil weChatUtil;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 用code换取openid
        String openid = weChatUtil.getOpenId(request.getCode());
        log.info("微信登录，openid={}", openid);

        // 2. 查询是否已注册，没有则新建
        Optional<User> existingUser = userRepository.findByOpenid(openid);
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            user = User.builder()
                    .openid(openid)
                    .nickname(request.getNickname())
                    .avatarUrl(request.getAvatarUrl())
                    .role("family")
                    .isActive(1)
                    .build();
            user = userRepository.save(user);
            log.info("新用户注册，userId={}, openid={}", user.getId(), openid);
        } else {
            user = existingUser.get();
            // 更新昵称和头像（如果有传入）
            boolean updated = false;
            if (StringUtils.hasText(request.getNickname()) && !request.getNickname().equals(user.getNickname())) {
                user.setNickname(request.getNickname());
                updated = true;
            }
            if (StringUtils.hasText(request.getAvatarUrl()) && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(request.getAvatarUrl());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
        }

        if (user.getIsActive() != 1) {
            throw BusinessException.unauthorized("账号已被禁用，请联系管理员");
        }

        // 3. 签发JWT令牌
        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isNewUser(isNewUser)
                .build();
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    @Override
    @Transactional
    public User updateUser(Long userId, String realName, String phone) {
        User user = getUserById(userId);
        if (StringUtils.hasText(realName)) user.setRealName(realName);
        if (StringUtils.hasText(phone)) user.setPhone(phone);
        return userRepository.save(user);
    }
}
