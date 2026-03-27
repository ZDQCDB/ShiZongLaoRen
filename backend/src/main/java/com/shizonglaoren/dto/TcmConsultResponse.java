package com.shizonglaoren.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TcmConsultResponse {

    /** 问诊记录ID */
    private Long recordId;

    /** AI 返回的中医建议 */
    private String advice;
}
