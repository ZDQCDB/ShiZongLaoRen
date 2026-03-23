package com.shizonglaoren.config;

import com.aliyun.facebody20191230.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 FaceBody 客户端配置（Tea SDK 3.0.8）
 * 照片以 InputStream（Base64解码后）传递，无需 OSS
 */
@Slf4j
@Configuration
public class AliCloudConfig {

    @Value("${aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.face.endpoint:facebody.cn-shanghai.aliyuncs.com}")
    private String faceEndpoint;

    /**
     * 构建 FaceBody 客户端 Bean（Tea SDK Client）
     */
    @Bean
    public Client faceBodyClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(faceEndpoint);
        Client client = new Client(config);
        log.info("阿里云 FaceBody 客户端初始化成功，endpoint={}", faceEndpoint);
        return client;
    }
}
