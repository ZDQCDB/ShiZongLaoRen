package com.shizonglaoren.controller;

import com.shizonglaoren.dto.ApiResponse;
import com.shizonglaoren.dto.TcmConsultRequest;
import com.shizonglaoren.dto.TcmConsultResponse;
import com.shizonglaoren.entity.TcmRecord;
import com.shizonglaoren.interceptor.AuthInterceptor;
import com.shizonglaoren.service.TcmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 中医问诊接口（需要登录）
 */
@Slf4j
@RestController
@RequestMapping("/tcm")
@RequiredArgsConstructor
public class TcmController {

    private final TcmService tcmService;

    /**
     * POST /api/tcm/stream
     * 流式问诊（SSE，逐字输出）- 前端主要使用此接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConsult(@Valid @RequestBody TcmConsultRequest request) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return tcmService.streamConsult(userId, request);
    }

    /**
     * POST /api/tcm/consult
     * 非流式问诊（兼容保留）
     */
    @PostMapping("/consult")
    public ApiResponse<TcmConsultResponse> consult(@Valid @RequestBody TcmConsultRequest request) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        TcmConsultResponse response = tcmService.consult(userId, request);
        return ApiResponse.success("问诊成功", response);
    }

    /**
     * GET /api/tcm/history
     * 获取当前用户的问诊历史
     */
    @GetMapping("/history")
    public ApiResponse<List<TcmRecord>> history() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        List<TcmRecord> records = tcmService.getHistory(userId);
        return ApiResponse.success(records);
    }
}
