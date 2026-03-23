package com.shizonglaoren.repository;

import com.shizonglaoren.entity.FaceSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人脸识别记录数据访问层
 */
@Repository
public interface FaceSearchLogRepository extends JpaRepository<FaceSearchLog, Long> {

    /** 查询某老人的识别历史 */
    List<FaceSearchLog> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId);

    /** 查询某用户的操作历史 */
    List<FaceSearchLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
