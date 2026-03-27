package com.shizonglaoren.repository;

import com.shizonglaoren.entity.TcmRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TcmRecordRepository extends JpaRepository<TcmRecord, Long> {

    /** 查询用户的问诊历史，按时间倒序 */
    List<TcmRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
