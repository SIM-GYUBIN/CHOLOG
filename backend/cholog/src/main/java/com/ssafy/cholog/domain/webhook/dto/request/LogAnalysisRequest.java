package com.ssafy.cholog.domain.webhook.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogAnalysisRequest {
    @NotBlank(message = "Log ID를 입력해주세요.")
    private String logId;
}
