package com.ssafy.cholog.domain.log.dto.response;

import com.ssafy.cholog.domain.log.entity.LogListDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class LogListResponse {
    @Schema(example = "W46RvpYBzb40v9OYAxOk")
    private String id;
    @Schema(example = "2023-10-01T12:34:56Z")
    private Instant timestamp; // 주요 타임스탬프
    @Schema(example = "INFO")
    private String level;
    @Schema(example = "User clicked on button#sendFetch")
    private String message;
    @Schema(example = "frontend")
    private String source;
    @Schema(example = "dev")
    private String environment;

    public static LogListResponse fromLogListDocument(LogListDocument doc) {
        return LogListResponse.builder()
                .id(doc.getId())
                .timestamp(doc.getTimestampOriginal())
                .level(doc.getLevel())
                .message(doc.getMessage())
                .source(doc.getSource())
                .environment(doc.getEnvironment())
                .build();
    }
}
