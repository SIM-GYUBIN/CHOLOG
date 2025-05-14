package com.example.logserver.service;

import com.example.logserver.model.LogEntry;
import com.example.logserver.repository.LogRepository;
import com.example.logserver.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 로그 통계 처리 서비스
 * 로그 데이터의 통계 분석을 제공
 */
@Service
public class StatisticsService {

    private final LogRepository logRepository;

    @Autowired
    public StatisticsService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 로그 통계 조회
     *
     * @param apiKey API 키
     * @param serviceName 서비스 이름
     * @param timeRange 시간 범위
     * @return 통계 정보 맵
     */
    public Map<String, Object> getLogStatistics(String apiKey, String serviceName, String timeRange) {
        // 시간 범위로 필터링된 로그 목록 조회
        List<LogEntry> filteredLogs = logRepository.getFilteredLogsByTime(apiKey, serviceName, timeRange);
        
        Map<String, Object> statistics = new HashMap<>();

        // 1. 총 로그 건수
        statistics.put("totalLogs", filteredLogs.size());

        // 2. 로그 레벨 분포
        Map<String, Long> levelStats = filteredLogs.stream()
                .filter(log -> log.getLevel() != null)
                .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        statistics.put("levelDistribution", levelStats);

        // 3. HTTP 상태 코드 분포
        Map<Integer, Long> statusStats = filteredLogs.stream()
                .filter(log -> log.getHttpStatus() != null)
                .collect(Collectors.groupingBy(LogEntry::getHttpStatus, Collectors.counting()));
        
        // 상태 코드 분포를 배열 형태로 변환
        List<Map<String, Object>> statusChartData = new ArrayList<>();
        statusStats.forEach((status, count) -> {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("status", status);
            dataPoint.put("count", count);
            statusChartData.add(dataPoint);
        });
        statistics.put("statusDistributionChart", statusChartData);

        // 4. 응답 시간 통계
        DoubleSummaryStatistics responseTimeStats = filteredLogs.stream()
                .filter(log -> log.getPerformanceMetrics() != null && log.getPerformanceMetrics().get("responseTime") != null)
                .mapToDouble(log -> {
                    Object responseTime = log.getPerformanceMetrics().get("responseTime");
                    if (responseTime instanceof Number) {
                        return ((Number) responseTime).doubleValue();
                    }
                    return 0.0;
                })
                .summaryStatistics();

        Map<String, Object> responseTimeMetrics = new HashMap<>();
        responseTimeMetrics.put("count", responseTimeStats.getCount());
        responseTimeMetrics.put("average", responseTimeStats.getAverage());
        responseTimeMetrics.put("max", responseTimeStats.getMax());
        responseTimeMetrics.put("min", responseTimeStats.getMin());
        statistics.put("responseTimeMetrics", responseTimeMetrics);

        // 5. 가장 많이 요청된 URI TOP 10
        Map<String, Long> uriStats = filteredLogs.stream()
                .filter(log -> log.getRequestUri() != null)
                .collect(Collectors.groupingBy(LogEntry::getRequestUri, Collectors.counting()));

        List<Map.Entry<String, Long>> topUris = uriStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        statistics.put("topRequestedUris", topUris);

        // 6. 에러 발생 분포
        Map<String, Long> errorStats = filteredLogs.stream()
                .filter(log -> "ERROR".equals(log.getLevel()) && log.getMessage() != null)
                .collect(Collectors.groupingBy(
                        log -> {
                            String msg = log.getMessage();
                            // 메시지가 너무 긴 경우 짧게 처리
                            return msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
                        },
                        Collectors.counting()
                ));

        List<Map.Entry<String, Long>> topErrors = errorStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        statistics.put("topErrors", topErrors);

        // 7. 플랫폼 분포
        Map<String, Long> platformStats = filteredLogs.stream()
                .filter(log -> log.getUaPlatform() != null)
                .collect(Collectors.groupingBy(LogEntry::getUaPlatform, Collectors.counting()));
        statistics.put("platformDistribution", platformStats);

        // 8. 모바일/데스크톱 분포
        Map<Boolean, Long> mobileStats = filteredLogs.stream()
                .filter(log -> log.getUaMobile() != null)
                .collect(Collectors.groupingBy(LogEntry::getUaMobile, Collectors.counting()));
        statistics.put("mobileDistribution", mobileStats);

        // 9. 시간대별 로그 건수 (최근 24시간)
        if (timeRange == null || timeRange.equals("24h") || timeRange.equals("1h") || timeRange.equals("6h")) {
            Map<Integer, Long> hourlyStats = new HashMap<>();
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);

            filteredLogs.stream()
                    .filter(log -> log.getTimestamp() != null && DateTimeUtil.isAfter(log.getTimestamp(), dayAgo))
                    .forEach(log -> {
                        try {
                            LocalDateTime timestamp = DateTimeUtil.parseTimestamp(log.getTimestamp());
                            int hour = timestamp.getHour();
                            hourlyStats.put(hour, hourlyStats.getOrDefault(hour, 0L) + 1);
                        } catch (Exception e) {
                            // 날짜 파싱 실패 시 무시
                        }
                    });
            statistics.put("hourlyDistribution", hourlyStats);
        }

        return statistics;
    }

    /**
     * 차트 데이터 조회
     *
     * @param apiKey API 키
     * @param serviceName 서비스 이름
     * @param period 기간
     * @return 차트 데이터 맵
     */
    public Map<String, Object> getChartData(String apiKey, String serviceName, String period) {
        // 기본값은 지난 24시간
        String timeRange = period != null ? period : "24h";
        Map<String, Object> statistics = getLogStatistics(apiKey, serviceName, timeRange);

        Map<String, Object> chartData = new HashMap<>();

        // 로그 레벨 분포 차트 데이터
        Map<String, Long> levelDistribution = (Map<String, Long>) statistics.get("levelDistribution");
        List<Map<String, Object>> levelChartData = new ArrayList<>();

        if (levelDistribution != null) {
            levelDistribution.forEach((level, count) -> {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("level", level);
                dataPoint.put("count", count);
                levelChartData.add(dataPoint);
            });
        }
        chartData.put("levelDistributionChart", levelChartData);

        // HTTP 상태 코드 분포 차트 데이터는 이미 배열 형태로 변환되어 있으므로 그대로 사용
        chartData.put("statusDistributionChart", statistics.get("statusDistributionChart"));

        // 시간별 로그 분포 차트 데이터
        Map<Integer, Long> hourlyDistribution = (Map<Integer, Long>) statistics.get("hourlyDistribution");
        List<Map<String, Object>> hourlyChartData = new ArrayList<>();

        if (hourlyDistribution != null) {
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("hour", hour);
                dataPoint.put("count", hourlyDistribution.getOrDefault(hour, 0L));
                hourlyChartData.add(dataPoint);
            }
        }
        chartData.put("hourlyDistributionChart", hourlyChartData);

        return chartData;
    }
} 