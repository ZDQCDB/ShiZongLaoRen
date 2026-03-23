package com.shizonglaoren.repository;

import com.shizonglaoren.entity.Elderly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 老人信息数据访问层
 */
@Repository
public interface ElderlyRepository extends JpaRepository<Elderly, Long> {

    /** 查询某用户录入的所有有效老人信息，按创建时间倒序 */
    List<Elderly> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, Integer isActive);

    /** 根据ID查询有效老人 */
    Optional<Elderly> findByIdAndIsActive(Long id, Integer isActive);

    /** 根据人脸 EntityId 查询 */
    Optional<Elderly> findByFaceEntityId(String faceEntityId);

    /** 按姓名模糊搜索（有效老人） */
    List<Elderly> findByNameContainingAndIsActive(String name, Integer isActive);

    /** 统计某用户录入老人数量 */
    long countByUserIdAndIsActive(Long userId, Integer isActive);
}
