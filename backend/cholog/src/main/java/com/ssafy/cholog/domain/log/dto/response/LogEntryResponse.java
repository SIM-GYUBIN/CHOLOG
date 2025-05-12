package com.ssafy.cholog.domain.log.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class LogEntryResponse {
    @Schema(example = "W46RvpYBzb40v9OYAxOk")
    private String id;
    @Schema(example = "2023-10-01T12:34:56Z")
    private Instant timestamp; // 주요 타임스탬프 (예: timestampEs)
//    @Schema(example = "3")
//    private Long sequence;
    @Schema(example = "INFO")
    private String level;
    @Schema(example = "User clicked on button#sendFetch")
    private String message;
    @Schema(example = "frontend")
    private String source;
    @Schema(example = "123123")
    private String projectKey;
    @Schema(example = "dev")
    private String environment;
    @Schema(example = "23eb3dc6-b6da-4374-8639-302fe3743a52")
    private String traceId;
    @Schema(example = "cholog")
    private String logger;
    @Schema(example = "event")
    private String logType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.ClientInfo client;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.HttpInfo http;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.ErrorInfo error;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.EventInfo event;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> payload;

    public static LogEntryResponse fromLogDocument(LogDocument doc) {
        return LogEntryResponse.builder()
                .id(doc.getId())
//                .timestamp(doc.getTimestampEs() != null ? doc.getTimestampEs() : doc.getTimestampOriginal()) // 기본으로 @timestamp 사용
                .timestamp(doc.getTimestampOriginal() != null ? doc.getTimestampOriginal() : doc.getTimestampEs()) // 기본으로 timestamp 사용
//                .sequence(doc.getSequence())
                .level(doc.getLevel())
                .message(doc.getMessage())
                .source(doc.getSource())
                .projectKey(doc.getProjectKey())
                .environment(doc.getEnvironment())
                .traceId(doc.getTraceId())
                .logger(doc.getLogger())
                .logType(doc.getLogType())
                .client(doc.getClient())
                .http(doc.getHttp())
                .error(doc.getError())
                .event(doc.getEvent())
                .payload(doc.getPayload())
                .build();
    }
}
