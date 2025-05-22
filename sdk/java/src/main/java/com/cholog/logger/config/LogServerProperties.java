package com.cholog.logger.config;

import ch.qos.logback.classic.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CHO:LOG Logging Library의 설정을 담당하는 Properties 클래스입니다.
 * 이 클래스는 Spring Boot의 자동 설정을 통해 애플리케이션의 로그 전송 설정을 관리합니다.
 * 
 * 주요 기능:
 * - 로그 서버 URL 및 API 키 설정
 * - 민감 정보 필터링 패턴 설정 (대소문자 구분 없이 필터링)
 * - 배치 전송 설정 (크기, 간격, 큐 용량)
 * - 디스크 큐 폴백 설정 (활성화, 경로, 재전송 간격, 최대 크기)
 * - 재시도 설정 (최대 시도 횟수, 지연 시간)
 * - 연결 상태 모니터링 설정 (체크 간격, 타임아웃)
 * - HTTPS 및 TLS 설정
 * - 로그 압축 설정 (v1.6.3 추가)
 * - 상태 지표 노출 설정 (v1.6.3 추가)
 * - 서비스 식별 및 환경 설정 (v1.6.7 추가)
 * - 연결 오류 로그 최소화 설정 (v1.7.5 추가)
 * - Logback 컨텍스트 통합 최적화 (v1.8.2 추가)
 * - CORS 설정 (v1.8.6 추가)
 * 
 * @author eddy1219
 * @version 1.8.6
 * @see com.cholog.logger.service.LogSenderService
 * @see com.cholog.logger.appender.CentralLogAppender
 */
@Component
@ConfigurationProperties(prefix = "cholog.logger")
@Validated
public class LogServerProperties {

    /**
     * 로그 서버의 URL입니다.
     * 설정하지 않으면 기본 CHO:LOG 서버로 전송됩니다.
     * 빈 문자열로 설정하면 로그 전송이 비활성화됩니다.
     * 
     * 예시: http://your-logging-server
     */
    private String url = "https://cholog-server.shop/api/logs/be";

    /**
     * 서버 식별을 위한 API 키입니다.
     * 이 키를 통해 로그 서버에서 서버별로 로그를 구분할 수 있습니다.
     *
     * 예시: server1-key
     */
    private String apiKey;

    /**
     * 서비스를 식별하는 논리적 이름입니다.
     * 중앙 로그 서버에서 로그를 필터링하거나 분석할 때 사용되는 주요 메타데이터입니다.
     * 예: "user-service", "order-processing-batch".
     * 로그 JSON에서는 'serviceName' 필드로 출력됩니다.
     * 설정하지 않으면 null이 사용됩니다.
     */
    private String serviceName = null;

    /**
     * API 키 검증 활성화 여부를 설정합니다.
     * 기본값은 {@code true}이며, 이 경우 {@link #apiKey}가 설정되어 있어야 합니다.
     * 개발 또는 테스트 환경에서 일시적으로 검증을 비활성화할 때 사용할 수 있습니다.
     * (주의: 프로덕션 환경에서는 항상 {@code true}로 설정하는 것을 권장합니다.)
     */
    private boolean validateApiKey = true;

    /**
     * 민감 정보로 간주하여 필터링할 필드 이름의 패턴 목록입니다.
     * 대소문자 구분 없이 매칭됩니다.
     * 
     * 예시:
     * - thread
     * - performanceMetrics.cpuUsage
     * - performanceMetrics.memoryUsage
     * - serverPort
     */
    private List<String> sensitivePatterns = new ArrayList<>();

    /**
     * 민감 정보를 대체할 문자열입니다.
     * 
     * 기본값: "***"
     */
    private String sensitiveValueReplacement = "***";

    /**
     * 전송할 최소 로그 레벨입니다.
     * 이 레벨 이상의 로그만 전송됩니다.
     * 
     * 기본값: INFO
     * 가능한 값: TRACE, DEBUG, INFO, WARN, ERROR
     */
    private Level logLevel = Level.INFO;

    // --- 로그 전송 관련 설정 ---

    /**
     * 로그 전송 실패 시 최대 재시도 횟수입니다.
     * 
     * 기본값: 3
     */
    private int maxRetries = 3;

    /**
     * 재시도 간격(밀리초)입니다.
     * 
     * 기본값: 1000 (1초)
     */
    private int retryDelay = 1000;

