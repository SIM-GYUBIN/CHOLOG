package com.example.logserver.controller;

import com.example.logserver.dto.LogFilterRequest;
import com.example.logserver.model.LogEntry;
import com.example.logserver.service.FilterService;
import com.example.logserver.service.LogService;
import com.example.logserver.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 로그 컨트롤러
 * 로그 관련 HTTP 요청 처리를 담당
 */
@Controller
public class LogController {

    private final LogService logService;
    private final FilterService filterService;
    private final StatisticsService statisticsService;

    @Autowired
    public LogController(LogService logService, FilterService filterService, StatisticsService statisticsService) {
        this.logService = logService;
        this.filterService = filterService;
        this.statisticsService = statisticsService;
    }

    /**
     * 로그 수신 및 처리 API
     * 
     * @param logJson 로그 JSON 문자열
     * @return 처리 결과
     */
    @PostMapping(value = "/logs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String receiveLogs(@RequestBody String logJson) {
        return logService.receiveLogs(logJson);
    }

    /**
     * 필터링된 로그 조회 API
     * 
     * @param apiKey API 키
     * @param path 경로
     * @param level 로그 레벨
     * @param status 상태 코드
     * @param httpStatus 상태 코드 별칭
     * @param statusStr 상태 코드 별칭
     * @param service 서비스 이름
     * @param serviceName 서비스 이름 별칭
     * @param query 검색 쿼리
     * @param platform 플랫폼
     * @param isMobile 모바일 여부
     * @return 필터링된 로그 목록
     */
    @GetMapping("/logs")
    @ResponseBody
    public List<Map<String, Object>> getLogs(
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String level,
            // 여러 파라미터 이름 지원 ('status', 'httpStatus', 'statusStr')
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "httpStatus", required = false) String httpStatus,
            @RequestParam(value = "statusStr", required = false) String statusStr,
            // 서비스 이름 ('service', 'serviceName')
            @RequestParam(value = "service", required = false) String service,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Boolean isMobile) {

        // 상태 코드 파라미터 처리
        String finalStatusStr = status != null ? status :
                (httpStatus != null ? httpStatus : statusStr);

        // 서비스 이름 파라미터 처리
        String finalService = service != null ? service : serviceName;

        // 로그 목록 가져오기
        List<LogEntry> logs = logService.getFilteredLogs(apiKey, path, level, finalStatusStr, finalService, query, platform, isMobile);
        
        // LogEntry 목록을 Map으로 변환하되 null 값 제외
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (LogEntry log : logs) {
            try {
                String json = mapper.writeValueAsString(log);
                Map<String, Object> logMap = mapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                result.add(logMap);
            } catch (Exception e) {
                // 에러 발생 시 처리
                continue;
            }
        }
        
        return result;
    }

    /**
     * 필터 옵션 조회 API
     * 
     * @return 필터 옵션 맵
     */
    @GetMapping("/filters")
    @ResponseBody
    public Map<String, Object> getFilterOptions() {
        return filterService.getFilterOptions();
    }

    /**
     * 전체 로그 상세 정보 조회 API
     * 
     * @param filter 필터 요청 객체
     * @return 로그 상세 정보 목록
     */
    @GetMapping("/logs/full")
    @ResponseBody
    public ResponseEntity<String> getFullLogs(LogFilterRequest filter) {
        // 서비스에서 로그 데이터 가져오기
        List<Map<String, Object>> logs = logService.getFullLogs(
                filter.getApiKey(),
                filter.getPath(),
                filter.getLevel(),
                filter.getFinalStatus(),
                filter.getFinalService(),
                filter.getQuery(),
                filter.getPlatform(),
                filter.getIsMobile()
        );
        
        try {
            // null 값을 포함하도록 ObjectMapper 설정
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            
            // 직접 JSON 문자열 생성
            String jsonResponse = mapper.writeValueAsString(logs);
            
            // JSON 문자열을 직접 반환
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 로그 상세 정보 조회 API
     * 
     * @param index 로그 인덱스
     * @return 로그 상세 정보
     */
    @GetMapping("/logs/detail/{index}")
    @ResponseBody
    public Map<String, Object> getLogDetail(@PathVariable int index) {
        return logService.getLogDetail(index);
    }

    /**
     * 메인 페이지 반환
     * 
     * @return 뷰 이름
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 로그 통계 조회 API
     * 
     * @param apiKey API 키
     * @param serviceName 서비스 이름
     * @param timeRange 시간 범위
     * @return 통계 정보
     */
    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getLogStatistics(
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String timeRange) {
    
        return statisticsService.getLogStatistics(apiKey, serviceName, timeRange);
    }

    /**
     * 차트 데이터 조회 API
     * 
     * @param apiKey API 키
     * @param serviceName 서비스 이름
     * @param period 기간
     * @return 차트 데이터
     */
    @GetMapping("/stats/charts")
    @ResponseBody
    public Map<String, Object> getChartData(
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String period) {
    
        return statisticsService.getChartData(apiKey, serviceName, period);
    }

    /**
     * 특정 API 키에 대한 필터링된 로그 조회 API
     * 
     * @param apiKey API 키 (경로 변수)
     * @param path 경로
     * @param level 로그 레벨
     * @param status 상태 코드
     * @param httpStatus 상태 코드 별칭
     * @param statusStr 상태 코드 별칭
     * @param service 서비스 이름
     * @param serviceName 서비스 이름 별칭
     * @param query 검색 쿼리
     * @param platform 플랫폼
     * @param isMobile 모바일 여부
     * @return 필터링된 로그 목록
     */
    @GetMapping("/logs/{apiKey}")
    @ResponseBody
    public List<Map<String, Object>> getLogsByApiKey(
            @PathVariable String apiKey,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String level,
            // 여러 파라미터 이름 지원 ('status', 'httpStatus', 'statusStr')
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "httpStatus", required = false) String httpStatus,
            @RequestParam(value = "statusStr", required = false) String statusStr,
            // 서비스 이름 ('service', 'serviceName')
            @RequestParam(value = "service", required = false) String service,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Boolean isMobile) {

        // 상태 코드 파라미터 처리
        String finalStatusStr = status != null ? status :
                (httpStatus != null ? httpStatus : statusStr);

        // 서비스 이름 파라미터 처리
        String finalService = service != null ? service : serviceName;

        // 로그 목록 가져오기
        List<LogEntry> logs = logService.getFilteredLogs(apiKey, path, level, finalStatusStr, finalService, query, platform, isMobile);
        
        // LogEntry 목록을 Map으로 변환하되 null 값 제외
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (LogEntry log : logs) {
            try {
                String json = mapper.writeValueAsString(log);
                Map<String, Object> logMap = mapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                result.add(logMap);
            } catch (Exception e) {
                // 에러 발생 시 처리
                continue;
            }
        }
        
        return result;
    }

    /**
     * 특정 API 키에 대한 전체 로그 상세 정보 조회 API
     * 
     * @param apiKey API 키 (경로 변수)
     * @param filter 필터 요청 객체
     * @return 로그 상세 정보 목록
     */
    @GetMapping("/logs/full/{apiKey}")
    @ResponseBody
    public ResponseEntity<String> getFullLogsByApiKey(
            @PathVariable String apiKey,
            LogFilterRequest filter) {
        
        // 경로 변수의 apiKey를 필터 객체에 설정
        if (filter == null) {
            filter = new LogFilterRequest();
        }
        filter.setApiKey(apiKey);
        
        // 서비스에서 로그 데이터 가져오기
        List<Map<String, Object>> logs = logService.getFullLogs(
                filter.getApiKey(),
                filter.getPath(),
                filter.getLevel(),
                filter.getFinalStatus(),
                filter.getFinalService(),
                filter.getQuery(),
                filter.getPlatform(),
                filter.getIsMobile()
        );
        
        try {
            // null 값을 포함하도록 ObjectMapper 설정
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            
            // 직접 JSON 문자열 생성
            String jsonResponse = mapper.writeValueAsString(logs);
            
            // JSON 문자열을 직접 반환
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 특정 API 키와 서비스에 대한 필터링된 로그 조회 API
     * 
     * @param apiKey API 키 (경로 변수)
     * @param serviceName 서비스 이름 (경로 변수)
     * @param path 경로
     * @param level 로그 레벨
     * @param status 상태 코드
     * @param httpStatus 상태 코드 별칭
     * @param statusStr 상태 코드 별칭
     * @param query 검색 쿼리
     * @param platform 플랫폼
     * @param isMobile 모바일 여부
     * @return 필터링된 로그 목록
     */
    @GetMapping("/logs/{apiKey}/{serviceName}")
    @ResponseBody
    public List<Map<String, Object>> getLogsByApiKeyAndServiceName(
            @PathVariable String apiKey,
            @PathVariable String serviceName,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String level,
            // 여러 파라미터 이름 지원 ('status', 'httpStatus', 'statusStr')
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "httpStatus", required = false) String httpStatus,
            @RequestParam(value = "statusStr", required = false) String statusStr,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Boolean isMobile) {

        // 상태 코드 파라미터 처리
        String finalStatusStr = status != null ? status :
                (httpStatus != null ? httpStatus : statusStr);

        // 로그 목록 가져오기
        List<LogEntry> logs = logService.getFilteredLogs(apiKey, path, level, finalStatusStr, serviceName, query, platform, isMobile);
        
        // LogEntry 목록을 Map으로 변환하되 null 값 제외
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (LogEntry log : logs) {
            try {
                String json = mapper.writeValueAsString(log);
                Map<String, Object> logMap = mapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                result.add(logMap);
            } catch (Exception e) {
                // 에러 발생 시 처리
                continue;
            }
        }
        
        return result;
    }

    /**
     * 특정 API 키와 서비스에 대한 전체 로그 상세 정보 조회 API
     * 
     * @param apiKey API 키 (경로 변수)
     * @param serviceName 서비스 이름 (경로 변수)
     * @param filter 필터 요청 객체
     * @return 로그 상세 정보 목록
     */
    @GetMapping("/logs/full/{apiKey}/{serviceName}")
    @ResponseBody
    public ResponseEntity<String> getFullLogsByApiKeyAndServiceName(
            @PathVariable String apiKey,
            @PathVariable String serviceName,
            LogFilterRequest filter) {
        
        // 경로 변수의 apiKey와 serviceName을 필터 객체에 설정
        if (filter == null) {
            filter = new LogFilterRequest();
        }
        filter.setApiKey(apiKey);
        filter.setServiceName(serviceName);
        
        // 서비스에서 로그 데이터 가져오기
        List<Map<String, Object>> logs = logService.getFullLogs(
                filter.getApiKey(),
                filter.getPath(),
                filter.getLevel(),
                filter.getFinalStatus(),
                filter.getFinalService(),
                filter.getQuery(),
                filter.getPlatform(),
                filter.getIsMobile()
        );
        
        try {
            // null 값을 포함하도록 ObjectMapper 설정
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            
            // 직접 JSON 문자열 생성
            String jsonResponse = mapper.writeValueAsString(logs);
            
            // JSON 문자열을 직접 반환
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}