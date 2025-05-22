package com.cholog.logger.service;

import com.cholog.logger.appender.CentralLogAppender;
import com.cholog.logger.config.LogServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * 중앙 로그 서버로 로그 메시지를 전송하는 서비스 클래스입니다.
 * Logback Appender({@link com.cholog.logger.appender.CentralLogAppender})로부터 로그 메시지를 받아 내부 큐에 저장하고,
 * 별도의 스케줄링 스레드를 사용하여 로그를 비동기(Asynchronous) 및 배치(Batching) 방식으로 중앙 서버에 전송합니다.
 *
 * 주요 기능:
 *     로그 메시지의 비동기 배치 전송
 *     HTTP/HTTPS 프로토콜 지원 및 API 키를 사용한 인증
 *     민감 정보 필터링 ({@link LogServerProperties#getSensitivePatterns()})
 *     로그 전송 실패 시 설정된 횟수만큼 재시도 ({@link LogServerProperties#getMaxRetries()})
 *     재시도 후에도 전송에 최종 실패할 경우, 로그 배치를 로컬 디스크에 기록 (Disk Queue Fallback, {@link LogServerProperties#isDiskQueueEnabled()})
 *     주기적으로 디스크에 저장된 로그 배치를 읽어 중앙 서버로 재전송 시도
 *     애플리케이션 종료 시 사용된 리소스(스레드 풀, HTTP 클라이언트)의 정상적인 정리 ({@link DisposableBean})
 *     중앙 서버 연결 상태 주기적 확인 및 자동 복구 시도
 *     과도한 연결 오류 로그 방지를 위한 로그 억제 기능 (v1.7.5)
 *     재시도 지수 백오프 전략으로 효율적인 재연결 시도 (v1.7.5)
 * 모든 동작은 {@link LogServerProperties}에 정의된 속성 값을 기반으로 설정됩니다.
 *
 * @author eddy1219
 * @version 1.8.6
 * @see com.cholog.logger.appender.CentralLogAppender
 * @see com.cholog.logger.config.LogServerProperties
 * @see org.springframework.beans.factory.DisposableBean
 */
@Service
public class LogSenderService implements DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(LogSenderService.class);

    private final LogServerProperties properties;
    private final CloseableHttpClient httpClient; // Apache HttpClient 4.x 사용
    private final ObjectMapper objectMapper;
    private final Pattern[] sensitivePatterns; // 민감한 값 필터링을 위한 패턴 배열

    /** 로그 메시지(JSON 문자열)를 임시 저장하는 스레드 안전 메모리 큐 (Bounded Queue) */
    private final BlockingQueue<String> logQueue;
    /** 배치 전송 및 디스크 큐 재전송 작업을 주기적으로 실행하는 스케줄러 */
    private final ScheduledExecutorService scheduler;
    /** 서비스의 활성 상태를 나타내는 플래그 (애플리케이션 종료 시 false로 설정됨) */
    private final AtomicBoolean active = new AtomicBoolean(true);
    /** 실패한 로그 배치를 저장할 디스크 디렉토리 경로 (null일 수 있음) */
    private final Path diskQueueDir;
    /** 디스크 큐 기능의 실제 활성화 여부 (경로 검증 후 결정됨) */
    private final boolean effectiveDiskQueueEnabled;
    /** 디스크 큐에 저장되는 파일의 확장자 */
    private static final String DISK_QUEUE_FILE_SUFFIX = ".logbatch";
    // 최대 재시도 실패 횟수 - 이 횟수를 초과하면 retried 폴더로 이동
    private static final int MAX_BATCH_RETRY_ATTEMPTS = 5;
    // 추가 폴더 이름 상수 정의
    private static final String RETRIED_FOLDER_NAME = "retried";

    // 연결 상태 체크 및 재전송 관련 필드 추가
    private final AtomicBoolean isServerAvailable = new AtomicBoolean(true);
    private final AtomicLong lastConnectionCheckTime = new AtomicLong(0);
    
    // v1.7.5: 연결 오류 로그 최적화 관련 필드
    private final AtomicLong lastErrorLogTime = new AtomicLong(0);
    private final AtomicInteger errorLogsInPeriod = new AtomicInteger(0);
    private long currentBackoffDelay;
    private final Random random = new Random();

    // JMX 메트릭 관리 객체
    private LogSenderMetrics metrics;
    
    // HTTP 요청 시 사용할 타임아웃 값 (밀리초)
    private static final int CONNECT_TIMEOUT = 5000; // 연결 타임아웃 (5초)
    private static final int SOCKET_TIMEOUT = 10000; // 데이터 수신 타임아웃 (10초)

    // 디스크 큐 작업에 대한 동기화 객체 추가
    private final Object diskQueueLock = new Object();

    /**
     * 생성자-기반 의존성 주입.
     * Spring Boot가 자동으로 필요한 빈들을 주입합니다.
     *
     * @param properties 로그 서버 설정
     */
    @Autowired
    public LogSenderService(LogServerProperties properties) {
        this.properties = Objects.requireNonNull(properties, "LogServerProperties cannot be null");
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient(); // HttpClient 초기화
        
        // v1.7.5: 지수 백오프 초기 지연값 설정
        this.currentBackoffDelay = properties.getInitialBackoffDelay();

        // 민감 정보 필터링 패턴 컴파일 (대소문자 구분 없이)
        this.sensitivePatterns = properties.getSensitivePatterns().stream()
                .map(pattern -> Pattern.compile(pattern.toLowerCase(), Pattern.CASE_INSENSITIVE))
                .toArray(Pattern[]::new);

        // 메모리 큐 초기화 (설정된 용량 사용)
        this.logQueue = new LinkedBlockingQueue<>(properties.getQueueCapacity());
        // 스케줄러 스레드 풀 초기화 (2개 스레드)
        this.scheduler = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("log-sender-scheduler-" + thread.getId());
            thread.setDaemon(true); // 데몬 스레드로 설정
            return thread;
        });

        // --- 디스크 큐 경로 설정 및 검증 ---
        Path determinedPath = null;
        boolean canUseDiskQueue = properties.isDiskQueueEnabled(); // 설정값 확인

        if (canUseDiskQueue) {
            logger.info("Disk queue feature is enabled via configuration. Initializing path...");
            try {
                determinedPath = properties.getDiskQueuePathObject();
                // 기본 경로 사용 시 경고
                if ("./log-queue".equals(properties.getDiskQueuePath())) {
                    logger.warn("Using default disk queue path './log-queue'. " +
                            "It is strongly recommended to set 'cholog.logger.disk-queue-path' explicitly " +
                            "to a persistent and writable location for production environments.");
                }
                // 디렉토리 생성 시도 (이미 존재하면 문제 없음)
                Files.createDirectories(determinedPath);
                // 쓰기 권한 확인
                if (!Files.isWritable(determinedPath)) {
                    logger.error("Configured disk queue path '{}' is not writable. Disk queuing disabled.",
                            determinedPath.toAbsolutePath());
                    canUseDiskQueue = false; // 쓰기 불가
                } else {
                    logger.info("Disk queue directory initialized and verified at: {}", determinedPath.toAbsolutePath());
                    // canUseDiskQueue 는 true 유지
                }
            } catch (InvalidPathException e) {
                logger.error("Invalid disk queue path configured: '{}'. Disk queuing disabled.", properties.getDiskQueuePath(), e);
                canUseDiskQueue = false; // 경로 오류
            } catch (IOException e) {
                logger.error("Failed to create or access disk queue directory: '{}'. Disk queuing disabled. Error: {}",
                        properties.getDiskQueuePath(), e.getMessage());
                canUseDiskQueue = false; // IO 오류
            } catch (Exception e) {
                logger.error("Unexpected error during disk queue path initialization for '{}'. Disk queuing disabled.",
                        properties.getDiskQueuePath(), e);
                canUseDiskQueue = false; // 기타 오류
            }
        } else {
            logger.info("Disk queue feature is disabled by configuration ('cholog.logger.disk-queue-enabled=false').");
            // canUseDiskQueue 는 false 유지
        }

        this.diskQueueDir = canUseDiskQueue ? determinedPath : null;
        this.effectiveDiskQueueEnabled = canUseDiskQueue; // 최종 활성화 여부 저장

        // --- 백그라운드 작업 스케줄링 ---
        // 1. 주기적인 메모리 큐 -> 서버 배치 전송 작업
        scheduler.scheduleWithFixedDelay(
                this::processBatchFromQueue,
                properties.getBatchFlushInterval(), // 초기 지연
                properties.getBatchFlushInterval(), // 반복 간격
                TimeUnit.MILLISECONDS
        );

        // 2. 디스크 큐가 활성화된 경우, 주기적인 디스크 큐 -> 서버 전송 작업
        if (effectiveDiskQueueEnabled) {
            scheduler.scheduleWithFixedDelay(
                    this::resendFromDisk,
                    properties.getDiskResendInterval(), // 초기 지연
                    properties.getDiskResendInterval(), // 반복 간격
                    TimeUnit.MILLISECONDS
            );
        }

        // 3. 주기적인 서버 연결 상태 확인 작업
        scheduler.scheduleWithFixedDelay(
            this::checkServerConnection,
                properties.getConnectionCheckInterval(), // 초기 지연
                properties.getConnectionCheckInterval(),
            TimeUnit.MILLISECONDS
        );

        // 4. JMX 메트릭 게시 (활성화된 경우)
        if (properties.isExposeMetricsViaJmx()) {
            try {
                registerJmxMetrics();
            } catch (Exception e) {
                logger.warn("Failed to register JMX metrics. Performance monitoring via JMX will not be available.", e);
            }
        }

        logger.info(String.format("LogSenderService initialized with: " +
                        "url=%s, batchSize=%d, batchFlushInterval=%dms, queueCapacity=%d, maxRetries=%d, " +
                        "diskQueue=%b, diskQueuePath=%s",
                properties.getUrl(), properties.getBatchSize(), properties.getBatchFlushInterval(),
                properties.getQueueCapacity(), properties.getMaxRetries(),
                effectiveDiskQueueEnabled, effectiveDiskQueueEnabled ? diskQueueDir.toAbsolutePath() : "N/A"));
    }

    /**
     * {@link CentralLogAppender}로부터 호출되어 개별 로그 메시지(JSON 문자열)를 내부 메모리 큐({@link #logQueue})에 추가합니다.
     * 로그 메시지는 추가 전에 민감 정보 필터링을 거칩니다 ({@link #filterSensitiveValues(String)}).
     * 서비스가 비활성 상태이거나(애플리케이션 종료 중), 로그 메시지가 null 또는 비어있거나, 로그 서버 URL이 설정되지 않은 경우 로그는 추가되지 않습니다.
     * 메모리 큐가 가득 찬 경우, 디스크 큐가 활성화되어 있으면 디스크에 저장하고, 그렇지 않으면 로그가 유실됩니다.
     *
     * @param jsonLog 전송할 개별 로그 이벤트의 JSON 문자열. null이거나 비어있으면 무시됩니다.
     */
    public void addToQueue(String jsonLog) {
        if (!active.get()) {
            logger.debug("로그 전송이 비활성화되어 있습니다 (서비스 종료 중).");
            return; // 종료 중이면 추가 안 함
        }
        if (jsonLog == null || jsonLog.isEmpty()) {
            logger.debug("빈 로그 메시지가 전달되었습니다.");
            return; // 빈 로그는 추가 안 함
        }

        // URL이 비어있으면 로그를 보내지 않음
        if (properties.getUrl() == null || properties.getUrl().trim().isEmpty()) {
            logger.warn("로그 서버 URL이 설정되지 않았습니다. 로그 메시지가 전송되지 않습니다.");
            return;
        }

        // 민감한 값 필터링
        String filteredLog = filterSensitiveValues(jsonLog);

        boolean added = logQueue.offer(filteredLog); // Non-blocking 추가 시도
        if (!added) {
            if (effectiveDiskQueueEnabled && diskQueueDir != null) {
                try {
                    // 큐가 가득 찬 경우 직접 디스크에 저장
                    logger.warn("로그 큐가 가득 찼습니다 (용량: {}). 로그를 디스크에 직접 저장합니다.", properties.getQueueCapacity());
                    saveBatchToDisk("[" + filteredLog + "]");
                    
                    // 메트릭 업데이트
                    if (metrics != null) {
                        metrics.incrementFailedLogs(1);
                    }
                } catch (Exception e) {
                    logger.error("큐 가득 참 시 로그 디스크 저장 실패: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("로그 큐가 가득 찼고 디스크 큐가 비활성화되어 있어 로그 메시지가 유실됩니다. (큐 용량: {})", 
                    properties.getQueueCapacity());
            }
        } else {
            if (logger.isTraceEnabled()) { // Trace 레벨 로그 활성화 시 큐 추가 로깅
                logger.trace("로그가 큐에 추가되었습니다. 현재 큐 크기: {}", logQueue.size());
            } else if (logger.isDebugEnabled()) {
                logger.debug("로그가 큐에 추가되었습니다. 큐 크기: {}", logQueue.size());
            }
        }
    }

    /**
     * 로그 메시지에서 민감한 값을 필터링합니다.
     * @param jsonLog 원본 로그 메시지
     * @return 필터링된 로그 메시지
     */
    private String filterSensitiveValues(String jsonLog) {
        if (sensitivePatterns == null || sensitivePatterns.length == 0) {
            return jsonLog;
        }

        try {
            Map<String, Object> logMap = objectMapper.readValue(jsonLog, new TypeReference<Map<String, Object>>() {});
            // 필터링을 위한 컨텍스트 객체 생성 (필터링 상태 추적)
            FilterContext context = new FilterContext(sensitivePatterns, properties.getSensitiveValueReplacement());
            
            // 실제 필터링 수행
            boolean isFiltered = filterSensitiveValuesRecursive(logMap, "", context);
            
            if (isFiltered) {
                logMap.put("filtered", true);
                // 보안을 위해 filteredFields는 제거
                logMap.remove("filteredFields");
            }
            
            return objectMapper.writeValueAsString(logMap);
        } catch (Exception e) {
            logger.warn("민감 정보 필터링 실패: {}", e.getMessage());
            return jsonLog;
        }
    }

    /**
     * 필터링 상태를 추적하기 위한 컨텍스트 클래스
     */
    private static class FilterContext {
        private final Pattern[] patterns;
        private final String replacement;
        
        public FilterContext(Pattern[] patterns, String replacement) {
            this.patterns = patterns;
            this.replacement = replacement;
        }
        
        public Pattern[] getPatterns() {
            return patterns;
        }
        
        public String getReplacement() {
            return replacement;
        }
    }

    private boolean filterSensitiveValuesRecursive(Map<String, Object> map, String parentPath, FilterContext context) {
        boolean isFiltered = false;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            // 키가 민감한 패턴과 일치하는지 확인 (대소문자 구분 없이)
            boolean isSensitive = isSensitivePath(currentPath, context.getPatterns());

            if (isSensitive && value != null) {
                // 민감한 값은 대체 문자열로 변경
                entry.setValue(context.getReplacement());
                isFiltered = true;
            } else if (value instanceof Map) {
                // Map인 경우 재귀적으로 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                if (filterSensitiveValuesRecursive(nestedMap, currentPath, context)) {
                    isFiltered = true;
                }
            } else if (value instanceof List) {
                // List인 경우 각 요소를 재귀적으로 처리
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                if (processListItems(list, currentPath, context)) {
                    isFiltered = true;
                }
            }
        }
        
        return isFiltered;
    }
    
    /**
     * 리스트 항목을 처리하여 민감 정보 필터링
     */
    private boolean processListItems(List<Object> list, String parentPath, FilterContext context) {
        boolean isFiltered = false;
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String itemPath = parentPath + "[" + i + "]";
            
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) item;
                if (filterSensitiveValuesRecursive(nestedMap, itemPath, context)) {
                    isFiltered = true;
                }
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) item;
                if (processListItems(nestedList, itemPath, context)) {
                    isFiltered = true;
                }
            }
        }
        
        return isFiltered;
    }
    
    /**
     * 경로가 민감한지 확인
     */
    private boolean isSensitivePath(String path, Pattern[] patterns) {
        if (path == null || patterns == null) {
            return false;
        }
        
        String lowerCasePath = path.toLowerCase();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(lowerCasePath).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 스케줄러에 의해 주기적으로 호출되어, 메모리 큐({@link #logQueue})에서 로그들을 가져와 배치를 구성하고 전송을 시도합니다.
     * 먼저 서버 연결 상태({@link #isServerAvailable})를 확인하고, 연결이 불안정하면 {@link #checkServerConnection()}을 호출하여 상태 갱신을 시도합니다.
     * 큐에서 설정된 배치 크기({@link LogServerProperties#getBatchSize()})만큼 로그를 꺼내어 리스트를 만듭니다.
     * 배치가 비어있지 않으면 {@link #sendBatchWithRetries(List)}를 호출하여 실제 전송 로직을 수행합니다.
     * 전송에 최종 실패하면 (재시도 포함), {@link #saveBatchToDisk(String)}를 통해 디스크 큐에 배치를 저장합니다.
     */
    private void processBatchFromQueue() {
        // 서버 가용성 확인 (원자적으로 현재 값 읽기)
        boolean serverAvailable = isServerAvailable.get();
        if (!serverAvailable) {
            // 서버가 가용하지 않을 때만 연결 확인 수행 (불필요한 체크 방지)
            logger.debug("서버 연결 불가 상태입니다. 연결 상태를 확인합니다.");
            checkServerConnection();
            return;
        }

        List<String> batch = new ArrayList<>();
        logQueue.drainTo(batch, properties.getBatchSize());

        if (batch.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("처리할 로그 배치가 없습니다. 로그 큐가 비어 있습니다.");
            }
            return;
        }

        logger.debug("로그 배치 처리를 시작합니다. 배치 크기: {}", batch.size());
        if (!sendBatchWithRetries(batch)) {
            // 전송 실패 시 디스크에 저장 (배치 내용 보존)
            try {
                String jsonArray = "[" + String.join(",", batch) + "]";
                if (effectiveDiskQueueEnabled) {
                    logger.info("모든 재시도 후 로그 전송 실패. 디스크 큐에 {} 개의 로그를 저장합니다.", batch.size());
                saveBatchToDisk(jsonArray);
                } else {
                    logger.warn("모든 재시도 후 로그 전송 실패. 디스크 큐가 비활성화되어 {} 개의 로그가 유실됩니다.", batch.size());
                }
            } catch (Exception e) {
                logger.error("로그 직렬화 또는 저장 실패: {}", e.getMessage());
            }
        } else {
            logger.debug("로그 배치 전송 성공. 전송된 로그 개수: {}", batch.size());
        }
    }

    /**
     * 주어진 로그 배치(List&lt;String&gt;, 각 문자열은 개별 JSON 로그)를 중앙 서버로 전송 시도합니다.
     * 개별 로그 문자열들을 JSON 배열 형식으로 결합한 후 (예: {@code "[log1,log2,...]"}),
     * {@link #executeSend(String)}를 호출하여 실제 HTTP POST 요청을 실행합니다.
     * 전송 실패 시, 설정된 최대 재시도 횟수({@link LogServerProperties#getMaxRetries()})와 재시도 간격({@link LogServerProperties#getRetryDelay()})에 따라 재시도합니다.
     * 모든 재시도 후에도 전송에 실패하면, 최종적으로 디스크 큐에 저장하기 위해 {@code false}를 반환하고 {@link #saveBatchToDisk(String)}가 호출되도록 합니다.
     * 서버가 사용 불가능({@link #isServerAvailable}가 false)으로 표시된 경우 즉시 실패 처리합니다.
     *
     * @param batch 전송할 개별 로그 메시지(JSON 문자열)의 리스트. 각 문자열은 유효한 JSON 객체여야 합니다.
     * @return 전송에 성공하면 {@code true}, 재시도를 포함하여 최종적으로 실패하면 {@code false}.
     */
    private boolean sendBatchWithRetries(List<String> batch) {
        if (batch == null || batch.isEmpty()) {
            return true; // 빈 배치는 성공으로 처리
        }

        // 로그 배치를 JSON 배열 문자열로 변환
        String jsonBatch = convertBatchToJsonString(batch);
        if (jsonBatch == null) {
            // 배치 변환 실패 기록
            if (metrics != null) {
                metrics.incrementFailedLogs(batch.size());
            }
            return false;
        }

        // 최대 재시도 횟수만큼 시도
        int maxRetries = properties.getMaxRetries();
        
        // 서버가 이전에 이미 사용 불가였고, 지수 백오프 사용 중이라면 현재 지연값 사용
        if (!isServerAvailable.get() && properties.isUseExponentialBackoff()) {
            try {
                Thread.sleep(currentBackoffDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("재시도 지연 중 인터럽트 발생", e);
            }
        }
        
        // 첫 번째 시도와 마지막 실패만 로그에 출력하기 위한 플래그
        boolean logFirstAttempt = true;
        
        Exception lastException = null;
        for (int retry = 1; retry <= maxRetries; retry++) {
            boolean isFirstAttempt = (retry == 1);
            boolean isLastAttempt = (retry == maxRetries);
            
            try {
                if (executeSend(jsonBatch)) {
                    // 전송 성공
                    if (!isServerAvailable.getAndSet(true)) {
                        // 서버 상태가 불가능에서 가능으로 변경됨을 로그로 기록
                        logger.info("서버 연결이 복구되었습니다: {}", properties.getUrl());
                    }
                    // 성공 시 백오프 지연값 초기화
                    if (properties.isUseExponentialBackoff()) {
                        currentBackoffDelay = properties.getInitialBackoffDelay();
                    }
                    
                    // 재시도 중 성공한 경우 (첫 시도 아닌 경우) 성공 로그 출력
                    if (retry > 1) {
                        logger.info("재시도 성공 ({}/{}): 로그 배치 전송 완료", retry, maxRetries);
                    }
                    
                    // 성공 시 메트릭 업데이트
                    if (metrics != null) {
                        metrics.incrementProcessedLogs(batch.size());
                    }
                    return true;
                }
            } catch (Exception e) {
                lastException = e;
                
                // 첫 시도 또는 마지막 시도만 로그 출력
                if (isFirstAttempt && logFirstAttempt) {
                    logConnectionError("로그 배치 전송 중 네트워크 오류 발생", e, retry, maxRetries);
                    logFirstAttempt = false; // 첫 시도 로그는 이미 출력함
                } else if (isLastAttempt) {
                    // 마지막 시도 실패 시에만 로그 출력
                    logConnectionError("모든 재시도 후 로그 전송 실패", e, retry, maxRetries);
                }
                
                // 서버 상태를 사용 불가로 설정
                if (isServerAvailable.getAndSet(false)) {
                    // 서버 상태가 가능에서 불가능으로 변경됨을 로그로 기록
                    // 연결 오류 로그 억제 설정 적용
                    if (!properties.isSuppressConnectionErrors() || 
                        (errorLogsInPeriod.get() <= properties.getMaxConnectionErrorLogsPerPeriod())) {
                        logger.error("서버 연결이 중단되었습니다: {}", properties.getUrl());
                    }
                }
                
                // 마지막 시도가 아니면 재시도 전 지연
                if (!isLastAttempt) {
                    long delayMs;
                    
                    if (properties.isUseExponentialBackoff()) {
                        // 지수 백오프 적용 (약간의 랜덤성 추가)
                        delayMs = currentBackoffDelay + (random.nextInt(1000) - 500); // ±500ms 랜덤성
                        
                        // 다음 백오프 지연값 계산 (최대값 제한)
                        currentBackoffDelay = Math.min(
                            currentBackoffDelay * 2,    // 지수적 증가
                            properties.getMaxBackoffDelay()  // 최대값 제한
                        );
                        
                        // 디버그 레벨로 변경
                        if (logger.isDebugEnabled()) {
                            logger.debug("{}초 후 재전송 시도 예정 (재시도 {}/{})", 
                                    Math.round(delayMs/1000.0), retry + 1, maxRetries);
                        }
                    } else {
                        // 고정 지연 사용
                        delayMs = properties.getRetryDelay();
                        
                        // 디버그 레벨로 변경
                        if (logger.isDebugEnabled()) {
                            logger.debug("{}ms 후 재전송 시도 예정 (재시도 {}/{})", 
                                    delayMs, retry + 1, maxRetries);
                        }
                    }
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                        logger.warn("재시도 지연 중 인터럽트 발생", ie);
                        break; // 인터럽트 발생 시 재시도 중단
                    }
                }
            }
        }

        // 모든 재시도 실패 후 디스크에 저장 시도
        if (effectiveDiskQueueEnabled) {
            saveBatchToDisk(jsonBatch);
            return false; // 전송 실패로 간주
        } else {
            logger.error("로그 배치 전송이 {} 회 재시도 후 최종 실패했으며, 디스크 큐가 비활성화되어 있어 로그가 손실됩니다.", 
                    maxRetries, lastException);
            return false;
        }
    }

    /**
     * HTTP 요청에 사용할 엔티티를 생성합니다. 압축 설정에 따라 다른 방식으로 처리합니다.
     *
     * @param jsonData JSON 데이터 문자열
     * @return 요청에 사용할 HttpEntity
     * @throws IOException 압축 중 오류 발생 시
     */
    private HttpEntity createRequestEntity(String jsonData) throws IOException {
        if (properties.isCompressLogs()) {
            // 압축 처리 로직
            byte[] originalData = jsonData.getBytes(StandardCharsets.UTF_8);
            byte[] compressedJson = compressData(originalData);
            
            if (logger.isDebugEnabled()) {
                logger.debug("로그 압축 적용: 원본 크기={}바이트, 압축 후 크기={}바이트, 압축률={}%", 
                    originalData.length, compressedJson.length, 
                    Math.round((1 - (double)compressedJson.length / originalData.length) * 100));
            }
            
            // 압축된 데이터로 엔티티 생성
            ByteArrayEntity entity = new ByteArrayEntity(compressedJson);
            entity.setContentType("application/json");
            return entity;
        } else {
            // 압축 없이 일반 텍스트로 전송
            StringEntity entity = new StringEntity(jsonData, StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            
            if (logger.isDebugEnabled()) {
                logger.debug("로그 압축 미적용: 전송 크기={}바이트", jsonData.getBytes(StandardCharsets.UTF_8).length);
            }
            
            return entity;
        }
    }

    /**
     * 요청 헤더에 API 키를 추가합니다.
     * 
     * @param post HTTP POST 요청 객체
     */
    private void addApiKeyHeaders(HttpPost post) {
        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            post.setHeader("X-API-Key", apiKey);
        }
    }

    /**
     * 지수 백오프 지연 시간을 초기화합니다.
     */
    private void resetBackoffDelay() {
        if (properties.isUseExponentialBackoff()) {
            currentBackoffDelay = properties.getInitialBackoffDelay();
        }
    }
    
    /**
     * 재시도 횟수에 따른 지연 시간을 계산합니다.
     * 
     * @param retry 현재 재시도 횟수
     * @return 지연 시간 (밀리초)
     */
    private long calculateBackoffDelay(int retry) {
        if (properties.isUseExponentialBackoff()) {
            long jitter = random.nextInt(1000) - 500; // ±500ms 랜덤성
            return currentBackoffDelay + jitter;
        } else {
            return properties.getRetryDelay();
        }
    }
    
    /**
     * 지수 백오프 지연 시간을 업데이트합니다.
     */
    private void updateBackoffDelay() {
        if (properties.isUseExponentialBackoff()) {
            currentBackoffDelay = Math.min(
                currentBackoffDelay * 2,
                properties.getMaxBackoffDelay()
            );
        }
    }

    /**
     * HTTP 요청 실행 메소드. 로그 배치를 중앙 서버로 POST 요청을 통해 전송합니다.
     *
     * @param jsonBatch 전송할 JSON 배치 문자열 (단일 로그 또는 로그 배열)
     * @return 전송 성공 시 true, 실패 시 false
     * @throws IOException HTTP 클라이언트 오류, 네트워크 오류 또는 서버 응답 처리 중 오류 발생 시
     */
    private boolean executeSend(String jsonBatch) throws IOException {
        // URL이 null이거나 비어있으면 빠른 실패
        if (properties.getUrl() == null || properties.getUrl().isEmpty()) {
            logger.error("로그 서버 URL이 설정되지 않았습니다. 로그를 전송할 수 없습니다.");
            return false;
        }

        HttpPost post = new HttpPost(properties.getUrl());
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        
        // API 키 설정이 있으면 요청 헤더에 추가
        addApiKeyHeaders(post);

        // 요청 엔티티 생성 및 설정
        post.setEntity(createRequestEntity(jsonBatch));
        if (properties.isCompressLogs()) {
            post.setHeader("Content-Encoding", "gzip");
        }

        // 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        post.setConfig(requestConfig);

        logger.debug("로그 전송 시도: URL={}", properties.getUrl());
                
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("서버 응답 상태 코드: {}", statusCode);
            
            // 성공적인 응답 코드 범위 (200~299)
            if (statusCode >= 200 && statusCode < 300) {
                logger.debug("로그 배치 전송 성공. 상태 코드: {}", statusCode);
                return true;
            } else {
                logger.warn("로그 배치 전송 실패. 서버 응답 상태 코드: {}", statusCode);
                // 서버 인증 오류 (API 키 관련) 특별 처리
                if (statusCode == 401 || statusCode == 403) {
                    logger.error("인증 오류 (상태 코드: {}). API 키 설정을 확인하세요.", statusCode);
                    if (properties.isValidateApiKey() && (properties.getApiKey() == null || properties.getApiKey().isEmpty())) {
                        logger.error("API 키 검증이 활성화되어 있지만 API 키가 설정되지 않았습니다. " +
                                "'cholog.logger.api-key' 속성으로 유효한 API 키를 설정하거나, " +
                                "'cholog.logger.validate-api-key=false'로 검증을 비활성화하세요.");
                    }
                }
                
                try {
                    if (response.getEntity() != null) {
                        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        if (responseBody != null && !responseBody.isEmpty()) {
                            logger.debug("서버 응답 본문: {}", responseBody);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("응답 본문을 읽을 수 없음: {}", e.getMessage());
                }
                
                return false;
            }
        }
    }

    /**
     * 전송에 최종 실패한 로그 배치(JSON 배열 문자열)를 디스크 파일로 저장합니다.
     * 디스크 큐 기능({@link #effectiveDiskQueueEnabled})이 활성화되어 있고 디스크 큐 디렉토리({@link #diskQueueDir})가 유효할 경우에만 실행됩니다.
     * 파일 이름은 현재 타임스탬프와 UUID를 조합하여 고유하게 생성되며, {@code .logbatch} 확장자를 가집니다.
     * 저장 시 발생할 수 있는 {@link IOException}은 로그로 기록만 하고 전파하지 않습니다.
     * 디스크 큐의 최대 크기를 확인하고 제한을 초과할 경우 가장 오래된 파일부터 삭제합니다.
     *
     * @param jsonBatch 디스크에 저장할 JSON 배열 문자열. 일반적으로 여러 JSON 로그 객체를 포함하는 배열 형태입니다.
     */
    private void saveBatchToDisk(String jsonBatch) {
        if (!effectiveDiskQueueEnabled || diskQueueDir == null) {
            logger.warn("디스크 큐가 비활성화되어 있어 로그 배치를 저장할 수 없습니다.");
            return;
        }

        // 디스크 큐 작업을 동기화하여 경합 상태 방지
        synchronized (diskQueueLock) {
            // 디스크 용량 관리 수행
            manageDiskQueueSize();
            
            // 고유한 파일명으로 저장
            saveJsonBatchToFile(jsonBatch, null);
        }
    }

    /**
     * 디스크 큐 파일 정리 및 최대 용량 관리를 담당합니다.
     * 주기적으로 호출하여 디스크 용량을 모니터링하고 필요 시 오래된 파일을 정리합니다.
     */
    private void manageDiskQueueSize() {
        if (!effectiveDiskQueueEnabled || diskQueueDir == null) {
            return;
        }
        
        // 이미 동기화된 컨텍스트에서 호출된다고 가정
        try {
            // 디스크 큐 디렉토리 크기 확인
            long currentSize = calculateDirectorySize(diskQueueDir);
            long maxSizeBytes = properties.getMaxDiskQueueSizeBytes();
            
            // 최대 크기가 지정된 경우(0보다 큰 경우) 디렉토리 크기 체크
            if (maxSizeBytes > 0 && currentSize > maxSizeBytes) {
                cleanupOldestFiles(currentSize, maxSizeBytes);
            }
        } catch (IOException e) {
            logger.error("디스크 큐 크기 관리 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 디스크 큐에 로그 배치를 저장합니다.
     * 동기화된 컨텍스트에서 호출되어야 합니다.
     * 
     * @param jsonBatch 저장할 JSON 배치
     * @param fileName 파일 이름 (null인 경우 자동 생성)
     * @return 저장 성공 여부
     */
    private boolean saveJsonBatchToFile(String jsonBatch, String fileName) {
        try {
            if (fileName == null) {
                fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + DISK_QUEUE_FILE_SUFFIX;
            }
            
            Path filePath = diskQueueDir.resolve(fileName);
            
            Files.write(
                filePath,
                jsonBatch.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            
            if (properties.isVerboseDiskQueueLogs()) {
                logger.info("로그 배치를 디스크에 저장했습니다: {}", fileName);
            }
            
            // 메트릭 업데이트
            if (metrics != null) {
                try {
                    int logCount = estimateLogCount(jsonBatch);
                    if (logCount > 0) {
                        metrics.incrementFailedLogs(logCount);
                    }
                } catch (Exception e) {
                    metrics.incrementFailedLogs(1);
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("로그 배치를 디스크에 저장하는 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * JSON 배치 문자열에서 로그 항목 수를 추정합니다.
     * 
     * @param jsonBatch JSON 배치 문자열
     * @return 추정된 로그 항목 수
     */
    private int estimateLogCount(String jsonBatch) {
        int logCount = 0;
        
        if (jsonBatch.startsWith("[") && jsonBatch.endsWith("]")) {
            // 간단한 배열 검증
            String trimmed = jsonBatch.trim();
            
            if (trimmed.length() > 2) { // "[]"보다 큰 경우
                // JSON 배열 내의 최상위 항목 수 (배열 내부 구조 분석)
                int bracketDepth = 0;
                int braceDepth = 0;
                boolean inQuote = false;
                boolean escaped = false;
                
                // 첫 번째 '[' 건너뛰기
                for (int i = 1; i < trimmed.length() - 1; i++) {
                    char c = trimmed.charAt(i);
                    
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    
                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }
                    
                    if (c == '"' && !escaped) {
                        inQuote = !inQuote;
                        continue;
                    }
                    
                    if (inQuote) {
                        continue;
                    }
                    
                    if (c == '[') {
                        bracketDepth++;
                    } else if (c == ']') {
                        bracketDepth--;
                    } else if (c == '{') {
                        braceDepth++;
                    } else if (c == '}') {
                        braceDepth--;
                    } else if (c == ',' && bracketDepth == 0 && braceDepth == 0) {
                        // 최상위 배열의 요소를 구분하는 콤마
                        logCount++;
                    }
                }
                
                // 마지막 요소도 카운트 (콤마로 끝나지 않으므로)
                logCount++;
            }
        } else {
            // 배열이 아닌 경우 단일 로그로 가정
            logCount = 1;
        }
        
        return logCount;
    }

    /**
     * 디스크 큐 디렉토리의 현재 총 크기를 바이트 단위로 계산합니다.
     * 디렉토리 내의 파일들의 크기 합계를 반환합니다.
     * 심볼릭 링크나 하위 디렉토리는 계산에서 제외됩니다 (depth 1).
     * 특정 시스템 디렉토리(errors, retried 등)도 계산에서 제외합니다.
     *
     * @param directory 크기를 계산할 디렉토리 경로. null이 아니어야 합니다.
     * @return 디렉토리 내 파일들의 총 크기 (bytes).
     * @throws IOException 파일 시스템 접근 오류 시.
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        Objects.requireNonNull(directory, "Directory path cannot be null for size calculation.");
        long size = 0;
        
        // 계산에서 제외할 디렉토리 이름 목록
        final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
            "errors", RETRIED_FOLDER_NAME, "tmp", "temp", "backup"
        ));
        
        try (Stream<Path> stream = Files.list(directory)) { // depth 1, 즉 해당 디렉토리의 직속 내용만
            for (Path path : stream.collect(Collectors.toList())) {
                // 디렉토리는 크기 계산에서 제외
                if (Files.isDirectory(path)) {
                    String dirName = path.getFileName().toString();
                    if (EXCLUDED_DIRS.contains(dirName)) {
                        // 시스템 디렉토리는 건너뛰기
                        continue;
                    }
                    // 다른 디렉토리도 크기 계산에서 제외
                    continue;
                }
                
                // 로그 배치 파일만 크기 계산에 포함 (.logbatch 확장자)
                if (Files.isRegularFile(path) && path.toString().endsWith(DISK_QUEUE_FILE_SUFFIX)) {
                    try {
                        size += Files.size(path);
                    } catch (IOException e) {
                        logger.warn("파일 크기 조회 실패: {} (원인: {}). 건너뜀.", path, e.getMessage());
                        // 개별 파일 크기 조회 실패 시, 전체 계산은 계속 진행
                    }
                }
            }
        }
        return size;
    }

    /**
     * 디스크 큐 디렉토리의 파일 크기가 제한을 초과할 경우,
     * 가장 오래된 로그 배치 파일부터 삭제하여 지정된 목표 크기 이하로 유지합니다.
     * 안전한 동시성 처리를 위해 diskQueueLock으로 동기화된 컨텍스트에서만 호출되어야 합니다.
     *
     * @param currentSize 현재 디렉토리 크기 (바이트)
     * @param targetSize 정리 후 목표 크기 (바이트)
     * @throws IOException 파일 시스템 접근 오류 발생 시
     */
    private void cleanupOldestFiles(long currentSize, long targetSize) throws IOException {
        if (diskQueueDir == null) {
            return;
        }
        
        // 동기화 블록 내에서 호출되므로 여기서는 추가 동기화를 하지 않음
        // 파일 정렬 및 필터링을 위한 Predicate와 Comparator 정의
        Predicate<Path> isLogBatchFile = p -> 
            Files.isRegularFile(p) && p.toString().endsWith(DISK_QUEUE_FILE_SUFFIX);
        
        Comparator<Path> byCreationTime = (p1, p2) -> {
            try {
                long time1 = Files.getLastModifiedTime(p1).toMillis();
                long time2 = Files.getLastModifiedTime(p2).toMillis();
                return Long.compare(time1, time2);
            } catch (IOException e) {
                logger.warn("파일 수정 시간 비교 중 오류: {}", e.getMessage());
                return 0; // 비교 불가 시 동등 처리
            }
        };
        
        // 파일 목록 가져오기 - 오래된 파일 우선 정렬
        List<Path> files;
        try (Stream<Path> fileStream = Files.list(diskQueueDir)) {
            files = fileStream
                .filter(isLogBatchFile)
                .sorted(byCreationTime)
                .collect(Collectors.toList());
        }
        
        // 목표: 현재 크기 - 목표 크기 만큼의 공간 확보
        long sizeToFree = currentSize - targetSize;
        long freedSize = 0;
        int deletedCount = 0;
        int failedCount = 0;
        
        // 안전하게 파일 삭제 진행
        for (Path file : files) {
            if (freedSize >= sizeToFree) {
                break; // 목표 크기에 도달하면 중단
            }
            
            try {
                // 파일 크기 읽기 및 삭제 시도를 원자적 작업으로 처리
                AtomicLong fileSize = new AtomicLong(0);
                
                // 파일 크기 먼저 확인 (삭제 전)
                if (Files.exists(file) && Files.isRegularFile(file)) {
                    fileSize.set(Files.size(file));
                }
                
                // 삭제 시도
                if (fileSize.get() > 0 && Files.deleteIfExists(file)) {
                    freedSize += fileSize.get();
                    deletedCount++;
                    if (properties.isVerboseDiskQueueLogs()) {
                        logger.info("디스크 큐 용량 확보를 위해 오래된 로그 파일 삭제: {} ({} 바이트)", 
                            file.getFileName(), fileSize.get());
                    }
                }
            } catch (IOException e) {
                logger.warn("오래된 로그 파일 삭제 중 오류 발생: {} - {}", file.getFileName(), e.getMessage());
                failedCount++;
                // 한 파일 삭제 실패해도 계속 진행
            }
        }
        
        // 정리 작업 결과 요약 로깅
        if (deletedCount > 0 || failedCount > 0) {
            double initialSizeMB = currentSize / (1024.0 * 1024);
            double finalSizeMB = (currentSize - freedSize) / (1024.0 * 1024);
            
            logger.info("디스크 큐 정리 완료: {} 파일 삭제 ({} 실패), {} 바이트 확보 (현재 사용량: {:.2f} → {:.2f} MB)", 
                deletedCount, failedCount, freedSize, 
                initialSizeMB, finalSizeMB);
        }
    }

    /**
     * 스케줄러에 의해 주기적으로 호출되어 중앙 로그 서버와의 연결 상태를 확인합니다.
     * 설정된 연결 확인 간격({@link #CONNECTION_CHECK_INTERVAL})마다 실행됩니다.
     * 로그 서버 URL로 간단한 HTTP GET 요청을 보내 응답 상태 코드를 확인합니다.
     * 2xx 응답을 받으면 서버가 사용 가능하다고 판단하고 {@link #isServerAvailable}를 {@code true}로 설정합니다.
     * 이전에 서버가 사용 불가능 상태였다가 다시 사용 가능 상태로 변경되면, 디스크에 저장된 로그 재전송({@link #resendFromDisk()})을 시도합니다.
     * 연결에 실패하거나 2xx 외의 응답을 받으면 서버가 사용 불가능하다고 판단하고, 경고 로그를 남깁니다.
     * 로그 서버 URL이 설정되지 않은 경우 아무 작업도 수행하지 않습니다.
     */
    private void checkServerConnection() {
        long currentTime = System.currentTimeMillis();
        // 마지막 체크 시간과 현재 시간의 차이가 CHECK_INTERVAL보다 작으면 체크하지 않음
        // 단, 서버가 불가용 상태일 경우 더 자주 체크 (10초마다)
        long interval = isServerAvailable.get() ? properties.getConnectionCheckInterval() : Math.min(10_000, properties.getConnectionCheckInterval());
        if (currentTime - lastConnectionCheckTime.get() < interval) {
            return;
        }

        lastConnectionCheckTime.set(currentTime);
        String url = properties.getUrl();
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        HttpGet request = new HttpGet(url);
        request.setConfig(RequestConfig.custom()
                .setConnectTimeout((int)properties.getConnectionCheckTimeout())
                .setSocketTimeout((int)properties.getConnectionCheckTimeout())
                .build());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            boolean wasUnavailable = !isServerAvailable.get();
            boolean isNowAvailable = statusCode >= 200 && statusCode < 300;
            isServerAvailable.set(isNowAvailable);
            
            // 상태 변경 시에만 로그 출력
            if (wasUnavailable && isNowAvailable) {
                logger.info("서버 연결이 복구되었습니다. 정상 운영을 재개합니다.");
                // 연결이 복구되면 즉시 디스크 큐 처리 시도
                // 비동기로 처리하여 메인 스레드 차단 방지
                CompletableFuture.runAsync(this::resendFromDisk, scheduler);
            } else if (!wasUnavailable && !isNowAvailable) {
                // 상태가 사용 가능에서 불가능으로 변경된 경우만 로그 출력
                // 연결 오류 로그 억제 설정 적용
                if (properties.isSuppressConnectionErrors()) {
                    long now = System.currentTimeMillis();
                    long lastErrorTime = lastErrorLogTime.get();
                    long errorPeriod = properties.getConnectionErrorLogPeriod();
                    int maxLogsPerPeriod = properties.getMaxConnectionErrorLogsPerPeriod();

                    // 새 기간이 시작된 경우 카운터 초기화
                    if (now - lastErrorTime > errorPeriod) {
                        errorLogsInPeriod.set(0);
                        lastErrorLogTime.set(now);
                    }

                    // 현재 기간 내 로그 개수가 최대값보다 작은 경우에만 로그 출력
                    if (errorLogsInPeriod.incrementAndGet() <= maxLogsPerPeriod) {
                        logger.warn("서버 연결이 끊어졌습니다. {}초 후 재시도합니다.", 
                        interval / 1000);
                    }
                } else {
                    logger.warn("서버 연결이 끊어졌습니다. {}초 후 재시도합니다.", 
                            interval / 1000);
                }
            } else if (wasUnavailable && !isNowAvailable) {
                // 계속 연결 불가능한 상태 - 디버그 레벨로만 로그 출력
                if (logger.isDebugEnabled()) {
                    logger.debug("서버 연결이 계속 불가능합니다. {}초 후 다시 확인합니다.", 
                            interval / 1000);
                }
            }
        } catch (IOException e) {
            boolean wasAvailable = isServerAvailable.getAndSet(false);
            
            // 상태가 변경된 경우만 로그 출력 (사용 가능에서 불가능으로)
            if (wasAvailable) {
                // 연결 오류 로그 억제 설정 적용
                if (properties.isSuppressConnectionErrors()) {
                    long now = System.currentTimeMillis();
                    long lastErrorTime = lastErrorLogTime.get();
                    long errorPeriod = properties.getConnectionErrorLogPeriod();
                    int maxLogsPerPeriod = properties.getMaxConnectionErrorLogsPerPeriod();

                    // 새 기간이 시작된 경우 카운터 초기화
                    if (now - lastErrorTime > errorPeriod) {
                        errorLogsInPeriod.set(0);
                        lastErrorLogTime.set(now);
                    }

                    // 현재 기간 내 로그 개수가 최대값보다 작은 경우에만 로그 출력
                    if (errorLogsInPeriod.incrementAndGet() <= maxLogsPerPeriod) {
                        logger.warn("서버 연결이 끊어졌습니다. {}초 후 재시도합니다.", 
                    interval / 1000);
                    }
                } else {
                    logger.warn("서버 연결이 끊어졌습니다. {}초 후 재시도합니다.", 
                            interval / 1000);
                }
            } else {
                // 계속 연결 불가능한 상태 - 디버그 레벨로만 로그 출력
                if (logger.isDebugEnabled()) {
                    logger.debug("서버 연결이 계속 불가능합니다. {}초 후 다시 확인합니다.", 
                            interval / 1000);
                }
            }
        }
    }

    /**
     * 스케줄러에 의해 주기적으로 호출되어 디스크 큐 디렉토리({@link #diskQueueDir})에 저장된 로그 배치 파일들을 찾아 서버로 재전송을 시도합니다.
     * 디스크 큐 기능({@link #effectiveDiskQueueEnabled})이 활성화되어 있고 디스크 큐 디렉토리가 유효할 경우에만 실행됩니다.
     * {@code .logbatch} 확장자를 가진 파일들을 오래된 순서대로 읽어 {@link #sendBatchWithRetries(List)}를 통해 전송을 시도합니다.
     * 성공적으로 전송된 파일은 삭제되고, 손상된 파일은 오류 디렉토리로 이동됩니다.
     */
    private void resendFromDisk() {
        if (!effectiveDiskQueueEnabled || diskQueueDir == null) {
            return;
        }

        // 서버가 사용 불가능한 상태면 빠르게 종료
        if (!isServerAvailable.get()) {
            logger.debug("로그 서버가 현재 사용 불가능 상태입니다. 디스크 큐 처리를 건너뜁니다.");
            return;
        }

        // 오류 파일 디렉토리 준비
        Path errorDir = null;
        Path retriedDir = null;
        try {
            errorDir = diskQueueDir.resolve("errors");
            if (!Files.exists(errorDir)) {
                Files.createDirectories(errorDir);
            }
            
            // 재시도 횟수 초과 파일 디렉토리 준비
            retriedDir = diskQueueDir.resolve(RETRIED_FOLDER_NAME);
            if (!Files.exists(retriedDir)) {
                Files.createDirectories(retriedDir);
            }
        } catch (IOException e) {
            logger.warn("오류/재시도 파일 디렉토리 생성 실패: {}", e.getMessage());
            // 디렉토리 생성 실패해도 계속 진행
        }

        // 파일별 재시도 횟수 추적을 위한 맵 (필요한 경우 디스크에 유지할 수 있음)
        Map<String, Integer> retryCountMap = new HashMap<>();

        try {
            List<Path> files = Files.list(diskQueueDir)
                    .filter(p -> p.toString().endsWith(DISK_QUEUE_FILE_SUFFIX))
                    .sorted() // 오래된 파일 먼저 처리 시도
                    .collect(Collectors.toList());

            for (Path file : files) {
                if (!active.get() || Thread.currentThread().isInterrupted()) {
                    logger.info("서비스 종료 또는 스레드 인터럽트로 디스크 재전송 중단.");
                    break;
                }
                
                // 서버 가용성 다시 확인 (배치 처리 중 상태가 변할 수 있음)
                if (!isServerAvailable.get()) {
                    logger.debug("로그 서버 연결이 중간에 끊겼습니다. 나머지 디스크 큐 처리를 중단합니다.");
                    break;
                }
                
                String fileName = file.getFileName().toString();
                
                // 재시도 카운트 가져오기 또는 초기화
                int retryCount = retryCountMap.getOrDefault(fileName, 0);
                
                // 최대 재시도 횟수를 초과한 경우 retried 폴더로 이동
                if (retryCount >= MAX_BATCH_RETRY_ATTEMPTS) {
                    try {
                        if (retriedDir != null && Files.exists(retriedDir)) {
                            Path targetPath = retriedDir.resolve(file.getFileName());
                            Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            logger.warn("최대 재시도 횟수({})를 초과한 로그 배치 파일을 이동: {} -> {}",
                                    MAX_BATCH_RETRY_ATTEMPTS, file.getFileName(), targetPath);
                            // 맵에서 제거
                            retryCountMap.remove(fileName);
                        } else {
                            // retried 디렉토리가 없는 경우 계속 처리 시도
                            logger.warn("최대 재시도 횟수({})를 초과했지만 retried 디렉토리가 없어 계속 처리: {}",
                                    MAX_BATCH_RETRY_ATTEMPTS, file.getFileName());
                        }
                        continue; // 다음 파일로 넘어감
                    } catch (IOException e) {
                        logger.error("최대 재시도 횟수 초과 파일 이동 실패: {}", e.getMessage());
                        // 이동 실패해도 계속 처리 시도
                    }
                }
                
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    
                    // 파일 내용 검증 - JSON 배열 형식인지 확인
                    boolean isValidJson = false;
                    try {
                        // 간단히 첫 문자와 마지막 문자로 JSON 배열 형식 확인
                        content = content.trim();
                        isValidJson = content.startsWith("[") && content.endsWith("]");
                    } catch (Exception e) {
                        logger.warn("로그 배치 파일 내용이 유효한 JSON 형식이 아님: {}", file.getFileName());
                    }
                    
                    if (!isValidJson) {
                        // 손상된 파일은 오류 디렉토리로 이동 또는 삭제
                        handleCorruptedFile(file, errorDir);
                        retryCountMap.remove(fileName); // 맵에서 제거
                        continue;
                    }
                    
                    // 파일 내용을 직접 전송 (이미 JSON 배열이므로 다시 배열로 감싸지 않음)
                    boolean success = false;
                    try {
                        // 디스크에서 읽은 JSON 배열 형식의 content를 직접 전송
                        success = executeSendDiskBatch(content);
                    } catch (Exception e) {
                        logger.error("디스크 배치 전송 중 오류 발생: {} - {}", file.getFileName(), e.getMessage());
                        if (!isServerAvailable.get()) {
                            // 서버 연결이 끊긴 경우, 다음 주기에 다시 시도
                            retryCount++;
                            retryCountMap.put(fileName, retryCount);
                            break;
                        }
                    }
                    
                    if (success) {
                        Files.delete(file);
                        logger.info("디스크에서 로그 배치 재전송 성공: {}", file.getFileName());
                        retryCountMap.remove(fileName); // 맵에서 제거
                    } else {
                        // 전송 실패 시 재시도 카운트 증가 후 맵에 저장
                        retryCount++;
                        retryCountMap.put(fileName, retryCount);
                        
                        logger.warn("디스크에서 로그 배치 재전송 실패 (재시도 {}/{}): {}", 
                                retryCount, MAX_BATCH_RETRY_ATTEMPTS, file.getFileName());
                        
                        // break 제거: 하나의 파일 실패로 인해 모든 파일 처리가 중단되지 않도록 함
                        // 대신 서버 가용성이 false로 바뀌었다면 중단
                        if (!isServerAvailable.get()) {
                            logger.warn("서버 연결 상태가 불안정합니다. 나머지 처리는 다음 주기에 시도합니다.");
                            break;
                        }
                        
                        // 너무 많은 파일이 연속 실패하면 잠시 지연
                        if (retryCount % 3 == 0) {
                            try {
                                Thread.sleep(1000); // 1초 지연
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("디스크 큐 파일 처리 중 오류 발생 {}: {}", file.getFileName(), e.getMessage());
                    // 손상된 파일은 오류 디렉토리로 이동 또는 삭제
                    handleCorruptedFile(file, errorDir);
                    retryCountMap.remove(fileName); // 맵에서 제거
                } catch (Exception e) {
                    logger.error("디스크 큐 파일 처리 중 예상치 못한 오류 발생 {}: {}", file.getFileName(), e.getMessage());
                    // 손상된 파일은 오류 디렉토리로 이동 또는 삭제
                    handleCorruptedFile(file, errorDir);
                    retryCountMap.remove(fileName); // 맵에서 제거
                }
            }
        } catch (IOException e) {
            logger.error("디스크 큐 디렉토리 접근 실패 {}: {}", diskQueueDir, e.getMessage());
        }
    }

    /**
     * 손상된 로그 배치 파일을 처리합니다.
     * 먼저 복구를 시도하고, 실패하면 오류 디렉토리로 이동하거나 삭제합니다.
     *
     * @param file 손상된 파일 경로
     * @param errorDir 오류 파일 저장 디렉토리 (null일 수 있음)
     */
    private void handleCorruptedFile(Path file, Path errorDir) {
        if (file == null || !Files.exists(file)) {
            logger.warn("처리할 파일이 존재하지 않습니다.");
            return;
        }

        try {
            // 파일 복구 시도
            boolean recovered = attemptToRecoverFile(file);
            
            if (recovered) {
                logger.info("손상된 로그 배치 파일 복구 성공: {}", file.getFileName());
                return;
            }
            
            // 복구 실패 시 오류 디렉토리로 이동 처리
            if (errorDir != null && Files.exists(errorDir) && Files.isDirectory(errorDir)) {
                // 오류 디렉토리에 이미 동일한 이름의 파일이 있는 경우, 타임스탬프를 추가하여 고유한 이름 생성
                String fileName = file.getFileName().toString();
                String baseName = fileName;
                String extension = "";
                
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }
                
                Path targetPath = errorDir.resolve(baseName + "-" + System.currentTimeMillis() + extension);
                Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.warn("손상된 로그 배치 파일을 오류 디렉토리로 이동: {} -> {}", 
                    file.getFileName(), targetPath);
            } else {
                // 오류 디렉토리가 없으면 삭제
                Files.delete(file);
                logger.warn("손상된 로그 배치 파일 삭제: {}", file.getFileName());
            }
        } catch (IOException e) {
            logger.error("손상된 로그 배치 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            try {
                // 모든 방법이 실패했을 경우, 마지막 수단으로 강제 삭제 시도
                Files.deleteIfExists(file);
                logger.warn("손상된 파일 강제 삭제 시도: {}", file.getFileName());
            } catch (IOException ex) {
                logger.error("손상된 파일 강제 삭제 실패. 파일 시스템 오류 가능성 있음: {}", ex.getMessage());
            }
        }
    }
    
    /**
     * 손상된 로그 배치 파일의 복구를 시도합니다.
     * JSON 배열 구조 및 중첩 레벨을 확인하여 더 정교한 복구를 수행합니다.
     * 
     * @param file 복구할 파일
     * @return 복구 성공 여부
     */
    private boolean attemptToRecoverFile(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            
            // 파일이 비어있는 경우
            if (content == null || content.trim().isEmpty()) {
                logger.warn("빈 로그 파일 발견. 삭제합니다: {}", file.getFileName());
                Files.delete(file);
                return true;
            }
            
            // 내용 트림
            content = content.trim();
            
            // 구조 결함 확인을 위한 변수들
            boolean needsRecovery = false;
            boolean hasJsonObjects = false;
            boolean isJsonArray = content.startsWith("[") && content.endsWith("]");
            
            // 기본 구조 검증
            if (!isJsonArray) {
                needsRecovery = true;
                
                // JSON 객체 존재 확인 ({로 시작하고 }로 끝나는 패턴)
                hasJsonObjects = content.contains("{") && content.contains("}");
                
                // 시작 대괄호 확인
                if (!content.startsWith("[")) {
                    content = "[" + content;
                }
                
                // 끝 대괄호 확인
                if (!content.endsWith("]")) {
                    content = content + "]";
                }
            } else {
                // JSON 배열 구조이지만 내부 유효성 검증
                int braceCount = 0;
                int bracketCount = 0;
                boolean inQuote = false;
                boolean escaped = false;
                
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    
                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }
                    
                    if (c == '"' && !escaped) {
                        inQuote = !inQuote;
                        continue;
                    }
                    
                    if (!inQuote) {
                        if (c == '{') {
                            braceCount++;
                            hasJsonObjects = true;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount < 0) {
                                needsRecovery = true; // 중첩 레벨 불균형
                            }
                        } else if (c == '[') {
                            bracketCount++;
                        } else if (c == ']') {
                            bracketCount--;
                            if (bracketCount < 0) {
                                needsRecovery = true; // 중첩 레벨 불균형
                            }
                        }
                    }
                }
                
                // 배열 내에 JSON 객체가 없는 경우
                if (!hasJsonObjects) {
                    needsRecovery = true;
                }
                
                // 중첩 레벨이 불균형한 경우
                if (braceCount != 0 || bracketCount != 0) {
                    needsRecovery = true;
                }
            }
            
            // 구조적 문제가 있고 복구가 필요하지만, JSON 객체가 존재하지 않는 경우
            if (needsRecovery && !hasJsonObjects) {
                logger.warn("파일에 유효한 JSON 객체가 없어 복구가 불가능합니다: {}", file.getFileName());
                return false;
            }
            
            // 필요한 경우에만 복구 시도
            if (needsRecovery) {
                // 복구 시도: JSON 배열 내에서 개별 JSON 객체 추출 후 재구성
                StringBuilder recoveredContent = new StringBuilder("[");
                boolean firstObject = true;
                
                // 가장 간단한 복구 방법: {} 패턴 검색 (훨씬 정교한 파싱이 필요할 수 있음)
                int start = 0;
                while (true) {
                    int openBrace = content.indexOf('{', start);
                    if (openBrace == -1) break;
                    
                    // 중첩 레벨을 추적하여 매칭되는 닫는 중괄호 찾기
                    int nestedLevel = 1;
                    int closeBrace = -1;
                    
                    for (int i = openBrace + 1; i < content.length(); i++) {
                        char c = content.charAt(i);
                        if (c == '{') {
                            nestedLevel++;
                        } else if (c == '}') {
                            nestedLevel--;
                            if (nestedLevel == 0) {
                                closeBrace = i;
                                break;
                            }
                        }
                    }
                    
                    // 매칭되는 닫는 중괄호를 찾지 못한 경우
                    if (closeBrace == -1) break;
                    
                    // 유효한 JSON 객체 추출
                    String jsonObject = content.substring(openBrace, closeBrace + 1);
                    
                    // 배열에 추가
                    if (!firstObject) {
                        recoveredContent.append(",");
                    }
                    recoveredContent.append(jsonObject);
                    firstObject = false;
                    
                    // 다음 시작 위치 설정
                    start = closeBrace + 1;
                }
                
                recoveredContent.append("]");
                
                // 최종 복구 결과가 원본과 동일하다면 실제로 복구된 것이 없음
                if (recoveredContent.toString().equals(content)) {
                    logger.info("파일은 이미 유효한 형식입니다: {}", file.getFileName());
                    return true;
                }
                
                // 적어도 하나의 JSON 객체를 찾은 경우에만 복구 진행
                if (recoveredContent.length() > 2) { // "[]"보다 길어야 함
                    // 복구된 내용으로 파일 다시 쓰기
                    Files.writeString(file, recoveredContent.toString(), StandardCharsets.UTF_8);
                    logger.info("로그 배치 파일 복구 완료: {} (복구된 객체 수: {}개)", 
                            file.getFileName(), recoveredContent.toString().split(",").length);
                    return true;
                } else {
                    logger.warn("복구 가능한 JSON 객체를 찾지 못했습니다: {}", file.getFileName());
                    return false;
                }
            }
            
            // 이미 올바른 형식이면 복구 필요 없음
            return true;
        } catch (Exception e) {
            logger.warn("파일 복구 시도 실패: {} - {}", file.getFileName(), e.getMessage());
            return false;
        }
    }

    /**
     * Spring 컨테이너가 종료될 때 호출되어 내부 스케줄러와 HTTP 클라이언트 리소스를 정상적으로 종료합니다.
     * {@link DisposableBean} 인터페이스 구현을 통해 자동 호출됩니다.
     * 먼저 서비스 활성 상태 플래그({@link #active})를 {@code false}로 설정하여 새로운 로그가 큐에 추가되는 것을 중단시킵니다.
     * 그 후 스케줄러 서비스를 종료하고, 메모리 큐에 남아있는 로그들의 마지막 전송을 시도합니다.
     * 마지막으로 HTTP 클라이언트를 닫아 모든 관련 리소스를 해제합니다.
     *
     * @throws Exception 스케줄러 종료 또는 HTTP 클라이언트 종료 중 예외 발생 시 (거의 발생하지 않음)
     */
    @Override
    public void destroy() throws Exception {
        logger.info("LogSenderService 종료 중...");
        
        try {
            // 활성 상태를 false로 설정하여 더 이상의 로그 추가를 방지
            active.set(false);
            
            // MBean 등록 해제
            if (metrics != null) {
                try {
                    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                    ObjectName name = new ObjectName("com.cholog.logger:type=LogSenderMetrics");
                    if (mbs.isRegistered(name)) {
                        mbs.unregisterMBean(name);
                        logger.info("JMX에서 LogSenderMetrics 등록 해제 완료");
                    }
                } catch (Exception e) {
                    logger.warn("JMX에서 LogSenderMetrics 등록 해제 실패: {}", e.getMessage());
                }
            }
            
            // 큐에 남아있는 로그 처리 시도
            processBatchFromQueue();
            
            // 스케줄러 종료
            shutdownExecutorService(scheduler, "로그 스케줄러");
            
            // HTTP 클라이언트 종료
            closeHttpClient();

            logger.info("LogSenderService 종료 완료.");
        } catch (Exception e) {
            logger.error("LogSenderService 종료 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 주어진 ExecutorService를 안전하게 종료하는 헬퍼 메소드.
     * @param executorService 종료할 ExecutorService
     * @param serviceName 로그 출력을 위한 서비스 이름
     */
    private void shutdownExecutorService(ExecutorService executorService, String serviceName) {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }
        logger.info("Shutting down {}...", serviceName);
        executorService.shutdown(); // 새로운 작업 거부
        try {
            // 지정된 시간 동안 현재 진행중인 작업 완료 대기
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) { // 대기 시간 조정 가능
                logger.warn("{} did not terminate gracefully after 10 seconds. Forcing shutdown...", serviceName);
                List<Runnable> droppedTasks = executorService.shutdownNow(); // 강제 종료 시도
                logger.warn("Forced shutdown for {}. {} tasks were potentially cancelled.", serviceName, droppedTasks.size());
                // 강제 종료 후에도 완전히 종료될 때까지 잠시 더 대기
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                    logger.error("{} did not terminate even after forced shutdown.", serviceName);
            } else {
                logger.info("{} terminated gracefully.", serviceName);
            }
        } catch (InterruptedException ie) {
            logger.warn("Shutdown interrupted while waiting for {}. Forcing shutdown immediately.", serviceName);
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
        }
    }

    /**
     * HttpClient 리소스를 안전하게 해제하는 헬퍼 메소드.
     */
    private void closeHttpClient() {
        logger.info("Closing HttpClient resources...");
        try {
            if (httpClient != null) {
                // PoolingHttpClientConnectionManager를 사용하는 경우, manager 종료도 고려할 수 있으나,
                // 보통 client.close()가 내부적으로 처리함.
                httpClient.close();
                logger.info("HttpClient closed successfully.");
            }
        } catch (IOException e) {
            logger.error("Error occurred while closing HttpClient", e);
        }
    }

    /**
     * Apache HttpClient 인스턴스를 생성하고 설정합니다. (HttpClient 4.x 기반)
     * 커넥션 풀링, 타임아웃, HTTPS (선택적 TLS 검증 무시) 설정을 포함합니다.
     * 이 메서드는 LogSenderService 생성자에서 호출되어 HTTP 클라이언트를 초기화합니다.
     *
     * @return 설정된 {@link CloseableHttpClient} 인스턴스.
     *         SSL 설정 오류 등 심각한 문제 발생 시 기본 HttpClient 인스턴스로 폴백될 수 있습니다.
     */
    private CloseableHttpClient createHttpClient() {
        try {
            HttpClientBuilder clientBuilder = HttpClients.custom();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(CONNECT_TIMEOUT) // 연결 수립 타임아웃
                    .setSocketTimeout(SOCKET_TIMEOUT)   // 데이터 수신 타임아웃
                    .setConnectionRequestTimeout(CONNECT_TIMEOUT) // 커넥션 풀에서 커넥션 요청 타임아웃
                    .build();
            clientBuilder.setDefaultRequestConfig(requestConfig);

            PoolingHttpClientConnectionManager connectionManager;
            if (properties.isUseHttps()) {
                SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
                if (properties.isAllowInsecureTls()) {
                    // 모든 인증서를 신뢰하도록 설정 (프로덕션에서는 절대 사용 금지!)
                    sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                    logger.warn("CHO:LOG - !!! Insecure TLS is ENABLED (allowInsecureTls=true). " +
                                "This configuration bypasses SSL certificate validation and should NOT be used in production environments. !!!");
                }
                SSLContext sslContext = sslContextBuilder.build();
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        properties.isAllowInsecureTls() ? NoopHostnameVerifier.INSTANCE : SSLConnectionSocketFactory.getDefaultHostnameVerifier()
                );
                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory)
                        .build();
                connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } else {
                connectionManager = new PoolingHttpClientConnectionManager();
            }

            connectionManager.setMaxTotal(properties.getHttpClientPoolMaxTotal()); // 전체 최대 커넥션 수
            connectionManager.setDefaultMaxPerRoute(properties.getHttpClientPoolDefaultMaxPerRoute()); // 호스트(라우트)당 최대 커넥션 수
            clientBuilder.setConnectionManager(connectionManager);
            // 유휴 커넥션 정리 주기 설정 (예: 30초마다 만료된 커넥션 및 유휴 커넥션 정리)
            clientBuilder.evictIdleConnections(properties.getHttpClientPoolEvictIdleConnectionsAfter(), TimeUnit.SECONDS);
            clientBuilder.evictExpiredConnections(); // 만료된 커넥션 즉시 제거 활성화

            logger.info("CHO:LOG - HttpClient initialized (Apache HttpClient 4.x based). Pool(MaxTotal:{}, DefaultMaxPerRoute:{}), Timeouts(Conn:{}, Socket:{}), HTTPS:{}, AllowInsecureTLS:{}",
                    connectionManager.getMaxTotal(), connectionManager.getDefaultMaxPerRoute(), CONNECT_TIMEOUT, SOCKET_TIMEOUT, 
                    properties.isUseHttps(), properties.isAllowInsecureTls());

            return clientBuilder.build();

        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            logger.error("CHO:LOG - Failed to create customized HttpClient due to SSL configuration error. Falling back to default HttpClient. Error: {}", e.getMessage(), e);
            return HttpClients.createDefault(); // 심각한 오류 시 기본 클라이언트로 폴백
        }
    }

    /**
     * JMX를 통해 로그 전송 서비스의 성능 메트릭을 등록합니다.
     * 이 메소드는 생성자에서 properties.isExposeMetricsViaJmx()가 true일 때 호출됩니다.
     */
    private void registerJmxMetrics() {
        if (metrics != null) {
            logger.debug("메트릭 객체가 이미 등록되어 있습니다.");
            return;
        }
        
        MBeanServer mbs = null;
        ObjectName name = null;
        
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            name = new ObjectName("com.cholog.logger:type=LogSenderMetrics");
            
            // 이미 등록된 경우 먼저 등록 해제
            if (mbs.isRegistered(name)) {
                try {
                    mbs.unregisterMBean(name);
                    logger.info("이전에 등록된 메트릭 객체를 해제했습니다: {}", name);
                } catch (Exception e) {
                    logger.warn("이전 메트릭 객체 등록 해제 실패: {}", e.getMessage());
                    // 계속 진행, 등록 재시도
                }
            }
        } catch (Exception e) {
            logger.warn("JMX 서버 액세스 실패: {}", e.getMessage());
            return;
        }
        
        // 등록 시도 전에 안전 확인
        if (mbs == null || name == null) {
            logger.warn("JMX 서버 또는 객체 이름이 null입니다. 메트릭을 등록할 수 없습니다.");
            return;
        }
        
        try {
            // 메트릭 객체 생성 (객체 생성 오류 분리)
            metrics = new LogSenderMetrics(logQueue, isServerAvailable, diskQueueDir);
        } catch (Exception e) {
            logger.error("메트릭 객체 생성 실패: {}", e.getMessage(), e);
            metrics = null;
            return;
        }
        
        // 최대 3회 등록 시도
        int maxRegistrationAttempts = 3;
        boolean registered = false;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRegistrationAttempts; attempt++) {
            try {
                mbs.registerMBean(metrics, name);
                registered = true;
                logger.info("LogSenderMetrics가 JMX에 등록되었습니다. 이름: {}", name);
                break;
            } catch (Exception e) {
                lastException = e;
                logger.warn("JMX 등록 시도 {}/{} 실패: {}", attempt, maxRegistrationAttempts, e.getMessage());
                
                if (attempt < maxRegistrationAttempts) {
                    try {
                        // 재시도 전 잠시 대기
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        if (!registered) {
            logger.error("모든 JMX 등록 시도 실패. 메트릭 수집은 계속하지만 JMX를 통한 모니터링은 사용할 수 없습니다.", lastException);
        }
        
        // JMX 등록 여부와 관계없이 메트릭 스케줄링 설정
        if (metrics != null && properties.isMetricsEnabled()) {
            // 실패하더라도 내부 메트릭은 유지
            try {
                scheduler.scheduleAtFixedRate(
                    this::updateMetrics,
                    properties.getMetricsCollectionInterval(),
                    properties.getMetricsCollectionInterval(),
                    TimeUnit.MILLISECONDS
                );
                logger.info("메트릭 수집이 {}ms 간격으로 활성화되었습니다.", properties.getMetricsCollectionInterval());
            } catch (Exception e) {
                logger.warn("메트릭 수집 스케줄링 실패: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 메트릭을 주기적으로 업데이트합니다.
     * 이 메서드는 properties.isMetricsEnabled()가 true일 때 정기적으로 호출됩니다.
     */
    private void updateMetrics() {
        try {
            // 현재 메트릭을 로깅하여 모니터링에 활용
            if (metrics != null) {
                // 디스크 메트릭 업데이트
                metrics.updateDiskMetrics();
                
                if (logger.isDebugEnabled()) {
                    logger.debug("현재 메트릭: {}", metrics);
                }
            }
        } catch (Exception e) {
            logger.warn("메트릭 업데이트 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 데이터를 GZIP으로 압축합니다.
     * 
     * @param data 압축할 바이트 배열
     * @return 압축된 바이트 배열
     * @throws IOException 압축 중 오류 발생 시
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(data);
        }
        return byteStream.toByteArray();
    }

    /**
     * 현재 연결 상태에 따라 로그 메시지를 출력합니다.
     * v1.7.5: 연결 오류 로그를 억제하는 기능이 추가되었습니다.
     *
     * @param message 로그 메시지
     * @param e 예외 객체 (null 가능)
     * @param retry 현재 재시도 횟수
     * @param maxRetries 최대 재시도 횟수
     */
    private void logConnectionError(String message, Exception e, int retry, int maxRetries) {
        if (!properties.isSuppressConnectionErrors()) {
            // 억제 기능이 비활성화된 경우 항상 로그 출력 (스택 트레이스 제외)
            logger.error("{} (재시도 {}/{}): {}", message, retry, maxRetries, e.getMessage());
            return;
        }

        long now = System.currentTimeMillis();
        long lastErrorTime = lastErrorLogTime.get();
        long errorPeriod = properties.getConnectionErrorLogPeriod();
        int maxLogsPerPeriod = properties.getMaxConnectionErrorLogsPerPeriod();

        // 새 기간이 시작된 경우 카운터 초기화
        if (now - lastErrorTime > errorPeriod) {
            errorLogsInPeriod.set(0);
            lastErrorLogTime.set(now);
        }

        // 현재 기간 내 로그 개수가 최대값보다 작은 경우에만 로그 출력
        if (errorLogsInPeriod.incrementAndGet() <= maxLogsPerPeriod) {
            logger.error("{} (재시도 {}/{}): {}", message, retry, maxRetries, e.getMessage());
        }
    }

    /**
     * 로그 목록을 JSON 배열 문자열로 변환합니다.
     *
     * @param batch 변환할 로그 목록
     * @return JSON 배열 문자열, 변환 실패 시 null
     */
    private String convertBatchToJsonString(List<String> batch) {
        if (batch == null || batch.isEmpty()) {
            return "[]";
        }
        
        try {
            // JSON 배열 형식으로 로그 병합
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append(batch.get(i));
            }
            jsonBuilder.append("]");
            return jsonBuilder.toString();
        } catch (Exception e) {
            logger.error("로그 배치를 JSON 문자열로 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 디스크에서 로드한 JSON 형식의 로그 배치를 전송합니다.
     * 이미 JSON 배열 형식인 문자열을 직접 전송하여 중복 배열화를 방지합니다.
     *
     * @param jsonBatchArray 전송할 JSON 배열 문자열 (이미 배열 형식이어야 함)
     * @return 전송 성공 시 true, 실패 시 false
     */
    private boolean executeSendDiskBatch(String jsonBatchArray) throws IOException {
        // URL이 null이거나 비어있으면 빠른 실패
        if (properties.getUrl() == null || properties.getUrl().isEmpty()) {
            logger.error("로그 서버 URL이 설정되지 않았습니다. 로그를 전송할 수 없습니다.");
            return false;
        }

        HttpPost post = new HttpPost(properties.getUrl());
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        
        // API 키 설정이 있으면 요청 헤더에 추가
        addApiKeyHeaders(post);

        // 요청 엔티티 생성 및 설정
        post.setEntity(createRequestEntity(jsonBatchArray));
        if (properties.isCompressLogs()) {
            post.setHeader("Content-Encoding", "gzip");
        }

        // 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        post.setConfig(requestConfig);
        
        // 최대 재시도 횟수만큼 시도
        int maxRetries = properties.getMaxRetries();
        
        for (int retry = 1; retry <= maxRetries; retry++) {
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                // 성공적인 응답 코드 범위 (200~299)
                if (statusCode >= 200 && statusCode < 300) {
                    if (!isServerAvailable.getAndSet(true)) {
                        logger.info("서버 연결이 복구되었습니다: {}", properties.getUrl());
                    }
                    resetBackoffDelay();
                    return true;
                } else {
                    if (retry == maxRetries) {
                        logger.warn("디스크 배치 전송 실패. 서버 응답 상태 코드: {} (재시도 {}/{})", 
                                statusCode, retry, maxRetries);
                    }
                    
                    // 서버 인증 오류 (API 키 관련) 특별 처리
                    if (statusCode == 401 || statusCode == 403) {
                        logger.error("인증 오류 (상태 코드: {}). API 키 설정을 확인하세요.", statusCode);
                        return false; // 인증 오류는 더 이상 시도하지 않음
                    }
                }
            } catch (Exception e) {
                // 마지막 시도만 로그 출력
                if (retry == maxRetries) {
                    logConnectionError("디스크 배치 전송 실패", e, retry, maxRetries);
                }
                
                // 서버 상태를 사용 불가로 설정
                if (isServerAvailable.getAndSet(false)) {
                    if (!properties.isSuppressConnectionErrors()) {
                        logger.error("서버 연결이 중단되었습니다: {}", properties.getUrl());
                    }
                }
                
                // 재시도 전 지연
                if (retry < maxRetries) {
                    try {
                        long delayMs = calculateBackoffDelay(retry);
                        Thread.sleep(delayMs);
                        updateBackoffDelay();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return false; // 모든 재시도 실패
    }
}