    /**
     * HTTPS 사용 여부입니다.
     * true로 설정하면 HTTP URL도 HTTPS로 자동 변환됩니다.
     * 
     * 기본값: false
     */
    private boolean useHttps = false;

    /**
     * 개발 환경에서 TLS 인증서 검증을 무시할지 여부입니다.
     * 프로덕션 환경에서는 반드시 false로 설정해야 합니다.
     * 
     * 기본값: false
     */
    private boolean allowInsecureTls = false;

    // --- 배치 처리 관련 설정 ---

    /**
     * 한 번에 전송할 로그의 최대 개수입니다.
     * 
     * 기본값: 100
     */
    private int batchSize = 100;

    /**
     * 로그를 모으는 최대 시간 간격(밀리초)입니다.
     * 이 시간이 지나면 배치 크기 미만이라도 전송을 시도합니다.
     * 
     * 기본값: 1000 (1초)
     */
    private int batchFlushInterval = 1000;

    /**
     * 메모리 큐의 최대 용량입니다.
     * 이 용량을 초과하면 로그가 유실될 수 있습니다.
     * 
     * 기본값: 10000
     */
    private int queueCapacity = 10000;

    // --- 디스크 큐(폴백) 관련 설정 ---

    /**
     * 전송 실패 시 디스크에 저장하는 기능의 활성화 여부입니다.
     * 
     * 기본값: true
     */
    private boolean diskQueueEnabled = true;

    /**
     * 실패한 로그를 저장할 디스크 경로입니다.
     * 
     * 기본값: ./log-queue
     */
    private String diskQueuePath = "./log-queue";

    /**
     * 디스크에 저장된 로그를 재전송 시도하는 간격(밀리초)입니다.
     * 
     * 기본값: 60000 (1분)
     */
    private int diskResendInterval = 60000;

    /**
     * 디스크 큐의 최대 크기 제한(MB)입니다.
     * 0 또는 음수로 설정하면 제한이 없습니다.
     * 
     * 기본값: 1024 (1GB)
     */
    private long maxDiskQueueSizeMb = 1024;

    // --- 연결 상태 모니터링 관련 설정 ---

    /**
     * 서버 연결 상태를 확인하는 간격(밀리초)입니다.
     * 
     * 기본값: 300000 (5분)
     */
    private long connectionCheckInterval = 300000;

    /**
     * 서버 연결 상태 확인 시 타임아웃(밀리초)입니다.
     * 
     * 기본값: 5000 (5초)
     */
    private long connectionCheckTimeout = 5000;

    // --- HttpClient Connection Pool Settings ---
    /**
     * Apache HttpClient 커넥션 풀의 전체 최대 커넥션 수입니다.
     * 기본값: 100
     */
    private int httpClientPoolMaxTotal = 100;

    /**
     * Apache HttpClient 커넥션 풀의 라우트(호스트)당 기본 최대 커넥션 수입니다.
     * 기본값: 20
     */
    private int httpClientPoolDefaultMaxPerRoute = 20;

    /**
     * Apache HttpClient 커넥션 풀에서 유휴 커넥션을 정리하는 주기(초)입니다.
     * 이 시간 동안 사용되지 않은 커넥션은 풀에서 제거될 수 있습니다.
     * 기본값: 30 (초)
     */
    private long httpClientPoolEvictIdleConnectionsAfter = 30L;

    // --- 상태 지표 관련 설정 (v1.6.3 추가) ---
    
    /**
     * 로깅 상태 지표 수집 활성화 여부입니다.
     * 활성화하면 로그 처리 성공률, 큐 상태 등의 지표를 수집합니다.
     * 
     * 기본값: true
     */
    private boolean metricsEnabled = true;
    
    /**
     * 상태 지표 수집 주기(밀리초)입니다.
     * 지표 수집 및 업데이트 간격을 설정합니다.
     * 
     * 기본값: 60000 (1분)
     */
    private long metricsCollectionInterval = 60000;
    
    /**
     * 애플리케이션 상태 정보와 로깅 지표를 JMX를 통해 노출할지 여부입니다.
     * Spring Boot Actuator를 사용하는 경우에도 사용 가능합니다.
     * 
     * 기본값: true
     */
    private boolean exposeMetricsViaJmx = true;

