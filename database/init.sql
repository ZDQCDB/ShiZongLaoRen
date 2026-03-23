-- =============================================
-- 寻找失踪老人系统 - 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS shizong_laoren DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE shizong_laoren;

-- =============================================
-- 用户表（家属/社区/警察等）
-- =============================================
CREATE TABLE IF NOT EXISTS `users` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid`      VARCHAR(100) NOT NULL                COMMENT '微信openid',
  `nickname`    VARCHAR(50)  DEFAULT NULL             COMMENT '微信昵称',
  `avatar_url`  VARCHAR(500) DEFAULT NULL             COMMENT '头像地址',
  `phone`       VARCHAR(20)  DEFAULT NULL             COMMENT '手机号',
  `real_name`   VARCHAR(50)  DEFAULT NULL             COMMENT '真实姓名',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'family' COMMENT '角色：family家属/community社区/police警察',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否激活：1是/0否',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 老人信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `elderly` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '老人ID',
  `user_id`         BIGINT       NOT NULL                COMMENT '录入人用户ID',
  `name`            VARCHAR(50)  NOT NULL                COMMENT '老人姓名',
  `gender`          VARCHAR(10)  NOT NULL                COMMENT '性别：男/女',
  `age`             INT          DEFAULT NULL             COMMENT '年龄',
  `id_card`         VARCHAR(100) DEFAULT NULL             COMMENT '身份证号（加密存储）',
  `address`         VARCHAR(300) NOT NULL                COMMENT '家庭住址',
  `contact_phone`   VARCHAR(20)  NOT NULL                COMMENT '紧急联系电话',
  `contact_name`    VARCHAR(50)  DEFAULT NULL             COMMENT '紧急联系人姓名',
  `relation`        VARCHAR(50)  DEFAULT NULL             COMMENT '联系人与老人关系',
  `photo_url`       VARCHAR(500) DEFAULT NULL             COMMENT '显示照片URL（第一张，本地服务器）',
  `face_entity_id`  VARCHAR(100) DEFAULT NULL             COMMENT '阿里云人脸库EntityId（= 老人ID字符串）',
  `photo_count`     INT          NOT NULL DEFAULT 0       COMMENT '已上传人脸照片数（最多5张）',
  `features`        VARCHAR(200) DEFAULT NULL             COMMENT '外貌特征描述',
  `medical_history` TEXT         DEFAULT NULL             COMMENT '病史/健康状况',
  `additional_info` TEXT         DEFAULT NULL             COMMENT '备注信息',
  `is_active`       TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否有效：1是/0已删除',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_name` (`name`),
  KEY `idx_contact_phone` (`contact_phone`),
  KEY `idx_face_entity_id` (`face_entity_id`),
  CONSTRAINT `fk_elderly_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人信息表';

-- =============================================
-- 老人人脸Token表（每人最多5张，每张对应一个Token）
-- =============================================
CREATE TABLE IF NOT EXISTS `elderly_face_tokens` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `elderly_id`   BIGINT       NOT NULL                COMMENT '老人ID',
  `face_token`   VARCHAR(200) NOT NULL                COMMENT '阿里云人脸Token',
  `photo_index`  INT          NOT NULL DEFAULT 1       COMMENT '第几张照片（1~5）',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_elderly_id` (`elderly_id`),
  CONSTRAINT `fk_token_elderly` FOREIGN KEY (`elderly_id`) REFERENCES `elderly` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人人脸Token表';

-- =============================================
-- 人脸识别记录表（审计日志）
-- =============================================
CREATE TABLE IF NOT EXISTS `face_search_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT       DEFAULT NULL             COMMENT '操作人ID（可为空，匿名识别）',
  `elderly_id`  BIGINT       DEFAULT NULL             COMMENT '识别到的老人ID',
  `confidence`  DECIMAL(5,2) DEFAULT NULL             COMMENT '识别置信度',
  `result`      VARCHAR(20)  DEFAULT NULL             COMMENT '结果：matched/not_found/error',
  `ip_address`  VARCHAR(50)  DEFAULT NULL             COMMENT '请求方IP地址',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_elderly_id` (`elderly_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人脸识别记录表';
