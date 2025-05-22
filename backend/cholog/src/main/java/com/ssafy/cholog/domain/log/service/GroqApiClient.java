package com.ssafy.cholog.domain.log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

// Groq API 요청/응답을 위한 내부 DTO
record GroqMessage(String role, String content) {
}

record GroqChatRequest(List<GroqMessage> messages, String model, Double temperature, Integer max_tokens,
                       Boolean stream) {
} // stream: false (일반적으로)

record GroqChoice(Integer index, GroqMessage message, GroqMessage delta, String finish_reason, Object logprobs) {
} // delta, logprobs는 스트리밍 또는 특정 기능 사용 시

record GroqUsage(Integer prompt_tokens, Integer completion_tokens, Integer total_tokens) {
}

record GroqChatCompletionResponse(String id, String object, Long created, String model, List<GroqChoice> choices,
                                  GroqUsage usage, String system_fingerprint) {
}


@Service
@Slf4j
public class GroqApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // JSON 문자열 변환 및 LogDocument 단순화용

    public GroqApiClient(WebClient.Builder webClientBuilder,
                         @Value("${groq.api.key}") String groqApiKey,
                         ObjectMapper objectMapper) { // ObjectMapper 주입 받도록 수정
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        // 주입받은 ObjectMapper를 사용하거나, 여기서 새로 생성할 수 있습니다.
        // JavaTimeModule은 Instant 등의 직렬화를 위해 등록하는 것이 좋습니다.
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    /**
     * Groq LLM을 사용하여 분석을 요청합니다.
     * userPrompt는 이미 포맷팅된 로그 데이터를 포함하고 있다고 가정합니다.
     *
     * @param model        사용할 LLM 모델
     * @param systemPrompt 시스템 메시지
     * @param userPrompt   사용자 메시지 (포맷팅된 로그 데이터 포함)
     * @param maxTokens    응답 최대 토큰 수
     * @param temperature  응답 생성 시 무작위성 (0.0 ~ 2.0)
     * @return Groq API 응답을 Mono 형태로 반환
     */
    public Mono<GroqChatCompletionResponse> analyzeWithGroq(String model, String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        List<GroqMessage> messages = List.of(
                new GroqMessage("system", systemPrompt),
                new GroqMessage("user", userPrompt)
        );

        // 스트리밍을 사용하지 않는 경우 stream: false 또는 null
        GroqChatRequest requestPayload = new GroqChatRequest(messages, model, temperature, maxTokens, false);

        log.debug("Sending request to Groq API. Model: {}, MaxTokens: {}, Temperature: {}", model, maxTokens, temperature);
        // log.trace("Groq Request Payload (user prompt may contain sensitive log data): {}", requestPayload);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(GroqChatCompletionResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                        log.debug("Successfully received response from Groq API. Finish reason: {}", response.choices().get(0).finish_reason());
                        if (response.usage() != null) {
                            log.debug("Groq API Usage: Prompt Tokens: {}, Completion Tokens: {}, Total Tokens: {}",
                                    response.usage().prompt_tokens(), response.usage().completion_tokens(), response.usage().total_tokens());
                        }
                    } else {
                        log.warn("Received null or empty choice response from Groq API.");
                    }
                })
                .doOnError(error -> log.error("Error calling Groq API: {}", error.getMessage(), error));
    }

    /**
     * LLM에 전달하기 위해 LogDocument 리스트를 JSON 문자열로 포맷팅합니다.
     * 각 LogDocument는 SimplifiedLogForLlm 형태로 단순화됩니다.
     * LogAnalysisService에서 Collections.singletonList()로 단일 로그를 전달하는 경우에도 이 메서드가 호출됩니다.
     *
     * @param logs 분석할 LogDocument 리스트
     * @return 로그 리스트의 JSON 문자열 표현
     */
    public String formatLogsForLlm(List<LogDocument> logs) {
        if (logs == null || logs.isEmpty()) {
            return "[]"; // 빈 배열
        }
        try {
            List<SimplifiedLogForLlm> simplifiedLogs = logs.stream()
                    .map(this::simplifyLog) // 내부 simplifyLog 메서드 사용
                    .collect(Collectors.toList());
            // LLM이 JSON 배열을 기대하는 경우 (현재 LogAnalysisService의 사용 방식과 일치)
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simplifiedLogs);
        } catch (Exception e) {
            log.error("Error formatting logs for LLM: {}", e.getMessage(), e);
            // 에러 발생 시 LLM에 전달될 문자열. 실제로는 예외를 던지거나 다른 방식으로 처리할 수 있습니다.
            return "[{\"error\":\"Failed to format logs\"}]";
        }
    }

    /**
     * LogDocument를 LLM 분석에 더 적합한 SimplifiedLogForLlm 형태로 변환합니다.
     * 이 메서드는 필요한 필드만 선택하고, 너무 긴 내용은 자르는 등의 역할을 합니다.
     *
     * @param log 원본 LogDocument
     * @return 단순화된 로그 객체
     */
    private SimplifiedLogForLlm simplifyLog(LogDocument log) {
        SimplifiedLogForLlm simple = new SimplifiedLogForLlm();
        simple.setTimestamp(log.getTimestampOriginal());
//        simple.setTimestamp(log.getTimestampEs() != null ? log.getTimestampEs() : log.getTimestampOriginal());
        simple.setLevel(log.getLevel());
        // 메시지가 너무 길 경우 처음 1000자만 사용 (예시)
        if (log.getMessage() != null && log.getMessage().length() > 1000) {
            simple.setMessage(log.getMessage().substring(0, 1000) + "...");
        } else {
            simple.setMessage(log.getMessage());
        }
        simple.setSource(log.getSource());
        simple.setProjectKey(log.getProjectKey()); // LogDocument에 projectKey가 있다고 가정
        simple.setEnvironment(log.getEnvironment());
        simple.setLogType(log.getLogType());

        if (log.getError() != null) {
            simple.setErrorType(log.getError().getType());
            simple.setErrorMessage(log.getError().getMessage());
            // 스택 트레이스는 매우 길 수 있으므로, 처음 500자 또는 특정 패턴만 포함
            if (log.getError().getStacktrace() != null && log.getError().getStacktrace().length() > 500) {
                simple.setErrorStacktrace(log.getError().getStacktrace().substring(0, 500) + "...");
            } else {
                simple.setErrorStacktrace(log.getError().getStacktrace());
            }
        }

        if (log.getHttp() != null) {
            simple.setHttpMethod(log.getHttp().getMethod());
            simple.setRequestUrl(log.getHttp().getRequestUri());
            if (log.getHttp().getStatusCode() != null) {
                simple.setStatusCode(log.getHttp().getStatusCode());
            }
            if (log.getHttp().getResponseTime() != null) {
                simple.setHttpDurationMs(log.getHttp().getResponseTime());
            }
        }

        // 필요한 경우 client, event 정보 등 추가
        if (log.getClient() != null) {
            simple.setClientUrl(log.getClient().getUrl());
            simple.setUserAgent(log.getClient().getUserAgent());
        }

        return simple;
    }

    /**
     * LLM 전달용으로 단순화된 로그 정보를 담는 내부 DTO.
     * LogDocument의 모든 필드를 포함하지 않고, 분석에 유용하거나 토큰을 절약할 수 있는 필드 위주로 구성합니다.
     */
    @Data // Lombok @Data 어노테이션 사용 (Getter, Setter, toString, equals, hashCode 자동 생성)
    private static class SimplifiedLogForLlm {
        private Instant timestamp;
        private String level;
        private String message;
        private String source;
        private String projectKey;
        private String environment;
        private String logType;

        // ErrorInfo
        private String errorType;
        private String errorMessage;
        private String errorStacktrace;

        // HttpInfo
        private String httpMethod;
        private String requestUrl;
        private Integer statusCode;
        private Long httpDurationMs;

        // ClientInfo
        private String clientUrl;
        private String userAgent;
    }
}