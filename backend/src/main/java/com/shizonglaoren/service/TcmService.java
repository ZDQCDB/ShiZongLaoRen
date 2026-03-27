package com.shizonglaoren.service;

import com.shizonglaoren.dto.TcmConsultRequest;
import com.shizonglaoren.dto.TcmConsultResponse;
import com.shizonglaoren.entity.TcmRecord;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface TcmService {

    /** 发起中医问诊（流式输出，逐字返回） */
    SseEmitter streamConsult(Long userId, TcmConsultRequest request);

    /** 发起中医问诊（非流式，一次返回，兼容旧版） */
    TcmConsultResponse consult(Long userId, TcmConsultRequest request);

    /** 查询用户问诊历史 */
    List<TcmRecord> getHistory(Long userId);
}
