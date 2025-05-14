package com.example.logserver.service;

import com.example.logserver.model.LogEntry;
import com.example.logserver.repository.LogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 로그 처리 서비스
 * 로그 데이터의 수신, 조회, 필터링 등을 담당
 */
@Service
public class LogService {

    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper objectMapperWithNulls;

    @Autowired
    public LogService(LogRepository logRepository, 
                     ObjectMapper objectMapper,
                     @Qualifier("objectMapperWithNulls") ObjectMapper objectMapperWithNulls) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.objectMapperWithNulls = objectMapperWithNulls;
    }

    /**
     * 로그 수신 및 처리
     *
     * @param logJson 로그 JSON 문자열
     * @return 처리 결과 메시지
     */
    public String receiveLogs(String logJson) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<LogEntry> entries;

            // 배열인지 확인
            if (logJson.trim().startsWith("[")) {
                // 배열을 LogEntry 리스트로 변환
                CollectionType listType = objectMapper.getTypeFactory()
                        .constructCollectionType(ArrayList.class, LogEntry.class);
                entries = objectMapper.readValue(logJson, listType);
            } else {
                // 단일 객체를 LogEntry로 변환
                LogEntry entry = objectMapper.readValue(logJson, LogEntry.class);
                entries = Collections.singletonList(entry);
            }

            // 로그 처리 로직
            for (LogEntry entry : entries) {
                if (entry.getTimestamp() == null) {
                    entry.setTimestamp(now.toString());
                }

                // 빈 필드나 null 제거
                entry.optimizeFields();

                // 로그 저장소에 추가
                logRepository.addLog(entry);
            }

            return "Logs received: " + entries.size();

        } catch (JsonProcessingException e) {
            return "Error parsing log data: " + e.getMessage();
        } catch (Exception e) {
            return "Error processing logs: " + e.getMessage();
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
                                          String status, String service, 
                                          String query, String platform, Boolean isMobile) {
        Integer parsedStatus = null;
        try {
            if (status != null) parsedStatus = Integer.parseInt(status);
        } catch (NumberFormatException e) {
            // invalid status filter
        }

        return logRepository.getFilteredLogs(apiKey, path, level, parsedStatus, service, query, platform, isMobile);
    }

    /**
     * 전체 로그 상세 정보 조회
     *
     * @param apiKey API 키
     * @param path 경로
     * @param level 로그 레벨
     * @param status HTTP 상태 코드
     * @param service 서비스 이름
     * @param query 검색 쿼리
     * @param platform 플랫폼
     * @param isMobile 모바일 여부
     * @return 로그 상세 정보 목록
     */
    public List<Map<String, Object>> getFullLogs(String apiKey, String path, String level, 
                                                String status, String service, 
                                                String query, String platform, Boolean isMobile) {
        List<LogEntry> filteredLogs = getFilteredLogs(apiKey, path, level, status, service, query, platform, isMobile);
        
        // LogEntry 객체를 Map으로 변환 (null 값을 포함한 모든 필드)
        return convertLogsToMaps(filteredLogs);
    }

    /**
     * 로그 목록을 Map으로 변환
     *
     * @param logs 로그 목록
     * @return Map으로 변환된 로그 목록
     */
    private List<Map<String, Object>> convertLogsToMaps(List<LogEntry> logs) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (LogEntry log : logs) {
            try {
                // 모든 필드를 포함하는 맵 생성
                Map<String, Object> logMap = new HashMap<>();
                
                // 기본 로그 정보
                logMap.put("level", log.getLevel());
                logMap.put("message", log.getMessage());
                logMap.put("timestamp", log.getTimestamp());
                logMap.put("logger", log.getLogger());
                logMap.put("thread", log.getThread());
                logMap.put("sequence", log.getSequence());
                
                // 애플리케이션/서버 정보
                logMap.put("serviceName", log.getServiceName());
                logMap.put("environment", log.getEnvironment());
                logMap.put("profiles", log.getProfiles());
                logMap.put("version", log.getVersion());
                logMap.put("hostName", log.getHostName());
                logMap.put("apiKey", log.getApiKey());
                
                // 요청 정보
                logMap.put("requestId", log.getRequestId());
                logMap.put("requestMethod", log.getRequestMethod());
                logMap.put("requestUri", log.getRequestUri());
                logMap.put("clientIp", log.getClientIp());
                logMap.put("userAgent", log.getUserAgent());
                logMap.put("httpStatus", log.getHttpStatus());
                
                // 브라우저 정보
                logMap.put("uaMobile", log.getUaMobile());
                logMap.put("uaPlatform", log.getUaPlatform());
                
                // 구조화된 객체들
                logMap.put("performanceMetrics", log.getPerformanceMetrics());
                logMap.put("mdcContext", log.getMdcContext());
                logMap.put("headers", log.getHeaders());
                logMap.put("throwable", log.getThrowable());
                
                // 요청/응답 정보
                logMap.put("requestParams", log.getRequestParams());
                logMap.put("responseHeaders", log.getResponseHeaders());
                
                // 기타 정보
                logMap.put("filtered", log.getFiltered());
                
                result.add(logMap);
            } catch (Exception e) {
                Map<String, Object> errorMap = Collections.singletonMap("error", "Failed to convert log entry: " + e.getMessage());
                result.add(errorMap);
            }
        }
        
        return result;
    }

    /**
     * 로그 상세 정보 조회
     *
     * @param index 로그 인덱스
     * @return 로그 상세 정보
     */
    public Map<String, Object> getLogDetail(int index) {
        LogEntry log = logRepository.getLogDetail(index);
        
        if (log == null) {
            return Collections.singletonMap("error", "Log entry index out of bounds");
        }
        
        try {
            // 모든 필드를 포함한 매핑 생성
            String json = objectMapperWithNulls.writeValueAsString(log);
            Map<String, Object> logMap = objectMapperWithNulls.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            // 원본 JSON도 포함
            logMap.put("rawJson", json);

            return logMap;
        } catch (Exception e) {
            return Collections.singletonMap("error", "Failed to get log detail: " + e.getMessage());
        }
    }
} 