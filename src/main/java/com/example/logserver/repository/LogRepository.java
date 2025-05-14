package com.example.logserver.repository;

import com.example.logserver.model.LogEntry;
import com.example.logserver.util.DateTimeUtil;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 로그 데이터 저장소
 * 로그 엔트리를 관리하고 필터링 기능을 제공
 */
@Repository
public class LogRepository {

    private final List<LogEntry> logEntries = Collections.synchronizedList(new LinkedList<>());
    private final int MAX_LOGS = 1000; // 최대 로그 저장 개수

    /**
     * 로그 추가
     *
     * @param entry 추가할 로그 엔트리
     */
    public void addLog(LogEntry entry) {
        synchronized (logEntries) {
            // 최신 로그를 리스트 앞쪽에 추가
            logEntries.add(0, entry);

            // 로그 수 제한
            if (logEntries.size() > MAX_LOGS) {
                logEntries.remove(logEntries.size() - 1);
            }
        }
    }

    /**
     * 여러 로그 추가
     *
     * @param entries 추가할 로그 엔트리 목록
     */
    public void addLogs(List<LogEntry> entries) {
        for (LogEntry entry : entries) {
            addLog(entry);
        }
    }

    /**
     * 모든 로그 조회
     *
     * @return 모든 로그 목록
     */
    public List<LogEntry> getAllLogs() {
        synchronized (logEntries) {
            return new ArrayList<>(logEntries);
        }
    }

    /**
     * 필터링된 로그 조회
     *
     * @param apiKey API 키
     * @param path 경로
     * @param level 로그 레벨
     * @param status HTTP 상태 코드
     * @param service 서비스 이름
     * @param query 검색 쿼리
     * @param platform 플랫폼
     * @param isMobile 모바일 여부
     * @return 필터링된 로그 목록
     */
    public List<LogEntry> getFilteredLogs(String apiKey, String path, String level, 
                                         Integer status, String service, 
                                         String query, String platform, Boolean isMobile) {
        synchronized (logEntries) {
            return logEntries.stream()
                    .filter(log -> apiKey == null || (log.getApiKey() != null && log.getApiKey().equals(apiKey)))
                    .filter(log -> path == null || (log.getRequestUri() != null && log.getRequestUri().contains(path)))
                    .filter(log -> level == null || (log.getLevel() != null && log.getLevel().equals(level)))
                    .filter(log -> status == null || (log.getHttpStatus() != null && log.getHttpStatus().equals(status)))
                    .filter(log -> service == null || (log.getServiceName() != null && log.getServiceName().equals(service)))
                    .filter(log -> query == null || (log.getMessage() != null && log.getMessage().toLowerCase().contains(query.toLowerCase())))
                    .filter(log -> platform == null || (log.getUaPlatform() != null && log.getUaPlatform().equalsIgnoreCase(platform)))
                    .filter(log -> isMobile == null || (log.getUaMobile() != null && log.getUaMobile().equals(isMobile)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 시간 범위로 필터링된 로그 조회
     *
     * @param apiKey API 키
     * @param serviceName 서비스 이름
     * @param timeRange 시간 범위
     * @return 필터링된 로그 목록
     */
    public List<LogEntry> getFilteredLogsByTime(String apiKey, String serviceName, String timeRange) {
        // 시간 범위에 따른 필터링
        LocalDateTime startTime = null;
        if (timeRange != null) {
            LocalDateTime now = LocalDateTime.now();
            switch (timeRange) {
                case "1h":
                    startTime = now.minusHours(1);
                    break;
                case "6h":
                    startTime = now.minusHours(6);
                    break;
                case "24h":
                    startTime = now.minusDays(1);
                    break;
                case "7d":
                    startTime = now.minusDays(7);
                    break;
                case "30d":
                    startTime = now.minusDays(30);
                    break;
            }
        }

        final LocalDateTime finalStartTime = startTime;

        synchronized (logEntries) {
            return logEntries.stream()
                    .filter(log -> apiKey == null || (log.getApiKey() != null && log.getApiKey().equals(apiKey)))
                    .filter(log -> serviceName == null || (log.getServiceName() != null && log.getServiceName().equals(serviceName)))
                    .filter(log -> finalStartTime == null || (log.getTimestamp() != null && DateTimeUtil.isAfter(log.getTimestamp(), finalStartTime)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 로그 상세 정보 조회
     *
     * @param index 로그 인덱스
     * @return 해당 인덱스의 로그 엔트리
     */
    public LogEntry getLogDetail(int index) {
        synchronized (logEntries) {
            if (index < 0 || index >= logEntries.size()) {
                return null;
            }
            return logEntries.get(index);
        }
    }
} 