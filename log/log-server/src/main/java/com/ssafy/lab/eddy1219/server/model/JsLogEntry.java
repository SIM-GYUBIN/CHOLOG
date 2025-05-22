package com.ssafy.lab.eddy1219.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class JsLogEntry {
    private String timestamp;
    private Long sequence;
    private String level; // 예: "INFO", "ERROR", "DEBUG", "TRACE", "WARN"
    private String message;
    private String source; // 예: "frontend"
    private String projectKey;
    private String environment;
    private String requestId;
    private String logger; // 예: "cholog", "console"
    private String logType;    // 예: "general", "error", "network", "event"

    private Map<String, Object> payload;

    @JsonProperty("error")
    private LogErrorInfo errorInfo;

    @JsonProperty("http")
    private LogHttpInfo httpInfo;

    @JsonProperty("client")
    private LogClientInfo clientInfo;

    @JsonProperty("event")
    private LogEventInfo eventInfo;

    @Data
    public static class LogErrorInfo {
        private String type;
        private String message;
        private String stacktrace;
    }

    @Data
    public static class LogHttpInfo {
//        private LogHttpRequestInfo request;
//        private LogHttpResponseInfo response;
        private String method;
        private String requestUri;
        private Integer status;
        private Long responseTime; // SDK는 number 타입이므로 Long으로 매핑
    }

//    @Data
//    public static class LogHttpRequestInfo {
//        private String method;
//        private String url;
//    }

//    @Data
//    public static class LogHttpResponseInfo {
//        private Integer statusCode; // SDK는 number 타입이므로 Integer로 매핑
//    }

    @Data
    public static class LogClientInfo {
        private String url;
        private String userAgent;
        private String referrer;
    }

    @Data
    public static class LogEventInfo {
        private String type;
        private String targetSelector;
        private Map<String, Object> properties;
    }
}
