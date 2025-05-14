package com.example.logserver.dto;

import lombok.Data;

/**
 * 로그 필터링 요청을 위한 DTO 클래스
 */
@Data
public class LogFilterRequest {
    private String apiKey;
    private String path;
    private String level;
    private String status;
    private String httpStatus;
    private String statusStr;
    private String service;
    private String serviceName;
    private String query;
    private String platform;
    private Boolean isMobile;
    private String timeRange;
    
    /**
     * 여러 변수명 중 하나를 선택하여 상태 코드 값을 반환
     * 
     * @return 최종 상태 코드 문자열
     */
    public String getFinalStatus() {
        if (status != null) return status;
        if (httpStatus != null) return httpStatus;
        return statusStr;
    }
    
    /**
     * 여러 변수명 중 하나를 선택하여 서비스 이름 값을 반환
     * 
     * @return 최종 서비스 이름
     */
    public String getFinalService() {
        if (service != null) return service;
        return serviceName;
    }
} 