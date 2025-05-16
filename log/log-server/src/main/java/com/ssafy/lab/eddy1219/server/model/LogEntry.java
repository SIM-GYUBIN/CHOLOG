package com.ssafy.lab.eddy1219.server.model;

import lombok.Data;
import java.util.Map;

/**
 * 중앙 로그 서버에서 수신하는 로그 데이터의 구조를 정의하는 DTO 클래스입니다.
 * Lombok의 @Data 어노테이션을 사용하여 Getter, Setter, toString 등을 자동 생성합니다.
 * 최신 SDK(v1.0.6) 출력 형식에 맞게 업데이트되었습니다.
 */
@Data
public class LogEntry {
    // 기본 로그 정보
    private String timestamp;
    private String level;
    private String logger;
    private String message;
    private String thread;
    private Long sequence;     // 로그 시퀀스 번호
    private String source = "backend"; // 기본값을 "backend"로 명시
    private String logType; // 서버에서 직접 넣어줄 거야 ! (ex: "general", "error")

    // 애플리케이션 정보
    private String serviceName;  // 이전 applicationName
    private String environment;  // Spring 활성 프로필 정보
    private String version;
    private String apiKey;      // API 인증키 (projectKey로 활용 가능)
    private String projectKey;  // << 명시적으로 projectKey 필드 사용 권장

    // 서버 정보
    private String hostName;
    private String ipAddress;
    private String serverPort;

    // 요청 정보 (HTTP 요청인 경우)
    private String requestId;    // 추적 ID
    private String clientIp;     // 이전 requestClientIp
    private String userAgent;    // 이전 requestUserAgent
    private Boolean uaMobile;    // 모바일 기기 여부
    private String uaPlatform;   // 플랫폼 정보 (Windows, MacOS 등)

    // HTTP 관련 정보 그룹화
    private Map<String, Object> http;  // requestMethod, requestUri, httpStatus, responseTime 포함

    // 헤더 정보
    private Map<String, String> headers;
    private Map<String, String> responseHeaders;

    // MDC 정보
    private Map<String, Object> mdcContext;  // 이전 mdc

    // 예외 정보
    private Map<String, Object> error;  // 이전 throwable

    // 성능 메트릭
    private PerformanceMetrics performanceMetrics;

    /**
     * 성능 메트릭 정보를 담는 내부 클래스입니다.
     * responseTime은 http 객체로 이동되었습니다.
     */
    @Data
    public static class PerformanceMetrics {
        private Long memoryUsage;    // MB 단위
        private Long cpuUsage;       // % 단위
        private Integer activeThreads;
        private Integer totalThreads;
    }
}