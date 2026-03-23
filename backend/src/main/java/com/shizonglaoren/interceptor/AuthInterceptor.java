package com.shizonglaoren.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizonglaoren.dto.ApiResponse;
import com.shizonglaoren.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器（验证JWT令牌）
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    /** 存储当前用户ID的ThreadLocal */
    public static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // OPTIONS预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        CURRENT_USER_ID.set(userId);
        // 将 userId 存入 request 属性，供 Controller 通过 request.getAttribute("userId") 读取
        request.setAttribute("userId", userId);
        log.debug("用户 {} 访问 {}", userId, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求完成后清除ThreadLocal，防止内存泄漏
        CURRENT_USER_ID.remove();
    }

    /** 从请求头提取JWT令牌 */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        // 也支持直接从token参数传入（兼容某些场景）
        return request.getParameter("token");
    }

    /** 写入401未授权响应 */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.unauthorized(message))
        );
    }
}
