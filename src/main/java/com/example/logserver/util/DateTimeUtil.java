package com.example.logserver.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 및 시간 처리를 위한 유틸리티 클래스
 */
public class DateTimeUtil {

    /**
     * 타임스탬프 문자열을 LocalDateTime으로 파싱
     *
     * @param timestamp 타임스탬프 문자열
     * @return 파싱된 LocalDateTime 객체, 파싱 실패 시 현재 시간 반환
     */
    public static LocalDateTime parseTimestamp(String timestamp) {
        try {
            // ISO 형식 (2025-05-09T08:29:00.766Z)
            if (timestamp.contains("T") && (timestamp.contains("Z") || timestamp.contains("+"))) {
                if (timestamp.endsWith("Z")) {
                    // UTC 시간을 그대로 유지 (Z 제거)
                    return LocalDateTime.parse(timestamp.substring(0, timestamp.length() - 1), 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else {
                    return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            }
            
            // 다른 형식들 처리
            DateTimeFormatter formatter;
            if (timestamp.contains("-")) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            } else if (timestamp.contains("/")) {
                formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            } else {
                formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
            }
            
            return LocalDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            // 기본 현재 시간 반환
            return LocalDateTime.now();
        }
    }

    /**
     * 타임스탬프가 기준 시간 이후인지 확인
     *
     * @param timestamp 타임스탬프 문자열
     * @param startTime 기준 시간
     * @return 기준 시간 이후인지 여부
     */
    public static boolean isAfter(String timestamp, LocalDateTime startTime) {
        try {
            LocalDateTime logTime = parseTimestamp(timestamp);
            return logTime.isAfter(startTime);
        } catch (Exception e) {
            return false;
        }
    }
} 