    /**
     * 로그 압축 활성화 여부입니다.
     * 활성화하면 로그를 gzip으로 압축하여 전송하여 대역폭을 절약합니다.
     * 
     * 기본값: false
     */
    private boolean compressLogs = false;

    /**
     * 연결 오류 로그 출력을 억제할지 여부를 설정합니다.
     * true로 설정하면 연결 오류가 발생할 때 모든 재시도마다 로그를 출력하지 않고,
     * 상태 변경(정상→오류, 오류→정상)이 있을 때만 로그를 출력합니다.
     * 
     * 기본값: true
     * 
     * @since 1.7.5
     */
    private boolean suppressConnectionErrors = true;

    /**
     * 지정된 기간 내에 출력할 최대 연결 오류 로그 수를 설정합니다.
     * suppressConnectionErrors가 true일 때 적용됩니다.
     * 
     * 기본값: 1
     * 
     * @since 1.7.5
     */
    private int maxConnectionErrorLogsPerPeriod = 1;

    /**
     * 연결 오류 로그 제한을 적용할 주기(밀리초)를 설정합니다.
     * suppressConnectionErrors가 true일 때 적용됩니다.
     * 
     * 기본값: 300000 (5분)
     * 
     * @since 1.7.5
     */
    private long connectionErrorLogPeriod = 300000;

    /**
     * 재시도 시 지수 백오프(Exponential Backoff) 전략을 사용할지 여부를 설정합니다.
     * true로 설정하면 연결 오류가 지속될 때 재시도 간격이 점점 길어집니다.
     * 
     * 기본값: true
     * 
     * @since 1.7.5
     */
    private boolean useExponentialBackoff = true;

    /**
     * 지수 백오프 전략 사용 시 초기 지연 시간(밀리초)을 설정합니다.
     * useExponentialBackoff가 true일 때 적용됩니다.
     * 
     * 기본값: 5000 (5초)
     * 
     * @since 1.7.5
     */
    private long initialBackoffDelay = 5000;

    /**
     * 지수 백오프 전략 사용 시 최대 지연 시간(밀리초)을 설정합니다.
     * useExponentialBackoff가 true일 때 적용됩니다.
     * 
     * 기본값: 1800000 (30분)
     * 
     * @since 1.7.5
     */
    private long maxBackoffDelay = 1800000;

    /**
     * 디스크에 저장된 로그 배치 관련 상세 로그를 출력할지 여부를 설정합니다.
     * false로 설정하면 디스크 저장 관련 INFO 레벨 로그가 출력되지 않습니다.
     * 
     * 기본값: false
     * 
     * @since 1.7.5
     */
    private boolean verboseDiskQueueLogs = false;

    /**
     * CORS 설정을 활성화할지 여부를 지정합니다.
     * 활성화할 경우 기본 CORS 필터가 등록됩니다.
     */
    private boolean corsEnabled = false;

    /**
     * 로깅 라이브러리의 활성화 여부를 지정합니다.
     * 기본적으로 활성화됩니다.
     */
    private boolean enabled = true;

    // --- Getters and Setters ---

    /**
     * 설정된 로그 서버 URL을 반환합니다.
     * @return 로그 서버 URL 문자열
     */
    public String getUrl() {
        return url;
    }

    /**
     * 로그 서버 URL을 설정합니다.
     * @param url 로그 서버 URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 설정된 API 키를 반환합니다.
     * @return API 키 문자열
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * API 키를 설정합니다.
     * @param apiKey API 키
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 민감 정보 필터링 패턴 목록을 반환합니다.
     * @return 민감 정보 필터링 패턴 리스트
     */
    public List<String> getSensitivePatterns() {
        return sensitivePatterns;
    }

    /**
     * 민감 정보 필터링 패턴 목록을 설정합니다.
     * @param sensitivePatterns 민감 정보 필터링 패턴 리스트
     */
    public void setSensitivePatterns(List<String> sensitivePatterns) {
        this.sensitivePatterns = sensitivePatterns;
    }

    /**
     * 민감 정보 대체 문자열을 반환합니다.
     * @return 민감 정보 대체 문자열
     */
    public String getSensitiveValueReplacement() {
        return sensitiveValueReplacement;
    }

    /**
     * 민감 정보 대체 문자열을 설정합니다.
     * @param sensitiveValueReplacement 민감 정보 대체 문자열
     */
    public void setSensitiveValueReplacement(String sensitiveValueReplacement) {
        this.sensitiveValueReplacement = sensitiveValueReplacement;
    }

