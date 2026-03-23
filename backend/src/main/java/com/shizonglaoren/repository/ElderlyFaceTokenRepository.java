package com.shizonglaoren.repository;

import com.shizonglaoren.entity.ElderlyFaceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 老人人脸Token数据访问层
 */
@Repository
public interface ElderlyFaceTokenRepository extends JpaRepository<ElderlyFaceToken, Long> {

    /** 查询某老人所有人脸Token */
    List<ElderlyFaceToken> findByElderlyIdOrderByPhotoIndex(Long elderlyId);

    /** 统计某老人已注册的照片数量 */
    int countByElderlyId(Long elderlyId);

    /** 删除某老人的所有人脸Token */
    void deleteByElderlyId(Long elderlyId);
}
