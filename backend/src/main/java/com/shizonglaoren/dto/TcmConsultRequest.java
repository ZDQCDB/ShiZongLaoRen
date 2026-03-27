package com.shizonglaoren.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TcmConsultRequest {

    /** 适用人群：elder=老人 / child=小孩 */
    @NotBlank(message = "请选择适用人群")
    private String userType;

    /** 症状描述 */
    @NotBlank(message = "请描述症状")
    @Size(max = 300, message = "症状描述不超过300字")
    private String symptom;
}