    /**
     * 전송할 최소 로그 레벨을 반환합니다.
     * @return Logback의 {@link Level} 객체
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * 전송할 최소 로그 레벨을 설정합니다.
     * @param logLevel Logback의 {@link Level} 객체
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * 로그 전송 실패 시 최대 재시도 횟수를 반환합니다.
     * @return 최대 재시도 횟수
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 로그 전송 실패 시 최대 재시도 횟수를 설정합니다.
     * @param maxRetries 최대 재시도 횟수
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 재시도 간격(밀리초)을 반환합니다.
     * @return 재시도 간격(밀리초)
     */
    public int getRetryDelay() {
        return retryDelay;
    }

    /**
     * 재시도 간격(밀리초)을 설정합니다.
     * @param retryDelay 재시도 간격(밀리초)
     */
    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * HTTPS 사용 여부를 반환합니다.
     * @return HTTPS 사용 시 {@code true}, 아니면 {@code false}
     */
    public boolean isUseHttps() {
        return useHttps;
    }

    /**
     * HTTPS 사용 여부를 설정합니다.
     * @param useHttps HTTPS 사용 시 {@code true}, 아니면 {@code false}
     */
    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    /**
     * 안전하지 않은 TLS 연결 허용 여부를 반환합니다.
     * (프로덕션 환경에서는 사용하지 마십시오.)
     * @return 허용 시 {@code true}, 아니면 {@code false}
     */
    public boolean isAllowInsecureTls() {
        return allowInsecureTls;
    }

    /**
     * 안전하지 않은 TLS 연결 허용 여부를 설정합니다.
     * @param allowInsecureTls 허용 시 {@code true}, 아니면 {@code false}
     */
    public void setAllowInsecureTls(boolean allowInsecureTls) {
        this.allowInsecureTls = allowInsecureTls;
    }

    /**
     * 한 번에 전송할 로그의 최대 개수(배치 크기)를 반환합니다.
     * @return 배치 크기
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 한 번에 전송할 로그의 최대 개수(배치 크기)를 설정합니다.
     * @param batchSize 배치 크기
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 로그를 모으는 최대 시간 간격(배치 플러시 간격, 밀리초)을 반환합니다.
     * @return 배치 플러시 간격(밀리초)
     */
    public int getBatchFlushInterval() {
        return batchFlushInterval;
    }

    /**
     * 로그를 모으는 최대 시간 간격(배치 플러시 간격, 밀리초)을 설정합니다.
     * @param batchFlushInterval 배치 플러시 간격(밀리초)
     */
    public void setBatchFlushInterval(int batchFlushInterval) {
        this.batchFlushInterval = batchFlushInterval;
    }

    /**
     * 메모리 큐의 최대 용량을 반환합니다.
     * @return 메모리 큐 용량
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * 메모리 큐의 최대 용량을 설정합니다.
     * @param queueCapacity 메모리 큐 용량
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * 디스크 큐(폴백) 활성화 여부를 반환합니다.
     * @return 활성화 시 {@code true}, 아니면 {@code false}
     */
    public boolean isDiskQueueEnabled() {
        return diskQueueEnabled;
    }

    /**
     * 디스크 큐(폴백) 활성화 여부를 설정합니다.
     * @param diskQueueEnabled 활성화 시 {@code true}, 아니면 {@code false}
     */
    public void setDiskQueueEnabled(boolean diskQueueEnabled) {
        this.diskQueueEnabled = diskQueueEnabled;
    }

    /**
     * 실패한 로그를 저장할 디스크 경로를 반환합니다.
     * @return 디스크 큐 경로 문자열
     */
    public String getDiskQueuePath() {
        return diskQueuePath;
    }

    /**
     * 실패한 로그를 저장할 디스크 경로를 설정합니다.
     * @param diskQueuePath 디스크 큐 경로 문자열
     */
    public void setDiskQueuePath(String diskQueuePath) {
        this.diskQueuePath = diskQueuePath;
    }

    /**
     * 디스크에 저장된 로그를 재전송 시도하는 간격(밀리초)을 반환합니다.
     * @return 디스크 큐 재전송 간격(밀리초)
     */
    public int getDiskResendInterval() {
        return diskResendInterval;
    }

