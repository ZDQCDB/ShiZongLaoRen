package com.shizonglaoren;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 寻找失踪老人系统 - 主启动类
 */
@SpringBootApplication
@EnableAsync
public class ShiZongLaoRenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiZongLaoRenApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  寻找失踪老人系统 - 服务启动成功！");
        System.out.println("  接口地址: http://localhost:8080/api");
        System.out.println("==============================================");
    }
}
