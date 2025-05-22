package com.cholog.logger.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * LogSenderMetricsMBean 인터페이스를 구현하는 클래스입니다.
 * 로그 전송 서비스의 성능 지표와 상태 정보를 수집하고 JMX를 통해 노출합니다.
 */
public class LogSenderMetrics implements LogSenderMetricsMBean {
    
    private final BlockingQueue<String> logQueue;
    private final AtomicBoolean isServerAvailable;
    private final Path diskQueueDir;
    private final long startTimeMillis;
    
    private final AtomicLong totalProcessedLogs = new AtomicLong(0);
    private final AtomicLong totalFailedLogs = new AtomicLong(0);
    
    // 디스크 메트릭 캐싱을 위한 필드들
    private final AtomicInteger diskQueueFileCount = new AtomicInteger(0);
    private final AtomicLong diskQueueSizeBytes = new AtomicLong(0);
    private final AtomicLong lastDiskMetricsUpdateTime = new AtomicLong(0);
    
    // 캐시 업데이트 주기 (5초)
    private static final long DISK_METRICS_CACHE_TTL = 5000;
    
    // 동시성 제어를 위한 읽기/쓰기 락
    private final ReadWriteLock diskMetricsLock = new ReentrantReadWriteLock();
    private final Lock diskMetricsReadLock = diskMetricsLock.readLock();
    private final Lock diskMetricsWriteLock = diskMetricsLock.writeLock();
    
    /**
     * LogSenderMetrics의 새 인스턴스를 생성합니다.
     * 
     * @param logQueue 로그 메시지 큐
     * @param isServerAvailable 서버 연결 상태
     * @param diskQueueDir 디스크 큐 디렉토리 경로
     */
    public LogSenderMetrics(BlockingQueue<String> logQueue, AtomicBoolean isServerAvailable, Path diskQueueDir) {
        this.logQueue = logQueue;
        this.isServerAvailable = isServerAvailable;
        this.diskQueueDir = diskQueueDir;
        this.startTimeMillis = System.currentTimeMillis();
        
        // 초기 디스크 메트릭 업데이트
        updateDiskMetrics();
    }
    
    /**
     * 로그가 성공적으로 처리되었을 때 호출하여 카운터를 증가시킵니다.
     * 
     * @param count 처리된 로그 수
     */
    public void incrementProcessedLogs(int count) {
        if (count > 0) {
            totalProcessedLogs.addAndGet(count);
        }
    }
    
    /**
     * 로그 처리에 실패했을 때 호출하여 카운터를 증가시킵니다.
     * 
     * @param count 실패한 로그 수
     */
    public void incrementFailedLogs(int count) {
        if (count > 0) {
            totalFailedLogs.addAndGet(count);
        }
    }
    
