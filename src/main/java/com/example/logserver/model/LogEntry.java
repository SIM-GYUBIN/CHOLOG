package com.example.logserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 로그 엔트리 모델 클래스
 * 로그 데이터를 저장하는 객체
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class LogEntry {
    // 기본 로그 정보
    private String level;
    private String message;
    private String timestamp;
    private String logger;
    private String thread;
    private String sequence;

    // 애플리케이션/서버 정보
    private String serviceName;
    private String environment;
    private String profiles; // Spring 활성 프로필 정보
    private String version;
    private String hostName;
    private String apiKey;

    // 요청 정보
    private String requestId;
    private String requestMethod;
    private String requestUri;
    private String clientIp;
    private String userAgent;
    private Integer httpStatus;

    // 브라우저 정보
    private Boolean uaMobile;
    private String uaPlatform;

    // 구조화된 객체들
    private Map<String, Object> performanceMetrics;
    private Map<String, Object> mdcContext;
    private Map<String, Object> headers;
    private Map<String, Object> throwable;

    // 요청/응답 정보
    private String requestParams;
    private Map<String, Object> responseHeaders;

    // 기타 정보
    private Boolean filtered;

    // 필드 최적화 메소드
    @JsonIgnore
    public void optimizeFields() {
        // 빈 맵/컬렉션 필드 제거
        if (mdcContext != null && mdcContext.isEmpty()) mdcContext = null;
        if (headers != null && headers.isEmpty()) headers = null;
        if (performanceMetrics != null && performanceMetrics.isEmpty()) performanceMetrics = null;
        if (throwable != null && throwable.isEmpty()) throwable = null;

        // 빈 문자열 필드 제거
        if (requestParams != null && requestParams.isEmpty()) requestParams = null;
        if (responseHeaders != null && responseHeaders.isEmpty()) responseHeaders = null;
        if (apiKey != null && apiKey.isEmpty()) apiKey = null;
    }
}