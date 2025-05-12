package com.cholog.logger.service;

import com.cholog.logger.appender.CentralLogAppender;
import com.cholog.logger.config.LogServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.management.*;
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
 *     과도한 연결 오류 로그 방지를 위한 로그 억제 기능
 *     재시도 지수 백오프 전략으로 효율적인 재연결 시도
 * 모든 동작은 {@link LogServerProperties}에 정의된 속성 값을 기반으로 설정됩니다.
 *
 * @author eddy1219
 * @version 1.8.7
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

    // HTTP 요청 시 사용할 타임아웃 값 (밀리초)
    private static final int CONNECT_TIMEOUT = 5000; // 연결 타임아웃 (5초)
    private static final int SOCKET_TIMEOUT = 10000; // 데이터 수신 타임아웃 (10초)

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
     * 메모리 큐가 가득 찬 경우, 로그는 유실되고 경고 메시지가 기록됩니다.
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
            logger.warn("로그 큐가 가득 찼습니다 (용량: {}). 로그 메시지가 유실됩니다.", properties.getQueueCapacity());
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
            boolean isFiltered = filterSensitiveValuesRecursive(logMap, "");

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

    private boolean filterSensitiveValuesRecursive(Map<String, Object> map, String parentPath) {
        boolean isFiltered = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            // 키가 민감한 패턴과 일치하는지 확인 (대소문자 구분 없이)
            boolean isSensitive = false;
            for (Pattern pattern : sensitivePatterns) {
                if (pattern.matcher(currentPath.toLowerCase()).find()) {
                    isSensitive = true;
                    break;
                }
            }

            if (isSensitive && value != null) {
                // 민감한 값은 대체 문자열로 변경
                entry.setValue(properties.getSensitiveValueReplacement());
                isFiltered = true;
            } else if (value instanceof Map) {
                // Map인 경우 재귀적으로 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                if (filterSensitiveValuesRecursive(nestedMap, currentPath)) {
                    isFiltered = true;
                }
            } else if (value instanceof List) {
                // List인 경우 각 요소를 재귀적으로 처리
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) item;
                        if (filterSensitiveValuesRecursive(nestedMap, currentPath + "[" + i + "]")) {
                            isFiltered = true;
                        }
                    }
                }
            }
        }

        return isFiltered;
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

        // 배치를 JSON 배열 문자열로 변환
        String jsonBatch = convertBatchToJsonString(batch);
        if (jsonBatch == null) {
            return false; // 변환 실패
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
     * 로그 배치를 중앙 서버로 HTTP POST 요청을 통해 전송합니다.
     * 현재 서버 연결 상태에 따라 전송을 시도하거나 스킵합니다.
     *
     * @param jsonBatch 서버로 전송할 JSON 문자열 (로그 배치)
     * @return 전송 성공 여부 (true: 성공, false: 실패)
     * @throws IOException HTTP 요청 수행 중 I/O 오류 발생 시
     */
    private boolean executeSend(String jsonBatch) throws IOException {
        if (jsonBatch == null || jsonBatch.isEmpty()) {
            return false;
        }

        HttpPost httpPost = new HttpPost(properties.getLogServerUrl() + "/api/v1/logs/batch");
        httpPost.addHeader("API-Key", properties.getApiKey());
        httpPost.addHeader("Content-Type", "application/json");

        // GZIP 압축 적용
        if (properties.isGzipEnabled()) {
            byte[] compressedData = compressData(jsonBatch.getBytes(StandardCharsets.UTF_8));
            ByteArrayEntity entity = new ByteArrayEntity(compressedData);
            httpPost.setEntity(entity);
            httpPost.addHeader("Content-Encoding", "gzip");
            logger.debug("GZIP 압축 적용됨: 원본 크기={}바이트, 압축 후 크기={}바이트, 압축률={}% (기본 활성화 상태)",
                jsonBatch.getBytes(StandardCharsets.UTF_8).length, compressedData.length,
                Math.round((1 - (double)compressedData.length / jsonBatch.getBytes(StandardCharsets.UTF_8).length) * 100));
        } else {
            StringEntity entity = new StringEntity(jsonBatch, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            logger.debug("GZIP 압축 비활성화됨: 전송 크기={}바이트 (압축이 기본 활성화되므로 필요시 비활성화)", jsonBatch.getBytes(StandardCharsets.UTF_8).length);
        }

        // 요청 제한 시간 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        httpPost.setConfig(requestConfig);

        logger.debug("로그 전송 시도: URL={}, Headers={}", properties.getLogServerUrl(),
                Arrays.toString(httpPost.getAllHeaders()));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("서버 응답 상태 코드: {}", statusCode);

            // 성공 응답(2xx) 처리
            if (statusCode >= 200 && statusCode < 300) {
                // 연결 성공 시 상태 복원
                if (!isServerAvailable.get()) {
                    isServerAvailable.set(true);
                    logger.info("서버 연결이 복원되었습니다. URL: {}", properties.getLogServerUrl());
                }

                // 응답 본문 확인 (미리 지정된 경우만)
                String responseBody = null;
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.debug("서버 응답: {}", responseBody);
                }

                return true;
            }
            // 오류 응답(4xx, 5xx) 처리
            else {
                String responseBody = null;
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                logger.warn("로그 전송 실패: HTTP 상태 코드={}, 응답={}",
                        statusCode, responseBody != null ? responseBody : "응답 본문 없음");

                if (statusCode >= 500) {
                    // 서버 측 오류는 재시도 가능
                    return false;
                } else if (statusCode == 401 || statusCode == 403) {
                    // 인증 오류는 영구적 문제로 처리
                    handleServerUnavailable("인증 오류 (API 키 확인 필요)");
                    return false;
                } else if (statusCode == 413) {
                    // 페이로드 너무 큰 경우, 압축 적용 시도
                    if (!properties.isGzipEnabled()) {
                        logger.warn("요청 크기가 너무 큽니다. cholog.logger.gzip-enabled=true 설정을 고려하세요.");
                    }
                    return false;
                } else {
                    // 기타 4xx 오류는 재시도하지 않음
                    return false;
                }
            }
        } catch (Exception e) {
            // 연결 실패 등 예외 처리
            handleServerUnavailable("연결 오류: " + e.getMessage());
            logger.error("로그 전송 중 예외 발생: {}", e.getMessage());
            return false;
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

        try {
            // 디스크 큐 디렉토리 크기 확인 및 필요시 정리
                    long currentSize = calculateDirectorySize(diskQueueDir);
            long maxSizeBytes = properties.getMaxDiskQueueSizeBytes();

            // 최대 크기가 지정된 경우(0보다 큰 경우) 디렉토리 크기 체크
            if (maxSizeBytes > 0 && currentSize > maxSizeBytes) {
                cleanupOldestFiles(currentSize, maxSizeBytes);
            }

            // 고유한 파일명 생성 (타임스탬프-UUID.logbatch)
            String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + DISK_QUEUE_FILE_SUFFIX;
            Path filePath = diskQueueDir.resolve(fileName);

            // 파일에 JSON 배치 문자열 저장
            Files.write(
                    filePath,
                    jsonBatch.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            // 디스크 저장 관련 로그 출력 여부 제어
            if (properties.isVerboseDiskQueueLogs()) {
                logger.info("로그 배치를 디스크에 저장했습니다: {}", fileName);
            }
        } catch (Exception e) {
            logger.error("로그 배치를 디스크에 저장하는 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 디스크 큐 디렉토리의 현재 총 크기를 바이트 단위로 계산합니다.
     * 디렉토리 내의 파일들의 크기 합계를 반환합니다.
     * 심볼릭 링크나 하위 디렉토리는 계산에서 제외됩니다 (depth 1).
     *
     * @param directory 크기를 계산할 디렉토리 경로. null이 아니어야 합니다.
     * @return 디렉토리 내 파일들의 총 크기 (bytes).
     * @throws IOException 파일 시스템 접근 오류 시.
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        Objects.requireNonNull(directory, "Directory path cannot be null for size calculation.");
        long size = 0;
        try (Stream<Path> stream = Files.list(directory)) { // depth 1, 즉 해당 디렉토리의 직속 내용만
            for (Path path : stream.collect(Collectors.toList())) {
                if (Files.isRegularFile(path)) { // 파일인 경우에만 크기 계산
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
     *
     * @param currentSize 현재 디렉토리 크기 (바이트)
     * @param targetSize 정리 후 목표 크기 (바이트)
     * @throws IOException 파일 시스템 접근 오류 발생 시
     */
    private void cleanupOldestFiles(long currentSize, long targetSize) throws IOException {
        if (diskQueueDir == null) {
            return;
        }

        // 최대 용량 초과 시 오래된 파일부터 정리
        List<Path> files = Files.list(diskQueueDir)
                .filter(p -> p.toString().endsWith(DISK_QUEUE_FILE_SUFFIX))
                .sorted() // 이름 기준 오름차순 정렬 (타임스탬프가 파일명 앞부분이므로 오래된 파일 먼저)
                .collect(Collectors.toList());

        long sizeToFree = currentSize - targetSize;
        long freedSize = 0;
        int deletedCount = 0;

        for (Path file : files) {
            if (freedSize >= sizeToFree) {
                break; // 목표 크기에 도달하면 중단
            }

            try {
                long fileSize = Files.size(file);
                Files.delete(file);
                freedSize += fileSize;
                deletedCount++;
                logger.info("디스크 큐 용량 확보를 위해 오래된 로그 파일 삭제: {} ({} 바이트)",
                    file.getFileName(), fileSize);
            } catch (IOException e) {
                logger.warn("오래된 로그 파일 삭제 중 오류 발생: {}", e.getMessage());
                // 한 파일 삭제 실패해도 계속 진행
            }
        }

        if (deletedCount > 0) {
            logger.info("디스크 큐 정리 완료: {} 파일 삭제, {} 바이트 확보 (현재 사용량: {} → {} MB)",
                deletedCount, freedSize,
                currentSize / (1024 * 1024), (currentSize - freedSize) / (1024 * 1024));
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
        // 서비스가 활성 상태가 아니면 실행하지 않음
        if (!active.get()) {
            return;
        }

        try {
            // 1. GET 요청으로 서버 연결 확인
            HttpGet request = new HttpGet(properties.getUrl());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 500) { // 2xx, 3xx, 4xx는 서버가 동작 중임을 의미
                    if (!isServerAvailable.getAndSet(true)) {
                        logger.info("서버 연결이 복구되었습니다: {}", properties.getUrl());
                        currentBackoffDelay = properties.getInitialBackoffDelay(); // 백오프 지연 초기화
                    }
                } else {
                    handleServerUnavailable("서버가 비정상 응답 코드를 반환했습니다: " + statusCode);
                }
            }
        } catch (Exception e) {
            handleServerUnavailable("서버 연결 점검 중 오류 발생: " + e.getMessage());
        }
    }

    private void handleServerUnavailable(String reason) {
        // 서버를 사용할 수 없는 것으로 표시
        if (isServerAvailable.getAndSet(false)) {
            // 새롭게 서버가 사용 불가능 상태로 변경된 경우만 로그 출력
            if (!properties.isSuppressConnectionErrors()) {
                // v1.8.6: 재시도 전략에 따라 메시지 수정
                if (properties.isUseExponentialBackoff()) {
                    long delaySeconds = currentBackoffDelay / 1000;
                    logger.warn("서버 연결이 끊어졌습니다. {} 초 후 재시도합니다. 사유: {}",
                        delaySeconds, reason);
                } else {
                    logger.warn("서버 연결이 끊어졌습니다. 재시도 간격: {} 초. 사유: {}",
                        properties.getRetryDelay() / 1000, reason);
            }
            }
        }

        // 지수 백오프 적용이 활성화된 경우 지연 시간 증가
        if (properties.isUseExponentialBackoff()) {
            // 현재 지연에 지터(jitter) 추가 (±10%)
            int jitter = random.nextInt(21) - 10; // -10 ~ +10 랜덤값
            double jitterFactor = 1 + (jitter / 100.0); // 0.9 ~ 1.1

            // 지연 시간 두 배로 늘리기 (최대값 제한)
            currentBackoffDelay = Math.min(
                (long) (currentBackoffDelay * 2 * jitterFactor),
                properties.getMaxBackoffDelay()
            );
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
     * 가능하면 오류 디렉토리로 이동하고, 불가능하면 삭제합니다.
     *
     * @param file 손상된 파일 경로
     * @param errorDir 오류 파일 저장 디렉토리 (null일 수 있음)
     */
    private void handleCorruptedFile(Path file, Path errorDir) {
        try {
            if (errorDir != null && Files.exists(errorDir) && Files.isDirectory(errorDir)) {
                // 오류 디렉토리로 이동
                Path targetPath = errorDir.resolve(file.getFileName());
                Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.warn("손상된 로그 배치 파일을 오류 디렉토리로 이동: {} -> {}",
                    file.getFileName(), targetPath);
            } else {
                // 오류 디렉토리가 없으면 삭제
                Files.delete(file);
                logger.warn("손상된 로그 배치 파일 삭제: {}", file.getFileName());
            }
        } catch (IOException e) {
            logger.error("손상된 로그 배치 파일 처리 중 오류 발생: {}", e.getMessage());
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
        logger.info("Shutting down LogSenderService...");
        active.set(false); // 새로운 작업 중단 플래그 설정

        // 스케줄러 종료 시도
        shutdownExecutorService(scheduler, "Scheduler");

        // 메모리 큐에 남은 로그 마지막으로 처리 시도
        logger.info("Processing remaining logs in memory queue before final shutdown...");
        processBatchFromQueue(); // 마지막 배치 처리

        // HTTP 클라이언트 종료
        closeHttpClient();
        logger.info("LogSenderService shutdown complete.");
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
     * JMX를 통해 모니터링할 수 있는 메트릭을 등록합니다.
     * 로그 큐 상태, 디스크 큐 상태, 서버 연결 상태 등을 확인할 수 있습니다.
     */
    public void registerJmxMetrics() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // MBean 객체 이름 생성
            ObjectName objectName = new ObjectName("com.cholog.logger:type=LogSenderMetrics");

            // LogSenderService의 메트릭을 노출할 MBean 등록
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("QueueSize", logQueue.size());
            metrics.put("QueueCapacity", properties.getQueueCapacity());
            metrics.put("IsServerAvailable", isServerAvailable.get());
            metrics.put("DiskQueueEnabled", effectiveDiskQueueEnabled);

            if (effectiveDiskQueueEnabled) {
                metrics.put("DiskQueuePath", diskQueueDir.toAbsolutePath().toString());
                try {
                    metrics.put("DiskQueueSize", calculateDirectorySize(diskQueueDir));
                    metrics.put("DiskQueueFileCount",
                            Files.list(diskQueueDir).filter(p -> p.toString().endsWith(DISK_QUEUE_FILE_SUFFIX)).count());
                } catch (IOException e) {
                    metrics.put("DiskQueueSize", -1L);
                    metrics.put("DiskQueueFileCount", -1L);
                }
            }

            // MBean 등록
            if (mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
            }
            mBeanServer.registerMBean(new LogSenderMetricsMXBean(metrics), objectName);

            logger.info("JMX metrics registered for LogSenderService");
        } catch (Exception e) {
            logger.warn("Failed to register JMX metrics: {}", e.getMessage());
        }
    }

    /**
     * LogSenderService의 메트릭을 JMX에 노출하는 MXBean 구현 클래스
     */
    public static class LogSenderMetricsMXBean implements DynamicMBean {
        private final Map<String, Object> metrics;

        public LogSenderMetricsMXBean(Map<String, Object> metrics) {
            this.metrics = metrics;
        }

        @Override
        public Object getAttribute(String attribute) throws AttributeNotFoundException {
            if (!metrics.containsKey(attribute)) {
                throw new AttributeNotFoundException("Attribute " + attribute + " not found");
            }
            return metrics.get(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute) {
            // 읽기 전용 메트릭
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            AttributeList list = new AttributeList();
            for (String attribute : attributes) {
                try {
                    list.add(new Attribute(attribute, getAttribute(attribute)));
                } catch (AttributeNotFoundException e) {
                    // 무시
                }
            }
            return list;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return new AttributeList(); // 읽기 전용 메트릭
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) {
            return null; // 작업 미지원
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            MBeanAttributeInfo[] attrs = metrics.keySet().stream()
                    .map(key -> {
                        Object value = metrics.get(key);
                        String type = value != null ? value.getClass().getName() : "java.lang.String";
                        return new MBeanAttributeInfo(
                                key, type, key, true, false, false);
                    })
                    .toArray(MBeanAttributeInfo[]::new);

            return new MBeanInfo(
                    this.getClass().getName(),
                    "CHO:LOG Sender Metrics",
                    attrs,
                    null, // 생성자
                    null, // 작업
                    null  // 알림
            );
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
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteStream)) {
            gzipOutputStream.write(data);
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
        if (properties.getApiKey() != null && !properties.getApiKey().isEmpty()) {
            post.setHeader("X-API-Key", properties.getApiKey());
            post.setHeader("X-Service-Name", properties.getServiceName());
            post.setHeader("X-Environment", properties.getEnvironment());
            logger.debug("API Key 헤더가 요청에 추가되었습니다.");
        }

        // 압축 활성화 여부에 따라 다르게 처리
        if (properties.isGzipEnabled()) {
            // 압축 처리 로직
            byte[] originalData = jsonBatchArray.getBytes(StandardCharsets.UTF_8);
            byte[] compressedJson = compressData(originalData);

            // 압축된 데이터로 엔티티 생성
            ByteArrayEntity entity = new ByteArrayEntity(compressedJson);
            entity.setContentType("application/json");
            post.setEntity(entity);
            post.setHeader("Content-Encoding", "gzip");
        } else {
            // 압축 없이 일반 텍스트로 전송
            StringEntity entity = new StringEntity(jsonBatchArray, StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            post.setEntity(entity);
        }

        // 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        post.setConfig(requestConfig);

        // 최대 재시도 횟수만큼 시도
        int maxRetries = properties.getMaxRetries();
        Exception lastException = null;

        for (int retry = 1; retry <= maxRetries; retry++) {
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();

                // 성공적인 응답 코드 범위 (200~299)
                if (statusCode >= 200 && statusCode < 300) {
                    if (!isServerAvailable.getAndSet(true)) {
                        logger.info("서버 연결이 복구되었습니다: {}", properties.getUrl());
                    }
                    return true;
                } else {
                    // 응답이 성공이 아님: 300 이상의 상태 코드
                    // 서버가 응답했지만 성공 코드가 아님을 표시
                    isServerAvailable.set(true);

                    // v1.8.6: 동일한 상태 코드에 대한 반복 로그를 줄이기
                    // 404 상태 코드는 서버가 존재하지 않거나 엔드포인트가 없는 경우로 특별 처리
                    if (retry == maxRetries) {
                        // 404 상태 코드는 서버가 존재하지 않거나 엔드포인트가 없는 경우로 특별 처리
                        if (statusCode == 404) {
                            // 404 오류는 일정 시간 간격으로만 로깅 (반복 로그 감소)
                            long now = System.currentTimeMillis();
                            long lastLogTime = lastErrorLogTime.get();
                            // 마지막 로그로부터 일정 시간이 지났을 때만 경고 로그 출력
                            if (now - lastLogTime > properties.getConnectionErrorLogPeriod()) {
                                logger.warn("로그 서버 경로가 존재하지 않습니다(404). URL 설정을 확인하세요: {}", properties.getUrl());
                                lastErrorLogTime.set(now);
                            } else {
                                // 간격 내에는 디버그 레벨로만 로깅
                                logger.debug("디스크 배치 전송 실패. 서버 응답 상태 코드: {} (재시도 {}/{})",
                                    statusCode, retry, maxRetries);
                            }
                        } else {
                            logger.warn("디스크 배치 전송 실패. 서버 응답 상태 코드: {} (재시도 {}/{})",
                                statusCode, retry, maxRetries);
                        }
                    }

                    // 서버 인증 오류 (API 키 관련) 특별 처리
                    if (statusCode == 401 || statusCode == 403) {
                        logger.error("인증 오류 (상태 코드: {}). API 키 설정을 확인하세요.", statusCode);
                        return false; // 인증 오류는 더 이상 시도하지 않음
                    }
                }
            } catch (Exception e) {
                lastException = e;

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
                        long delayMs = properties.isUseExponentialBackoff() ?
                                Math.min(properties.getInitialBackoffDelay() * (1 << (retry - 1)), properties.getMaxBackoffDelay()) :
                                properties.getRetryDelay();
                        Thread.sleep(delayMs);
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
