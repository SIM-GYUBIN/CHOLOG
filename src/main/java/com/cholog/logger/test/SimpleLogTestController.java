package com.cholog.logger.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * CHO:LOG SDK 테스트용 간단한 로그 서버 컨트롤러
 */
@RestController
@RequestMapping("/api/logs")
public class SimpleLogTestController {

    // 최근 로그 메시지 저장 (최대 10개)
    private final List<String> recentLogs = new ArrayList<>();
    private static final int MAX_LOGS = 10;

    /**
     * 로그 수신 엔드포인트
     */
    @PostMapping
    public ResponseEntity<String> receiveLog(HttpServletRequest request) throws IOException {
        String contentEncoding = request.getHeader("Content-Encoding");
        String logContent;
        
        // GZIP 압축 확인
        if (contentEncoding != null && contentEncoding.contains("gzip")) {
            // 압축 해제
            try (GZIPInputStream gzipStream = new GZIPInputStream(request.getInputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                logContent = content.toString();
            }
        } else {
            // 압축되지 않은 요청
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                logContent = content.toString();
            }
        }
        
        // 콘솔에 로그 출력
        System.out.println("로그 수신: " + logContent);
        
        // 최근 로그 목록에 추가
        synchronized (recentLogs) {
            recentLogs.add(logContent);
            if (recentLogs.size() > MAX_LOGS) {
                recentLogs.remove(0);
            }
        }
        
        return ResponseEntity.ok("로그 수신 완료");
    }
    
    /**
     * 최근 로그 조회 엔드포인트
     */
    @GetMapping
    public ResponseEntity<List<String>> getRecentLogs() {
        synchronized (recentLogs) {
            return ResponseEntity.ok(new ArrayList<>(recentLogs));
        }
    }
    
    /**
     * 서버 상태 확인 엔드포인트 (SDK의 서버 연결 체크 요청에 응답)
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("OK");
    }
} 