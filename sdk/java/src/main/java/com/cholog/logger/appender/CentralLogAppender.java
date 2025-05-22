package com.cholog.logger.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.cholog.logger.config.LogServerProperties;
import com.cholog.logger.service.LogSenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CHO:LOG Logging Library의 핵심 클래스로, Logback 프레임워크의 Appender 역할을 합니다.
 * 이 클래스는 애플리케이션에서 생성된 모든 로그 이벤트를 가로채서 JSON 형식으로 변환하고,
 * LogSenderService를 통해 중앙 로그 서버로 비동기적으로 전송합니다.
 * <p>
 * 주요 기능:
 * - 로그 이벤트를 JSON 형태로 변환
 * - MDC(Mapped Diagnostic Context)에서 중요 컨텍스트 정보 추출
 * - 시스템 성능 지표(CPU, 메모리, 스레드 수 등) 수집
 * - HTTP 요청 정보(URI, 메소드, 헤더 등)와 연계
 * - 중앙 서버로의 비동기 전송을 위한 큐 관리
 *
 * @author eddy1219
 * @version 1.0.4
 * @see com.cholog.logger.service.LogSenderService
 * @see ch.qos.logback.core.AppenderBase
 */
@Component
public class CentralLogAppender extends AppenderBase<ILoggingEvent> {

    // --- MDC 키 상수 정의 ---
    /**
     * MDC 키: HTTP 요청 처리 시간 (ms). {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String RESPONSE_TIME_MDC_KEY = "responseTime";
    /**
     * MDC 키: 고유 요청 ID (UUID). {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    /**
     * MDC 키: HTTP 요청 메소드 (GET, POST 등). {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";
    /**
     * MDC 키: HTTP 요청 URI. {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String REQUEST_URI_MDC_KEY = "requestUri";
    /**
     * MDC 키: 클라이언트 IP 주소. {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String REQUEST_CLIENT_IP_MDC_KEY = "requestClientIp";
    /**
     * MDC 키: 사용자 에이전트 (User-Agent 헤더). {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String REQUEST_USER_AGENT_MDC_KEY = "requestUserAgent";
    /**
     * MDC 키: HTTP STATUS. {@link com.cholog.logger.filter.RequestTimingFilter} 에서 설정.
     */
    public static final String HTTP_STATUS_MDC_KEY = "httpStatus";
    // ------------------------

    // 스레드별 requestId 캐시를 저장하는 ConcurrentHashMap
    private final ConcurrentHashMap<String, String> threadRequestIdMap = new ConcurrentHashMap<>();

    private final LogSenderService logSenderService;
    private final LogServerProperties properties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    // 서버 정보 필드
    private String hostName;
    private String ipAddress;
    private String serverPort;

    // 시스템 메트릭 수집을 위한 MXBean 필드
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;

    // Spring Boot 서버 포트 (Spring Environment에서 주입)
    @Value("${server.port:8080}") // Spring Boot 표준 포트 설정 사용
    private String port;

    // 시퀀스 번호 관리를 위한 카운터
    private long sequenceCounter;

    /**
     * 생성자-기반 의존성 주입.
     * Spring Boot가 자동으로 필요한 빈들을 주입합니다.
     *
     * @param logSenderService 로그 전송 서비스
     * @param properties       로그 서버 설정
     * @param environment      Spring 환경 (profiles, properties)
     */
    @Autowired
    public CentralLogAppender(LogSenderService logSenderService, LogServerProperties properties,
                              Environment environment) {
        this.logSenderService = Objects.requireNonNull(logSenderService, "LogSenderService must not be null");
        this.properties = Objects.requireNonNull(properties, "LogServerProperties must not be null");
        this.environment = Objects.requireNonNull(environment, "Environment must not be null");
        this.objectMapper = new ObjectMapper();

        // JMX MXBean 초기화
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();

        // 서버 정보 초기화
        initializeServerInfo();
    }

    /**
     * Logback에 의해 컨텍스트가 설정된 후 호출됩니다.
     * 이제 안전하게 Logback 관련 메서드를 호출할 수 있습니다.
     */
    @Override
    public void setContext(ch.qos.logback.core.Context context) {
        super.setContext(context);
        addInfo("CentralLogAppender constructed with LogSenderService and context initialized.");
    }

