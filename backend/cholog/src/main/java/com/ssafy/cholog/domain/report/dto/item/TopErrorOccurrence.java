package com.ssafy.cholog.domain.report.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopErrorOccurrence {
    @Schema(example = "1")
    private int rank;                 // 순위 (1-5)
    @Schema(example = "XHRError")
    private String errorIdentifier;   // 에러 식별자 (예: 프론트 `error.type` 또는 백엔드 `error.className`)
    @Schema(example = "1")
    private long occurrenceCount;     // 발생 건수
    @Schema(example = "frontend")
    private String sourceOrigin;      // 에러 발생 출처 ("frontend" 또는 "backend")
}
