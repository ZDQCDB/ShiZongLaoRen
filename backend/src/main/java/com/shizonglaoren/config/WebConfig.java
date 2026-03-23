package com.shizonglaoren.config;

import com.shizonglaoren.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * 注意：
 *  - 拦截器路径不包含 server.servlet.context-path（/api），Spring 会自动处理
 *  - 老人照片通过 FileController 的 GET /api/files/photos/{filename} 接口提供，
 *    不再使用 addResourceHandlers 静态映射（便于控制安全和缓存头）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Value("${file.elderly-photo-dir:/data/ShiZongLaoRen}")
    private String elderlyPhotoDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")          // 拦截所有接口
                .excludePathPatterns(
                        "/user/login",           // 登录无需认证
                        "/face/search",          // 人脸搜索允许匿名（路人/警察可扫描）
                        "/files/**"              // 老人照片展示无需认证（公开访问）
                );
    }
}
