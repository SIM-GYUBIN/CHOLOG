package com.ssafy.cholog.domain.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

@Data
@Document(indexName = "pjt-*") // 인덱스 패턴 지정, 실제 사용 시 동적으로 정확한 인덱스명 지정
@JsonIgnoreProperties(ignoreUnknown = true) // ES에는 있지만 Document에 정의되지 않은 필드는 무시
public class LogDocument {

    @Id
    @Field(name = "_id") // Elasticsearch의 내부 ID 필드
    private String id;

    // ES의 @timestamp 필드는 ISO 8601 형식의 문자열 또는 epoch_millis. Instant로 매핑.
    // 로그 예시에서는 배열로 되어 있으나, 일반적으로 단일 값으로 저장됨.
    // 만약 배열로 저장된다면 List<String>으로 받고, 서비스 로직에서 첫번째 값을 사용하거나 전처리 필요.
    // 여기서는 단일 Instant 값으로 가정.
    @Field(name = "@timestamp", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'") // Elasticsearch 필드 매핑도 일관성을 위해 변경 권장
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", timezone = "UTC")
    private Instant timestampEs; // ES 표준 타임스탬프

    @Field(name = "timestamp", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // Elasticsearch 필드 매핑도 일관성을 위해 변경 권장
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestampOriginal;

    @Field(name = "sequence", type = FieldType.Long)
    private Long sequence;

    @Field(name = "level", type = FieldType.Keyword)
    private String level;

    @Field(name = "message", type = FieldType.Text)
    private String message; // 필요시 message.keyword도 고려

    @Field(name = "source", type = FieldType.Keyword)
    private String source;

    @Field(name = "projectKey", type = FieldType.Keyword)
    private String projectKey;

    @Field(name = "environment", type = FieldType.Keyword)
    private String environment;

    @Field(name = "traceId", type = FieldType.Keyword)
    private String traceId;

    @Field(name = "logger", type = FieldType.Keyword)
    private String logger;

    @Field(name = "logType", type = FieldType.Keyword)
    private String logType;

    // 중첩 객체들 (JsLogEntry 구조 참고)
    @Field(name = "client", type = FieldType.Object)
    private ClientInfo client;

    @Field(name = "http", type = FieldType.Object)
    private HttpInfo http;

    @Field(name = "error", type = FieldType.Object)
    private ErrorInfo error;

    @Field(name = "event", type = FieldType.Object)
    private EventInfo event;

    // 기타 정의되지 않은 필드를 위한 공간 (예: JsLogEntry의 payload)
    // Flattened 타입은 내부 필드를 자동으로 검색 가능하게 하지만, 필드 수가 많으면 성능 이슈 있을 수 있음
    @Field(type = FieldType.Flattened)
    private Map<String, Object> payload;

    // --- 중첩 객체 정의 ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientInfo {
        // 로그 예시에서는 client.url이 배열. 단일 값으로 저장되었다고 가정. 아니면 List<String> url;
        @Field(name = "url", type = FieldType.Text)
        private String url;
        @Field(name = "userAgent", type = FieldType.Text)
        private String userAgent;
        @Field(name = "referrer", type = FieldType.Text)
        private String referrer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpInfo {
        @Field(name = "request", type = FieldType.Object)
        private HttpRequestInfo request;
        @Field(name = "response", type = FieldType.Object)
        private HttpResponseInfo response;
        @Field(name = "durationMs", type = FieldType.Long)
        private Long durationMs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpRequestInfo {
        @Field(name = "method", type = FieldType.Keyword)
        private String method;
        @Field(name = "url", type = FieldType.Text)
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpResponseInfo {
        @Field(name = "statusCode", type = FieldType.Integer)
        private Integer statusCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorInfo {
        @Field(name = "type", type = FieldType.Keyword)
        private String type;
        @Field(name = "message", type = FieldType.Text)
        private String message;
        @Field(name = "stacktrace", type = FieldType.Text)
        private String stacktrace;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventInfo {
        @Field(name = "type", type = FieldType.Keyword)
        private String type;
        @Field(name = "targetSelector", type = FieldType.Text)
        private String targetSelector;
        @Field(name = "properties", type = FieldType.Object)
        private Map<String, Object> properties;
    }
}