package com.example.logserver.service;

import com.example.logserver.model.LogEntry;
import com.example.logserver.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 필터 옵션 관련 서비스
 * 로그 데이터의 필터 옵션을 제공
 */
@Service
public class FilterService {

    private final LogRepository logRepository;

    @Autowired
    public FilterService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 모든 필터 옵션 조회
     *
     * @return 필터 옵션 맵
     */
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> filters = new HashMap<>();
        List<LogEntry> logEntries = logRepository.getAllLogs();

        // API 키(apiKey) 목록
        filters.put("apiKeys", logEntries.stream()
                .map(LogEntry::getApiKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        // 로그 레벨 목록
        filters.put("levels", logEntries.stream()
                .map(LogEntry::getLevel)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        // 서비스명 목록
        filters.put("services", logEntries.stream()
                .map(LogEntry::getServiceName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        // 경로(URI) 목록
        filters.put("paths", logEntries.stream()
                .map(LogEntry::getRequestUri)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        // HTTP 상태 코드 목록
        filters.put("statusCodes", logEntries.stream()
                .map(LogEntry::getHttpStatus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted() // 상태 코드 정렬
                .collect(Collectors.toList()));

        // 플랫폼 목록 추가
        filters.put("platforms", logEntries.stream()
                .map(LogEntry::getUaPlatform)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        // 모바일 여부 필터 추가
        filters.put("mobileOptions", Arrays.asList(true, false));

        return filters;
    }
} 