package com.ssafy.lab.eddy1219.server.controller;

import com.ssafy.lab.eddy1219.server.Service.LogstashService;
import com.ssafy.lab.eddy1219.server.model.JsLogEntry;
import com.ssafy.lab.eddy1219.server.model.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 중앙 로그 서버의 로그 수신 및 조회를 담당하는 컨트롤러입니다.
 * POST /api/logs 로 로그 배치를 받아 Logstash로 전송합니다.
 * GET /api/logs 로 최근 수신된 로그 목록을 조회합니다.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogstashService logstashService;

    // 최근 로그를 저장할 메모리 내 큐 (동시성 지원)
    private static final int MAX_LOG_ENTRIES = 100; // 메모리에 보관할 최대 로그 수
    private final Queue<LogEntry> recentLogs = new ConcurrentLinkedQueue<>();
    private final Queue<JsLogEntry> recentJsLogs = new ConcurrentLinkedQueue<>();

    public LogController(LogstashService logstashService) {
        this.logstashService = logstashService;
    }

    /**
     * 서버가 살아있는지 확인하는 헬스 체크 API입니다.
     * (GET /api/logs/be)
     *
     * @return 200
     */
    @GetMapping("/be")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

    /**
     * 로그 배치를 수신하여 Logstash로 전송합니다.
     * (POST /api/logs)
     *
     * @param logEntries 로그 엔트리 객체 리스트
     * @return 처리 결과 (성공 시 200 OK)
     */
    @PostMapping("/be")
    public ResponseEntity<Void> receiveLogBatch(@RequestBody List<LogEntry> logEntries) {

        if (logEntries == null || logEntries.isEmpty()) {
            System.out.println("Received empty log batch.");
            return ResponseEntity.ok().build(); // 빈 배치는 정상 처리
        }

//        System.out.println("Received log batch with " + logEntries.size() + " entries. Forwarding to Logstash...");

        // 새 로그 추가 및 오래된 로그 제거 (큐 크기 제한) - Logstash 전송과는 별개로 최근 로그를 메모리에 유지
        for (LogEntry entry : logEntries) {
            if (entry != null) { // 리스트 내 null 요소 방지
                // 1. LogEntry 생성 시 이미 projectKey가 설정된 경우 (예: MDC 통해)
                // 2. apiKey 필드를 projectKey로 사용 (아래 예시)
                // 3. 고정된 projectKey 사용 (환경변수/설정파일에서 주입)
                if (entry.getProjectKey() == null || entry.getProjectKey().trim().isEmpty()) {
                    if (entry.getApiKey() != null && !entry.getApiKey().isEmpty()) {
                        // apiKey에 프로젝트 식별자가 포함된 경우 파싱 로직 추가 가능
                        entry.setProjectKey(entry.getApiKey()); // apiKey를 projectKey로 사용
                    }
                }

                // logType 설정 로직!!!!!
                if (entry.getLevel() != null && "ERROR".equalsIgnoreCase(entry.getLevel())) {
                    entry.setLogType("error");
                } else {
                    entry.setLogType("general");
                }

                // 큐가 꽉 찼으면 가장 오래된 것 제거
                while (recentLogs.size() >= MAX_LOG_ENTRIES) {
                    recentLogs.poll();
                }
                // 새 로그 추가
                recentLogs.offer(entry);
            }
        }

        // Logstash로 로그 전송
        logstashService.sendLogs(logEntries);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/js")
    public ResponseEntity<Void> receiveJsLogBatch(@RequestBody List<JsLogEntry> logEntries) { // Jackson이 List<새로운 LogEntry>로 변환
        if (logEntries == null || logEntries.isEmpty()) {
            System.out.println("Received empty log batch.");
            return ResponseEntity.ok().build();
        }

        // 여기서 logEntries의 내용을 디버깅하거나 로깅하여 올바르게 역직렬화되었는지 확인해보세요.
        // 예: System.out.println("First log entry message: " + logEntries.get(0).getMessage());
        // if (logEntries.get(0).getHttpInfo() != null) {
        //     System.out.println("HTTP method: " + logEntries.get(0).getHttpInfo().getRequest().getMethod());
        // }
//        System.out.println("Received log batch with " + logEntries.size() + " entries. Forwarding to Logstash...");

        for (JsLogEntry entry : logEntries) {
            if (entry != null) {
                while (recentJsLogs.size() >= MAX_LOG_ENTRIES) {
                    recentJsLogs.poll();
                }
                recentJsLogs.offer(entry);
            }
        }

        logstashService.sendJsLogs(logEntries); // LogstashService는 수정된 LogEntry 리스트를 받음

        return ResponseEntity.ok().build();
    }
}