    /**
     * 디스크에 저장된 로그를 재전송 시도하는 간격(밀리초)을 설정합니다.
     * @param diskResendInterval 디스크 큐 재전송 간격(밀리초)
     */
    public void setDiskResendInterval(int diskResendInterval) {
        this.diskResendInterval = diskResendInterval;
    }

    /**
     * 디스크 큐의 최대 크기 제한(MB)을 반환합니다.
     * @return 디스크 큐 최대 크기(MB)
     */
    public long getMaxDiskQueueSizeMb() {
        return maxDiskQueueSizeMb;
    }

    /**
     * 디스크 큐의 최대 크기 제한(MB)을 설정합니다.
     * @param maxDiskQueueSizeMb 디스크 큐 최대 크기(MB)
     */
    public void setMaxDiskQueueSizeMb(long maxDiskQueueSizeMb) {
        this.maxDiskQueueSizeMb = maxDiskQueueSizeMb;
    }

    /**
     * 서버 연결 상태를 확인하는 간격(밀리초)을 반환합니다.
     * @return 연결 확인 간격(밀리초)
     */
    public long getConnectionCheckInterval() {
        return connectionCheckInterval;
    }

    /**
     * 서버 연결 상태를 확인하는 간격(밀리초)을 설정합니다.
     * @param connectionCheckInterval 연결 확인 간격(밀리초)
     */
    public void setConnectionCheckInterval(long connectionCheckInterval) {
        this.connectionCheckInterval = connectionCheckInterval;
    }

    /**
     * 서버 연결 상태 확인 시 타임아웃(밀리초)을 반환합니다.
     * @return 연결 확인 타임아웃(밀리초)
     */
    public long getConnectionCheckTimeout() {
        return connectionCheckTimeout;
    }

    /**
     * 서버 연결 상태 확인 시 타임아웃(밀리초)을 설정합니다.
     * @param connectionCheckTimeout 연결 확인 타임아웃(밀리초)
     */
    public void setConnectionCheckTimeout(long connectionCheckTimeout) {
        this.connectionCheckTimeout = connectionCheckTimeout;
    }

    /**
     * 설정된 디스크 큐 경로를 {@link java.nio.file.Path} 객체로 반환합니다.
     * @return 디스크 큐 경로의 Path 객체
     */
    public java.nio.file.Path getDiskQueuePathObject() {
        return Paths.get(this.diskQueuePath);
    }

    /**
     * 설정된 디스크 큐 최대 크기 제한(MB)을 바이트 단위로 변환하여 반환합니다.
     * 0 또는 음수이면 제한 없음을 나타내는 {@code Long.MAX_VALUE}를 반환합니다.
     * @return 디스크 큐 최대 크기(바이트)
     */
    public long getMaxDiskQueueSizeBytes() {
        if (this.maxDiskQueueSizeMb <= 0) {
            return Long.MAX_VALUE; // 제한 없음
        }
        return this.maxDiskQueueSizeMb * 1024 * 1024;
    }

    /**
     * HttpClient 커넥션 풀의 전체 최대 커넥션 수를 반환합니다.
     * @return 전체 최대 커넥션 수
     */
    public int getHttpClientPoolMaxTotal() {
        return httpClientPoolMaxTotal;
    }

    /**
     * HttpClient 커넥션 풀의 전체 최대 커넥션 수를 설정합니다.
     * @param httpClientPoolMaxTotal 전체 최대 커넥션 수
     */
    public void setHttpClientPoolMaxTotal(int httpClientPoolMaxTotal) {
        this.httpClientPoolMaxTotal = httpClientPoolMaxTotal;
    }

    /**
     * HttpClient 커넥션 풀의 라우트당 기본 최대 커넥션 수를 반환합니다.
     * @return 라우트당 기본 최대 커넥션 수
     */
    public int getHttpClientPoolDefaultMaxPerRoute() {
        return httpClientPoolDefaultMaxPerRoute;
    }

    /**
     * HttpClient 커넥션 풀의 라우트당 기본 최대 커넥션 수를 설정합니다.
     * @param httpClientPoolDefaultMaxPerRoute 라우트당 기본 최대 커넥션 수
     */
    public void setHttpClientPoolDefaultMaxPerRoute(int httpClientPoolDefaultMaxPerRoute) {
        this.httpClientPoolDefaultMaxPerRoute = httpClientPoolDefaultMaxPerRoute;
    }

