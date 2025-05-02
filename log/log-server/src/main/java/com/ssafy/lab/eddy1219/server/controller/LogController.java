package com.ssafy.lab.eddy1219.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lab.eddy1219.server.Service.LogstashService;
import com.ssafy.lab.eddy1219.server.model.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
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

    private final ObjectMapper objectMapper;
    private final LogstashService logstashService;

    // 최근 로그를 저장할 메모리 내 큐 (동시성 지원)
    private static final int MAX_LOG_ENTRIES = 100; // 메모리에 보관할 최대 로그 수
    private final Queue<LogEntry> recentLogs = new ConcurrentLinkedQueue<>();

    /**
     * 생성자를 통해 ObjectMapper와 LogstashService를 주입받습니다.
     *
     * @param objectMapper    Spring 컨텍스트에 등록된 ObjectMapper 빈
     * @param logstashService Logstash로 로그를 전송하는 서비스 빈
     */
    public LogController(ObjectMapper objectMapper, LogstashService logstashService) {
        this.objectMapper = objectMapper;
        this.logstashService = logstashService;
    }

    /**
     * 로그 배치를 수신하여 Logstash로 전송합니다.
     * (POST /api/logs)
     *
     * @param logEntries 로그 엔트리 객체 리스트
     * @return 처리 결과 (성공 시 200 OK)
     */
    @PostMapping
    public ResponseEntity<Void> receiveLogBatch(@RequestBody List<LogEntry> logEntries) {

        if (logEntries == null || logEntries.isEmpty()) {
            System.out.println("Received empty log batch.");
            return ResponseEntity.ok().build(); // 빈 배치는 정상 처리
        }

        System.out.println("Received log batch with " + logEntries.size() + " entries. Forwarding to Logstash...");

        // 새 로그 추가 및 오래된 로그 제거 (큐 크기 제한) - Logstash 전송과는 별개로 최근 로그를 메모리에 유지
        for (LogEntry entry : logEntries) {
            if (entry != null) { // 리스트 내 null 요소 방지
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

    /**
     * 최근 수신된 로그 목록을 조회합니다.
     * (GET /api/logs)
     *
     * @return 최근 로그 엔트리 리스트 (JSON 형식)
     */
    @GetMapping
    public ResponseEntity<List<LogEntry>> getRecentLogs() {
        // 현재 큐에 있는 로그를 리스트로 복사하여 반환 (순서는 들어온 순서)
        List<LogEntry> logsToReturn = new LinkedList<>(recentLogs);
        return ResponseEntity.ok(logsToReturn);
    }
}