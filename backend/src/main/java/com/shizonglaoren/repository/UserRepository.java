package com.shizonglaoren.repository;

import com.shizonglaoren.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据微信openid查询用户
     */
    Optional<User> findByOpenid(String openid);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据openid判断是否存在
     */
    boolean existsByOpenid(String openid);
}