    /**
     * HttpClient 커넥션 풀의 유휴 커넥션 정리 주기(초)를 반환합니다.
     * @return 유휴 커넥션 정리 주기(초)
     */
    public long getHttpClientPoolEvictIdleConnectionsAfter() {
        return httpClientPoolEvictIdleConnectionsAfter;
    }

    /**
     * HttpClient 커넥션 풀의 유휴 커넥션 정리 주기(초)를 설정합니다.
     * @param httpClientPoolEvictIdleConnectionsAfter 유휴 커넥션 정리 주기(초)
     */
    public void setHttpClientPoolEvictIdleConnectionsAfter(long httpClientPoolEvictIdleConnectionsAfter) {
        this.httpClientPoolEvictIdleConnectionsAfter = httpClientPoolEvictIdleConnectionsAfter;
    }

    /**
     * 로깅 상태 지표 수집 활성화 여부를 반환합니다.
     * @return 지표 수집 활성화 시 {@code true}, 아니면 {@code false}
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /**
     * 로깅 상태 지표 수집 활성화 여부를 설정합니다.
     * @param metricsEnabled 지표 수집 활성화 시 {@code true}, 아니면 {@code false}
     */
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    /**
     * 상태 지표 수집 주기(밀리초)를 반환합니다.
     * @return 지표 수집 주기(밀리초)
     */
    public long getMetricsCollectionInterval() {
        return metricsCollectionInterval;
    }

    /**
     * 상태 지표 수집 주기(밀리초)를 설정합니다.
     * @param metricsCollectionInterval 지표 수집 주기(밀리초)
     */
    public void setMetricsCollectionInterval(long metricsCollectionInterval) {
        this.metricsCollectionInterval = metricsCollectionInterval;
    }

    /**
     * 상태 지표를 JMX를 통해 노출할지 여부를 반환합니다.
     * @return JMX 노출 활성화 시 {@code true}, 아니면 {@code false}
     */
    public boolean isExposeMetricsViaJmx() {
        return exposeMetricsViaJmx;
    }

    /**
     * 상태 지표를 JMX를 통해 노출할지 여부를 설정합니다.
     * @param exposeMetricsViaJmx JMX 노출 활성화 시 {@code true}, 아니면 {@code false}
     */
    public void setExposeMetricsViaJmx(boolean exposeMetricsViaJmx) {
        this.exposeMetricsViaJmx = exposeMetricsViaJmx;
    }

    /**
     * 서비스 이름을 반환합니다.
     * 이 값은 로그 JSON의 'serviceName' 필드로 출력됩니다.
     * 
     * @return 서비스 이름
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 서비스 이름을 설정합니다.
     * 
     * @param serviceName 서비스 이름
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * API 키 검증 활성화 여부를 반환합니다.
     * @return API 키 검증이 활성화된 경우 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean isValidateApiKey() {
        return validateApiKey;
    }

    /**
     * API 키 검증 활성화 여부를 설정합니다.
     * @param validateApiKey 검증 활성화 여부
     */
    public void setValidateApiKey(boolean validateApiKey) {
        this.validateApiKey = validateApiKey;
    }

    /**
     * 로그 압축 활성화 여부를 반환합니다.
     * @return 압축 활성화 시 {@code true}, 아니면 {@code false}
     */
    public boolean isCompressLogs() {
        return compressLogs;
    }

    /**
     * 로그 압축 활성화 여부를 설정합니다.
     * @param compressLogs 압축 활성화 시 {@code true}, 아니면 {@code false}
     */
    public void setCompressLogs(boolean compressLogs) {
        this.compressLogs = compressLogs;
    }

    /**
     * 연결 오류 로그 출력을 억제할지 여부를 반환합니다.
     * @return 연결 오류 로그 출력 억제 여부
     */
    public boolean isSuppressConnectionErrors() {
        return suppressConnectionErrors;
    }

    /**
     * 연결 오류 로그 출력을 억제할지 여부를 설정합니다.
     * @param suppressConnectionErrors 연결 오류 로그 출력 억제 여부
     */
    public void setSuppressConnectionErrors(boolean suppressConnectionErrors) {
        this.suppressConnectionErrors = suppressConnectionErrors;
    }

    /**
     * 지정된 기간 내에 출력할 최대 연결 오류 로그 수를 반환합니다.
     * @return 최대 연결 오류 로그 수
     */
    public int getMaxConnectionErrorLogsPerPeriod() {
        return maxConnectionErrorLogsPerPeriod;
    }

