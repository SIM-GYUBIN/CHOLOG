package com.ssafy.cholog.domain.log.dto.response;

import com.ssafy.cholog.domain.log.entity.LogDocument;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class LogEntryResponse {
    private String id;
    private Instant timestamp; // 주요 타임스탬프 (예: timestampEs)
    private Long sequence;
    private String level;
    private String message;
    private String source;
    private String projectKey;
    private String environment;
    private String traceId;
    private String logger;
    private String logType;
    private LogDocument.ClientInfo client;
    private LogDocument.HttpInfo http;
    private LogDocument.ErrorInfo error;
    private LogDocument.EventInfo event;
    private Map<String, Object> payload;

    public static LogEntryResponse fromLogDocument(LogDocument doc) {
        return LogEntryResponse.builder()
                .id(doc.getId())
//                .timestamp(doc.getTimestampEs() != null ? doc.getTimestampEs() : doc.getTimestampOriginal()) // 기본으로 @timestamp 사용
                .timestamp(doc.getTimestampOriginal() != null ? doc.getTimestampOriginal() : doc.getTimestampEs()) // 기본으로 timestamp 사용
                .sequence(doc.getSequence())
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
