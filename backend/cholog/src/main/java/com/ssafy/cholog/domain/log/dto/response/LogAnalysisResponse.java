package com.ssafy.cholog.domain.log.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogAnalysisResponse {
    @Schema(description = "LLM으로부터 받은 분석 결과 텍스트")
    private String analysisResult;
    @Schema(description = "분석에 사용된 Groq 모델명")
    private String modelUsed;
}