    /**
     * 지정된 기간 내에 출력할 최대 연결 오류 로그 수를 설정합니다.
     * @param maxConnectionErrorLogsPerPeriod 최대 연결 오류 로그 수
     */
    public void setMaxConnectionErrorLogsPerPeriod(int maxConnectionErrorLogsPerPeriod) {
        this.maxConnectionErrorLogsPerPeriod = maxConnectionErrorLogsPerPeriod;
    }

    /**
     * 연결 오류 로그 제한을 적용할 주기(밀리초)를 반환합니다.
     * @return 연결 오류 로그 제한 주기(밀리초)
     */
    public long getConnectionErrorLogPeriod() {
        return connectionErrorLogPeriod;
    }

    /**
     * 연결 오류 로그 제한을 적용할 주기(밀리초)를 설정합니다.
     * @param connectionErrorLogPeriod 연결 오류 로그 제한 주기(밀리초)
     */
    public void setConnectionErrorLogPeriod(long connectionErrorLogPeriod) {
        this.connectionErrorLogPeriod = connectionErrorLogPeriod;
    }

    /**
     * 재시도 시 지수 백오프(Exponential Backoff) 전략을 사용할지 여부를 반환합니다.
     * @return 지수 백오프 전략 사용 여부
     */
    public boolean isUseExponentialBackoff() {
        return useExponentialBackoff;
    }

    /**
     * 재시도 시 지수 백오프(Exponential Backoff) 전략을 사용할지 여부를 설정합니다.
     * @param useExponentialBackoff 지수 백오프 전략 사용 여부
     */
    public void setUseExponentialBackoff(boolean useExponentialBackoff) {
        this.useExponentialBackoff = useExponentialBackoff;
    }

    /**
     * 지수 백오프 전략 사용 시 초기 지연 시간(밀리초)을 반환합니다.
     * @return 초기 지연 시간(밀리초)
     */
    public long getInitialBackoffDelay() {
        return initialBackoffDelay;
    }

    /**
     * 지수 백오프 전략 사용 시 초기 지연 시간(밀리초)을 설정합니다.
     * @param initialBackoffDelay 초기 지연 시간(밀리초)
     */
    public void setInitialBackoffDelay(long initialBackoffDelay) {
        this.initialBackoffDelay = initialBackoffDelay;
    }

    /**
     * 지수 백오프 전략 사용 시 최대 지연 시간(밀리초)을 반환합니다.
     * @return 최대 지연 시간(밀리초)
     */
    public long getMaxBackoffDelay() {
        return maxBackoffDelay;
    }

    /**
     * 지수 백오프 전략 사용 시 최대 지연 시간(밀리초)을 설정합니다.
     * @param maxBackoffDelay 최대 지연 시간(밀리초)
     */
    public void setMaxBackoffDelay(long maxBackoffDelay) {
        this.maxBackoffDelay = maxBackoffDelay;
    }

    /**
     * 디스크에 저장된 로그 배치 관련 상세 로그를 출력할지 여부를 반환합니다.
     * @return 디스크 로그 배치 상세 로그 출력 여부
     */
    public boolean isVerboseDiskQueueLogs() {
        return verboseDiskQueueLogs;
    }

    /**
     * 디스크에 저장된 로그 배치 관련 상세 로그를 출력할지 여부를 설정합니다.
     * @param verboseDiskQueueLogs 디스크 로그 배치 상세 로그 출력 여부
     */
    public void setVerboseDiskQueueLogs(boolean verboseDiskQueueLogs) {
        this.verboseDiskQueueLogs = verboseDiskQueueLogs;
    }

    /**
     * CORS 설정을 활성화할지 여부를 반환합니다.
     *
     * @return CORS 설정 활성화 여부
     */
    public boolean isCorsEnabled() {
        return corsEnabled;
    }

    /**
     * CORS 설정을 활성화할지 여부를 설정합니다.
     *
     * @param corsEnabled CORS 설정 활성화 여부
     */
    public void setCorsEnabled(boolean corsEnabled) {
        this.corsEnabled = corsEnabled;
    }

    /**
     * 로깅 라이브러리의 활성화 여부를 반환합니다.
     *
     * @return 활성화 여부
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 로깅 라이브러리의 활성화 여부를 설정합니다.
     *
     * @param enabled 활성화 여부
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}