    /**
     * Appender의 이름을 설정합니다.
     * 이 메서드는 Logback에 의해 호출됩니다.
     */
    @Override
    public void setName(String name) {
        super.setName(name);
        addInfo("CentralLogAppender name set to: " + name);
    }

    /**
     * Appender를 시작합니다.
     * Context가 null인 경우 경고 메시지를 출력하고 시작하지 않습니다.
     */
    @Override
    public void start() {
        if (getContext() == null) {
            System.err.println("CHOLOG: Context is null, cannot start CentralLogAppender");
            return;
        }
        
        if (getName() == null) {
            System.err.println("CHOLOG: Name is null, setting default name for CentralLogAppender");
            setName("CHOLOG_CENTRAL_APPENDER");
        }
        
        super.start();
        addInfo("CentralLogAppender started successfully");
    }

    /**
     * Appender 초기화 시 서버의 호스트 이름, IP 주소, 포트 번호를 조회하여 내부 필드에 저장합니다.
     * 포트 번호는 Spring Environment를 통해 주입된 값을 사용합니다.
     * 정보 조회 실패 시 기본값(null)을 사용하고 에러 로그를 남깁니다.
     */
    private void initializeServerInfo() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            this.hostName = localHost.getHostName();
            this.ipAddress = localHost.getHostAddress();
            this.serverPort = this.port; // @Value로 주입된 포트 사용
        } catch (UnknownHostException e) {
            this.hostName = null;
            this.ipAddress = null;
            this.serverPort = this.port != null ? this.port : null;
            // context가 설정된 후에만 오류 로그를 남기도록 하지 않음
        } catch (Exception e) {
            this.hostName = null;
            this.ipAddress = null;
            this.serverPort = null;
        }
    }

    /**
     * 현재 시스템의 성능 관련 메트릭(CPU 사용률, Heap 메모리 사용량, 스레드 수)을 수집합니다.
     * MXBean 접근 시 발생할 수 있는 예외를 처리하고 경고 로그를 남깁니다.
     *
     * @param event 로그 이벤트 객체 (현재 메소드 내부에서는 직접 사용되지 않음)
     * @return 수집된 시스템 메트릭 정보를 담은 Map 객체. 실패 시 빈 Map 또는 에러값 포함 가능.
     */
    private Map<String, Object> collectSystemMetrics(ILoggingEvent event) {
        Map<String, Object> metrics = new HashMap<>();
        // CPU 사용량
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double processCpuLoad = sunOsBean.getProcessCpuLoad();
                metrics.put("cpuUsage", processCpuLoad >= 0 ? Math.round(processCpuLoad * 100) : 0L); // Long 타입 통일
            } else {
                metrics.put("cpuUsage", -1L); // 사용 불가
            }
        } catch (Exception e) {
            metrics.put("cpuUsage", -2L); // 오류
            addWarn("Could not get CPU usage", e);
        }
        // Heap 메모리 사용량
        try {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            metrics.put("memoryUsage", usedMemory / (1024 * 1024)); // MB 단위
        } catch (Exception e) {
            metrics.put("memoryUsage", -1L); // 오류
            addWarn("Could not get Memory usage", e);
        }
        // 스레드 정보
        try {
            metrics.put("activeThreads", threadBean.getThreadCount());
            metrics.put("totalThreads", (int) threadBean.getTotalStartedThreadCount());
        } catch (Exception e) {
            metrics.put("activeThreads", -1); // 오류
            metrics.put("totalThreads", -1);
            addWarn("Could not get Thread usage", e);
        }
        return metrics;
    }

    /**
     * Logback에 의해 호출되는 핵심 메소드. 로그 이벤트를 받아 처리합니다.
     * 설정된 로그 레벨({@link LogServerProperties#getLogLevel()}) 이상의 이벤트만 처리하며,
     * 각종 정보를 수집/보강하여 JSON으로 변환 후 {@link LogSenderService#addToQueue(String)}로 전송 요청합니다.
     * <p>
     * v1.0.3부터는 {@link com.cholog.logger.filter.RequestTimingFilter}에 의해 MDC에 저장된 `requestId`가 있다면,
     * 이를 추출하여 모든 로그(Tomcat/Servlet 컨테이너 자체 에러 로그 포함)의 `requestId` 필드에 주입합니다.
     *
     * @param event Logback으로부터 전달받은 로그 이벤트 객체
     */
    @Override
    protected void append(ILoggingEvent event) {
        // 1. Appender 시작 상태 확인 (Logback 표준)
        if (!isStarted()) {
            return;
        }

        // 2. 로그 레벨 필터링
        if (!event.getLevel().isGreaterOrEqual(properties.getLogLevel())) {
            return;
        }

        try {
            // 3. 최종 로그 데이터 담을 Map 생성
            Map<String, Object> logData = new HashMap<>();

            // 4. 기본 로그 정보 추가
            logData.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
            logData.put("level", event.getLevel().toString());
            String eventLoggerName = event.getLoggerName();
            logData.put("logger", eventLoggerName);
            logData.put("message", event.getFormattedMessage()); // 포맷팅된 메시지 사용
            logData.put("thread", event.getThreadName());

            // source 추가 (backend)
            logData.put("source", "backend");

            // 시퀀스 번호 추가 (설정이 활성화된 경우)
            logData.put("sequence", 1L);

            // 5. 애플리케이션 정보 추가
            String serviceName = properties.getServiceName();
            if (serviceName != null && !serviceName.isEmpty()) {
                logData.put("serviceName", serviceName);
            } else {
                // 서비스 이름이 설정되지 않았을 경우 기본값이나 경고 메시지
                logData.put("serviceName", "unknown-service");
                addWarn("서비스 이름(service-name)이 설정되지 않았습니다. 'cholog.logger.service-name' 속성을 설정하세요.");
            }
//            logData.put("environment", properties.getEnvironment());
            
            // 버전 정보 추가
            String version = environment.getProperty("app.version");
            if (version != null) {
                logData.put("version", version);
            }
            
            // Spring 활성 프로필 정보 추가
            String[] activeProfiles = environment.getActiveProfiles();
            if (activeProfiles.length > 0) {
                logData.put("environment", String.join(",", activeProfiles));
            } else {
                // 기본 프로필이 사용 중인 경우
                String[] defaultProfiles = environment.getDefaultProfiles();
                if (defaultProfiles.length > 0) {
                    logData.put("environment", String.join(",", defaultProfiles));
                } else {
                    logData.put("environment", "default");
                }
            }

            // 6. 서버 정보 추가
            logData.put("hostName", this.hostName);
            logData.put("ipAddress", this.ipAddress);
            logData.put("serverPort", this.serverPort);

            // API 키를 apiKey 필드에 추가
            String apiKey = properties.getApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                logData.put("apiKey", apiKey);
            }

            // 7. 시스템 성능 메트릭 수집 (CPU, Memory, Threads) - 요청 시간은 나중에 처리
            Map<String, Object> systemMetrics = collectSystemMetrics(event);

            // 8. MDC(Mapped Diagnostic Context) 정보 처리
            Map<String, String> mdcProperties = event.getMDCPropertyMap();
            if (mdcProperties != null && !mdcProperties.isEmpty()) {
                Map<String, Object> mdcForOutput = new HashMap<>();
                Map<String, String> headers = new HashMap<>();

                // 8a. MDC에서 핵심 정보 추출하여 루트 필드로 저장 (중복 방지)
                String requestIdFromMDC = mdcProperties.get(REQUEST_ID_MDC_KEY);
                String requestMethod = mdcProperties.get(REQUEST_METHOD_MDC_KEY);
                String requestUri = mdcProperties.get(REQUEST_URI_MDC_KEY);
                String clientIp = mdcProperties.get(REQUEST_CLIENT_IP_MDC_KEY);
                String userAgent = mdcProperties.get(REQUEST_USER_AGENT_MDC_KEY);
                String statusStr = mdcProperties.get(HTTP_STATUS_MDC_KEY);

                // 현재 스레드 이름
                String threadName = event.getThreadName();
                
                // 스레드 기반 requestId 추적 처리
                if (requestIdFromMDC != null) {
                    // MDC에 requestId가 있으면 스레드별 맵에 저장
                    threadRequestIdMap.put(threadName, requestIdFromMDC);
                } else {
                    // MDC에 requestId가 없으면 스레드별 맵에서 조회
                    requestIdFromMDC = threadRequestIdMap.get(threadName);
                }

                // 추출한 핵심 정보를 추가
                if (requestIdFromMDC != null) {
                    logData.put("requestId", requestIdFromMDC);
                }
                
                // HTTP 관련 정보를 http 객체로 묶어서 저장
                Map<String, Object> httpData = new HashMap<>();
                
                // HTTP 정보는 http 객체에만 추가 (루트에는 추가하지 않음)
                if (requestMethod != null) httpData.put("requestMethod", requestMethod);
                if (requestUri != null) httpData.put("requestUri", requestUri);
                if (clientIp != null) logData.put("clientIp", clientIp); // clientIp는 루트에 유지

                // HTTP 상태 코드 처리 (숫자로 변환)
                if (statusStr != null) {
                    try {
                        httpData.put("httpStatus", Integer.parseInt(statusStr));
                    } catch (NumberFormatException e) {
                        addWarn("Could not parse httpStatus from MDC: " + statusStr);
                    }
                }

                // 요청 시간 추출하여 http 객체에 추가 (performanceMetrics에는 추가하지 않음)
                String responseTimeStr = mdcProperties.get(RESPONSE_TIME_MDC_KEY);
                if (responseTimeStr != null) {
                    try {
                        long responseTime = Long.parseLong(responseTimeStr);
                        httpData.put("responseTime", responseTime); // http 객체에만 responseTime 추가
                    } catch (NumberFormatException e) {
                        addWarn("Could not parse responseTime from MDC: " + responseTimeStr);
                    }
                }

                // HTTP 데이터가 비어있지 않으면 로그에 추가
                if (!httpData.isEmpty()) {
                    logData.put("http", httpData);
                }

                // UserAgent 필드 처리 - 루트 레벨에만 추가
                if (userAgent != null) {
                    logData.put("userAgent", userAgent);
                }

                // 8b. request_headers 처리 - 헤더 객체로 통합, 중복 방지
                String headerJsonStr = mdcProperties.get("request_headers");
                if (headerJsonStr != null && !headerJsonStr.isEmpty() && !headerJsonStr.equals("{}")) {
                    try {
                        // JSON 문자열에서 헤더 맵으로 변환
                        headers = objectMapper.readValue(headerJsonStr,
                                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

                        // 중요 헤더 정보 추출 (sec-ch-ua-mobile, sec-ch-ua-platform 등)
                        String uaMobile = headers.get("sec-ch-ua-mobile");
                        if (uaMobile != null) {
                            logData.put("uaMobile", !uaMobile.replace("?", "").equals("0"));
                        }

                        String uaPlatform = headers.get("sec-ch-ua-platform");
                        if (uaPlatform != null) {
                            // "Windows" 같은 형식으로 따옴표 제거
                            logData.put("uaPlatform", uaPlatform.replace("\"", ""));
                        }

                        // UserAgent 필드가 아직 없으면 헤더에서 가져옴
                        if (userAgent == null) {
                            userAgent = headers.get("user-agent");
                            if (userAgent != null) {
                                logData.put("userAgent", userAgent);
                            }
                        } else {
                            logData.put("userAgent", userAgent);
                        }
                    } catch (Exception e) {
                        addWarn("Failed to parse request_headers JSON: " + headerJsonStr, e);
                    }
                }

                // 요청 매개변수 포함 (있는 경우)
                String requestParams = mdcProperties.get("request_params");
                if (requestParams != null && !requestParams.isEmpty() && !requestParams.equals("{}")) {
                    try {
                        // JSON 문자열을 객체로 변환하여 저장
                        Map<String, Object> paramsMap = objectMapper.readValue(requestParams,
                                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                        logData.put("requestParams", paramsMap);
                    } catch (Exception e) {
                        // 파싱 실패 시 원래 문자열 그대로 사용
                        addWarn("Failed to parse request_params JSON: " + requestParams, e);
                        logData.put("requestParams", requestParams);
                    }
                }

                // 8c. 응답 헤더 처리 (v1.6.7부터 객체 형태로 처리)
                String responseHeadersJson = mdcProperties.get("response_headers");
                if (responseHeadersJson != null && !responseHeadersJson.isEmpty() && !responseHeadersJson.equals("{}")) {
                    try {
                        // JSON 문자열에서 응답 헤더 객체로 변환
                        Map<String, String> responseHeaders = objectMapper.readValue(responseHeadersJson,
                                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
                        // 객체 그대로 로그 데이터에 추가 (문자열로 직렬화하지 않음)
                        logData.put("responseHeaders", responseHeaders);
                    } catch (Exception e) {
                        addWarn("Failed to parse response headers: " + e.getMessage());
                        // 파싱 실패 시, 원본 문자열을 그대로 유지
                        logData.put("responseHeaders", responseHeadersJson);
                    }
                }

                // 8c. 중요한 MDC 값만 mdcContext 객체에 저장 (중복 최소화)
                mdcProperties.forEach((key, value) -> {
                    // 이미 루트 레벨로 추출한 값, request_header_ 접두사가 있는 값은 제외
                    if (!key.equals(REQUEST_ID_MDC_KEY) &&
                            !key.equals(REQUEST_METHOD_MDC_KEY) &&
                            !key.equals(REQUEST_URI_MDC_KEY) &&
                            !key.equals(REQUEST_CLIENT_IP_MDC_KEY) &&
                            !key.equals(REQUEST_USER_AGENT_MDC_KEY) &&
                            !key.equals(HTTP_STATUS_MDC_KEY) &&
                            !key.equals(RESPONSE_TIME_MDC_KEY) &&
                            !key.startsWith("request_header_") &&
                            !key.equals("request_headers") &&
                            !key.equals("request_params") &&
                            !key.equals("response_headers") &&
                            // 추가 필터링: 이미 루트 레벨로 추출된 값의 단순 변형도 제외
                            !key.equals("http_status")) { // HTTP 상태가 "httpStatus"로 이미 루트 레벨에 있음
                        mdcForOutput.put(key, value);
                    }
                });

                // MDC 컨텍스트가 비어있지 않은 경우만 추가
                if (!mdcForOutput.isEmpty()) {
                    logData.put("mdcContext", mdcForOutput);
                }

                // 헤더 정보가 비어있지 않은 경우만 추가
                if (!headers.isEmpty()) {
                    logData.put("headers", headers);
                }
            }

            // 9. 성능 메트릭 추가 (responseTime 제외 - http 객체로 이동됨)
            if (!systemMetrics.isEmpty()) {
                logData.put("performanceMetrics", systemMetrics);
            }
            
            // 추가: Tomcat 컨테이너 및 Spring 예외 로그에 HTTP 상태 코드 설정
            if (event.getLevel().toString().equals("ERROR") && eventLoggerName != null) {
                boolean needsStatusCode = false;
                String message = event.getFormattedMessage();
                String threadName = event.getThreadName();
                
                // 1. Tomcat 컨테이너 예외 감지
                if ((eventLoggerName.contains("org.apache.catalina") || eventLoggerName.contains("org.apache.tomcat")) &&
                    message != null &&
                    (message.contains("threw exception") ||
                     message.contains("Servlet.service") ||
                     message.contains("Exception processing"))) {
                    needsStatusCode = true;
                    
                    // Tomcat 로그에도 requestId 적용 (맵에 있는 경우)
                    String requestIdFromMap = threadRequestIdMap.get(threadName);
                    if (requestIdFromMap != null && !logData.containsKey("requestId")) {
                        logData.put("requestId", requestIdFromMap);
                    }
                }
                
                // 2. Spring 프레임워크 예외 감지
                else if ((eventLoggerName.contains("org.springframework") ||
                         eventLoggerName.contains("DispatcherServlet") ||
                         eventLoggerName.contains("HandlerAdapter")) &&
                        message != null &&
                        (message.contains("Exception") ||
                         message.contains("Error") ||
                         message.contains("Failed"))) {
                    needsStatusCode = true;
                    
                    // Spring 로그에도 requestId 적용 (맵에 있는 경우)
                    String requestIdFromMap = threadRequestIdMap.get(threadName);
                    if (requestIdFromMap != null && !logData.containsKey("requestId")) {
                        logData.put("requestId", requestIdFromMap);
                    }
                }
                
                // 3. 일반적인 예외 메시지 패턴 감지
                else if (message != null &&
                        (message.contains("Exception:") ||
                         message.contains("Error:") ||
                         message.contains("Throwable:"))) {
                    needsStatusCode = true;
                    
                    // 일반 에러 로그에도 requestId 적용 (맵에 있는 경우)
                    String requestIdFromMap = threadRequestIdMap.get(threadName);
                    if (requestIdFromMap != null && !logData.containsKey("requestId")) {
                        logData.put("requestId", requestIdFromMap);
                    }
                }
                
                // 상태 코드가 필요하고 아직 설정되지 않은 경우 500으로 설정
                if (needsStatusCode) {
                    // http 객체가 없으면 생성
                    @SuppressWarnings("unchecked")
                    Map<String, Object> httpData = (Map<String, Object>) logData.get("http");
                    if (httpData == null) {
                        httpData = new HashMap<>();
                        logData.put("http", httpData);
                    }
                    
                    // http 객체에 상태 코드 추가
                    if (!httpData.containsKey("httpStatus")) {
                        httpData.put("httpStatus", 500);
                    }
                }
            }

            // 10. 예외(Exception) 정보 추가
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                Map<String, Object> throwableData = new HashMap<>();
                throwableData.put("type", throwableProxy.getClassName());
                throwableData.put("message", throwableProxy.getMessage());
                
                // 예외가 있는 경우 httpStatus가 설정되어 있지 않으면 500으로 설정
                @SuppressWarnings("unchecked")
                Map<String, Object> httpData = (Map<String, Object>) logData.get("http");
                if (httpData == null) {
                    httpData = new HashMap<>();
                    logData.put("http", httpData);
                }
                
                if (!httpData.containsKey("httpStatus")) {
                    httpData.put("httpStatus", 500);
                }

                // 예외 발생 시에도 requestId 체크 (스레드 맵에 있는 경우)
                if (!logData.containsKey("requestId")) {
                    String requestIdFromMap = threadRequestIdMap.get(event.getThreadName());
                    if (requestIdFromMap != null) {
                        logData.put("requestId", requestIdFromMap);
                    }
                }

                // 스택 트레이스 (문자열 배열로 변환)
                StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
                if (stackTraceElements != null) { // null 체크 추가
                    String[] stackTraceStrings = new String[stackTraceElements.length];
                    for (int i = 0; i < stackTraceElements.length; i++) {
                        stackTraceStrings[i] = stackTraceElements[i].toString();
                    }
                    throwableData.put("stacktrace", stackTraceStrings);
                }

                // 원인 예외(Cause) 정보 (간략히)
                IThrowableProxy cause = throwableProxy.getCause();
                if (cause != null) {
                    Map<String, Object> causeData = new HashMap<>();
                    causeData.put("type", cause.getClassName());
                    causeData.put("message", cause.getMessage());
                    throwableData.put("cause", causeData);
                }
                logData.put("error", throwableData);
            }
            
            // 10.5 중복 필드 제거: HTTP 관련 필드가 루트 레벨에 있고 http 객체도 있다면 루트 레벨 필드를 제거
            if (logData.containsKey("http")) {
                // 루트 레벨에서 http 객체로 이동된 필드 제거
                logData.remove("httpStatus");
                logData.remove("requestMethod");
                logData.remove("requestUri");
                logData.remove("responseTime");
            }

            // 11. 최종 Map을 JSON 문자열로 변환
            String jsonLog = objectMapper.writeValueAsString(logData);

            // 12. LogSenderService의 큐에 추가하여 비동기 전송 요청
            logSenderService.addToQueue(jsonLog);

        } catch (Exception e) {
            // Appender 내부에서 심각한 오류 발생 시 Logback 상태 시스템에 에러 기록
            addError("Failed to process log event in CentralLogAppender for logger " + event.getLoggerName(), e);
        }
    }

    // 맵 정리를 위한 메서드 (필요 시 주기적으로 호출할 수 있음)
    private void cleanupRequestIdMap() {
        if (threadRequestIdMap.size() > 1000) { // 맵 크기가 너무 크면 정리
            threadRequestIdMap.clear();
        }
    }

    @Override
    public void stop() {
        addInfo("Stopping CentralLogAppender.");
        threadRequestIdMap.clear(); // 맵 정리
        super.stop();
    }
}