    /**
     * 디스크 큐 메트릭을 업데이트합니다.
     * 이 메서드는 정기적으로 호출되어 캐시된 값을 갱신합니다.
     * 대용량 디렉토리 처리를 최적화하여 성능 문제를 방지합니다.
     */
    public void updateDiskMetrics() {
        // 디스크 큐가 사용 불가능하면 업데이트하지 않음
        if (diskQueueDir == null || !Files.exists(diskQueueDir)) {
            diskMetricsWriteLock.lock();
            try {
                diskQueueFileCount.set(0);
                diskQueueSizeBytes.set(0);
                lastDiskMetricsUpdateTime.set(System.currentTimeMillis());
            } finally {
                diskMetricsWriteLock.unlock();
            }
            return;
        }
        
        // 락 획득 전에 시간 기록 (락 획득 대기 시간 제외)
        long updateStartTime = System.currentTimeMillis();
        
        // 업데이트 시작 시간이 너무 오래되었으면(최대 30초) 강제로 업데이트
        boolean forceUpdate = (updateStartTime - lastDiskMetricsUpdateTime.get() > 30000);
        
        // 이미 최근에 업데이트되었고 강제 업데이트가 아니면 건너뜀
        if (!forceUpdate && updateStartTime - lastDiskMetricsUpdateTime.get() < DISK_METRICS_CACHE_TTL) {
            return;
        }
        
        diskMetricsWriteLock.lock();
        try {
            // 로그 배치 파일 확장자
            final String LOG_BATCH_FILE_SUFFIX = ".logbatch";
            
            // 연산 중간값을 저장할 변수
            int fileCount = 0;
            long totalSize = 0;
            
            // 파일 카운팅 제한 (최대 1000개 파일만 계산)
            final int MAX_FILES_TO_PROCESS = 1000;
            
            try (Stream<Path> files = Files.list(diskQueueDir)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    // 로그 배치 파일만 처리
                    if (Files.isRegularFile(file) && file.toString().endsWith(LOG_BATCH_FILE_SUFFIX)) {
                        fileCount++;
                        try {
                            totalSize += Files.size(file);
                        } catch (IOException e) {
                            // 개별 파일 크기 조회 실패 시 무시하고 계속 진행
                        }
                        
                        // 처리 파일 수 제한 (대용량 디렉토리 성능 저하 방지)
                        if (fileCount >= MAX_FILES_TO_PROCESS) {
                            // 로그 출력 - 데이터가 너무 많아 정확한 측정 불가
                            System.out.println("[LogSenderMetrics] 디스크 큐 파일이 너무 많습니다. 정확한 카운트를 위해 디스크 큐 정리가 필요합니다.");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                // 디렉토리 접근 오류 시 이전 값 유지
                return;
            }
            
            // 모든 연산이 성공한 경우에만 값 업데이트
            diskQueueFileCount.set(fileCount);
            diskQueueSizeBytes.set(totalSize);
            lastDiskMetricsUpdateTime.set(updateStartTime);
        } finally {
            diskMetricsWriteLock.unlock();
        }
    }
    
    @Override
    public int getQueueSize() {
        return logQueue.size();
    }
    
    @Override
    public long getTotalProcessedLogs() {
        return totalProcessedLogs.get();
    }
    
    @Override
    public long getTotalFailedLogs() {
        return totalFailedLogs.get();
    }
    
    @Override
    public boolean isServerConnected() {
        return isServerAvailable.get();
    }
    
    @Override
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }
    
    @Override
    public double getLogsPerSecond() {
        long uptime = getUptimeSeconds();
        return uptime > 0 ? (double) totalProcessedLogs.get() / uptime : 0;
    }
    
    @Override
    public int getDiskQueueFileCount() {
        checkAndUpdateDiskMetricsIfNeeded();
        
        diskMetricsReadLock.lock();
        try {
            return diskQueueFileCount.get();
        } finally {
            diskMetricsReadLock.unlock();
        }
    }
    
    @Override
    public long getDiskQueueSizeBytes() {
        checkAndUpdateDiskMetricsIfNeeded();
        
        diskMetricsReadLock.lock();
        try {
            return diskQueueSizeBytes.get();
        } finally {
            diskMetricsReadLock.unlock();
        }
    }
    
    /**
     * 필요한 경우 디스크 메트릭을 업데이트합니다.
     * 캐시 TTL이 경과한 경우에만 업데이트합니다.
     * 업데이트 요청이 중복되지 않도록 원자적 연산을 사용합니다.
     */
    private void checkAndUpdateDiskMetricsIfNeeded() {
        long now = System.currentTimeMillis();
        long lastUpdate = lastDiskMetricsUpdateTime.get();
        
        // TTL 경과 시 업데이트 필요
        if (now - lastUpdate > DISK_METRICS_CACHE_TTL) {
            // 다른 스레드가 이미 업데이트 중일 수 있으므로 CAS로 확인
            // 업데이트 표시로 현재 시간을 사용 (Long.MAX_VALUE 대신)
            if (lastDiskMetricsUpdateTime.compareAndSet(lastUpdate, now)) {
                try {
                    updateDiskMetrics();
                } catch (Exception e) {
                    // 예외 발생 시 원래 타임스탬프로 복원하여 다음 호출에서 재시도하도록 함
                    lastDiskMetricsUpdateTime.set(lastUpdate);
                }
            }
        }
    }
    
    /**
     * 모든 메트릭 값을 문자열로 반환합니다.
     * 이 메서드는 디버깅 및 로깅 목적으로 유용합니다.
     * 
     * @return 메트릭 값들을 포함하는 문자열
     */
    @Override
    public String toString() {
        return String.format(
            "LogMetrics[queue=%d, processed=%d, failed=%d, serverConnected=%b, diskFiles=%d, diskSize=%d bytes]",
            getQueueSize(),
            getTotalProcessedLogs(),
            getTotalFailedLogs(),
            isServerConnected(),
            getDiskQueueFileCount(),
            getDiskQueueSizeBytes()
        );
    }
} 