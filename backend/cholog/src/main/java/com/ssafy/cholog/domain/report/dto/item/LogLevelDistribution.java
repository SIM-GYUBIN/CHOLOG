package com.ssafy.cholog.domain.report.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LogLevelDistribution {
    private List<LogLevelDetail> distribution; // 각 레벨별 상세 정보 리스트
    @Schema(example = "13")
    private long totalLogsInDistribution;

    @Getter
    @Builder
    public static class LogLevelDetail {
        @Schema(example = "INFO")
        private String level;       // 로그 레벨 (예: "INFO", "ERROR", "WARN")
        @Schema(example = "8")
        private long count;         // 해당 레벨의 로그 수
        @Schema(example = "61.54")
        private double percentage;  // 전체 대비 비율 (0.0 ~ 100.0)
    }
}
