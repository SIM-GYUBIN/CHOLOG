package com.ssafy.lab.eddy1219.server.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lab.eddy1219.server.model.JsLogEntry;
import com.ssafy.lab.eddy1219.server.model.LogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogstashService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${logstash.url:http://localhost:9600}")
    private String logstashUrl;

    public void sendLogs(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonPayload = objectMapper.writeValueAsString(logEntries);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(logstashUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully sent " + logEntries.size() + " logs to Logstash.");
            } else {
                System.err.println("Failed to send logs to Logstash. Status code: " + response.getStatusCode());
                System.err.println("Response body: " + response.getBody());
            }

        } catch (JsonProcessingException e) {
            System.err.println("Error converting log entries to JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error sending logs to Logstash: " + e.getMessage());
        }
    }

    public void sendJsLogs(List<JsLogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonPayload = objectMapper.writeValueAsString(logEntries);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            // 보내기 전 json 어떻게 생겼는지 출력
//            System.out.println(jsonPayload);


            ResponseEntity<String> response = restTemplate.postForEntity(logstashUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully sent " + logEntries.size() + " logs to Logstash.");
            } else {
                System.err.println("Failed to send logs to Logstash. Status code: " + response.getStatusCode());
                System.err.println("Response body: " + response.getBody());
            }

        } catch (JsonProcessingException e) {
            System.err.println("Error converting log entries to JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error sending logs to Logstash: " + e.getMessage());
        }
    }
}