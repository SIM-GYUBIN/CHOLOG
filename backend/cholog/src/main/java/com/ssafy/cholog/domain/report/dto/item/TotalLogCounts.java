package com.ssafy.cholog.domain.report.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TotalLogCounts {
    @Schema(example = "13")
    private long overallTotal;    // 전체 로그 수
    @Schema(example = "6")
    private long frontendTotal;   // 프론트엔드 로그 수
    @Schema(example = "7")
    private long backendTotal;    // 백엔드 로그 수
}
