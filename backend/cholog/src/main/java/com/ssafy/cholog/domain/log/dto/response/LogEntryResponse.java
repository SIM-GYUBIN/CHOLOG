package com.ssafy.cholog.domain.log.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.cholog.domain.log.dto.item.HttpItem;
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
    private Instant timestamp; // 주요 타임스탬프
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
    private HttpItem http;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.ErrorInfo error;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LogDocument.EventInfo event;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> payload;

    public static LogEntryResponse fromLogDocument(LogDocument doc) {
        HttpItem httpItem = null;
        LogDocument.HttpInfo httpInfo = doc.getHttp();
        if (httpInfo != null) { // httpInfo 객체가 null이 아닐 때만 HttpItem 생성
            HttpItem.HttpRequestInfo.HttpRequestInfoBuilder requestBuilder = HttpItem.HttpRequestInfo.builder();
            if (httpInfo.getMethod() != null) {
                requestBuilder.method(httpInfo.getMethod());
            }
            if (httpInfo.getRequestUri() != null) {
                requestBuilder.url(httpInfo.getRequestUri());
            }

            HttpItem.HttpResponseInfo.HttpResponseInfoBuilder responseBuilder = HttpItem.HttpResponseInfo.builder();
            if (httpInfo.getStatusCode() != null) {
                responseBuilder.statusCode(httpInfo.getStatusCode());
            }

            httpItem = HttpItem.builder()
                    .request(requestBuilder.build())
                    .response(responseBuilder.build())
                    .durationMs(httpInfo.getResponseTime()) // HttpInfo의 getResponseTime()이 null을 반환할 수 있다면, HttpItem의 durationMs도 null이 됨
                    .build();
        }

        return LogEntryResponse.builder()
                .id(doc.getId())
//                .timestamp(doc.getTimestampEs() != null ? doc.getTimestampEs() : doc.getTimestampOriginal()) // 기본으로 @timestamp 사용
//                .timestamp(doc.getTimestampOriginal() != null ? doc.getTimestampOriginal() : doc.getTimestampEs()) // 기본으로 timestamp 사용
                .timestamp(doc.getTimestampOriginal())
//                .sequence(doc.getSequence())
                .level(doc.getLevel())
                .message(doc.getMessage())
                .source(doc.getSource())
                .projectKey(doc.getProjectKey())
                .environment(doc.getEnvironment())
                .traceId(doc.getRequestId())
                .logger(doc.getLogger())
                .logType(doc.getLogType())
                .client(doc.getClient())
                .http(httpItem)
                .error(doc.getError())
                .event(doc.getEvent())
                .payload(doc.getPayload())
                .build();
    }
}
