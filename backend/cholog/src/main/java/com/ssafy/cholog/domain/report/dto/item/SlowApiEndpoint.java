package com.ssafy.cholog.domain.report.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlowApiEndpoint {
    @Schema(example = "1")
    private int rank;                  // 순위 (1-5)
    @Schema(example = "POST")
    private String httpMethod;         // HTTP 요청 메소드 (예: "GET", "POST")
    @Schema(example = "http://localhost:8080/test")
    private String requestPath;        // 요청 경로 (백엔드 `requestUri`)
    @Schema(example = "2336")
    private long averageResponseTimeMs; // 평균 응답 시간 (밀리초)
    @Schema(example = "2336")
    private long maxResponseTimeMs;     // 최대 응답 시간 (밀리초)
    @Schema(example = "1")
    private long totalRequests;         // 해당 경로의 총 요청 횟수
}
