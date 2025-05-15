package com.ssafy.cholog.domain.log.service;

import com.ssafy.cholog.domain.log.dto.response.LogAnalysisResponse;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.webhook.dto.request.LogAnalysisRequest;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final GroqApiClient groqApiClient;
    private final ProjectRepository projectRepository;

    @Value("${groq.default-model}")
    private String defaultModel;

    @Value("${groq.default-temperature}")
    private double defaultTemperature;

    @Value("${groq.default-max-tokens}")
    private int defaultMaxTokens;

    public Mono<LogAnalysisResponse> analyzeLogWithGroq(Integer projectId, LogAnalysisRequest request) {
        // 블로킹 호출들을 Mono.fromCallable로 감싸고 적절한 스케줄러에서 실행
        return Mono.fromCallable(() -> {
                    // 1. 프로젝트 토큰 조회 (블로킹)
                    Project project = projectRepository.findById(projectId) // 이 메서드가 Optional<Project>를 반환한다고 가정
                            .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId", projectId));
                    String projectToken = project.getProjectToken(); // project 객체에 getProjectToken() 메서드가 있다고 가정

                    // 2. Elasticsearch에서 단일 로그 조회 (블로킹)
                    String indexName = "pjt-" + projectToken;
                    LogDocument logDocument = elasticsearchOperations.get(request.getLogId(), LogDocument.class, IndexCoordinates.of(indexName));

                    if (logDocument == null) {
                        // CustomException에 addParameter 메서드가 있다고 가정
                        throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                                .addParameter("projectId", projectId)
                                .addParameter("logId", request.getLogId());
                    }
                    return logDocument; // 다음 단계로 logDocument 전달
                })
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업을 별도 스레드 풀에서 실행
                .flatMap(logDocument -> { // 이전 단계의 결과를 받아 리액티브 체인 계속
                    String formattedLog = groqApiClient.formatLogsForLlm(Collections.singletonList(logDocument));

                    String systemPrompt = "당신은 [CHO:LOG]의 공식 마스코트이자, 사용자를 돕는 것을 매우 좋아하는 똑똑하고 친절한 AI 로그 분석가 '초록이'입니다. " +
                            "당신의 주요 임무는 사용자가 제공하는 JSON 형식의 단일 로그 항목을 분석하여 유용한 정보를 제공하는 것입니다. " +
                            "분석 결과에는 다음 세 가지 핵심 내용이 반드시 포함되어야 합니다: " +
                            "1. 로그에서 확인된 주요 문제점 또는 에러 상황. " +
                            "2. 해당 문제의 가장 가능성이 높은 근본 원인에 대한 추론. " +
                            "3. 사용자가 문제를 해결하거나 상황을 개선하는 데 도움이 될 수 있는 구체적인 권장 조치 (1~2가지). " +
                            "모든 답변은 반드시 한국어로 작성해야 합니다. " +
                            "그리고 가장 중요한 것은, 친절하고 공손하게 대답해야 한다는 것입니다. " +
                            "예를 들어, 분석 결과를 설명할 때 다음과 같은 방식으로 답변해야 합니다: " +
                            "\"이 로그를 보니, 입력값이 유효하지 않아서 오류가 발생한 것 같아요! 🐸 보내는 값이 올바른지 다시 한번 확인해보세요! 🐸\" " +
                            "사용자에게 항상 밝고 긍정적인 태도로, 명확하고 이해하기 쉽게 설명해주세요.";
                    String userPrompt = "안녕, 초록아! 아래 로그 데이터를 보고 무슨 일인지 자세히 좀 알려줘! " +
                            "꼼꼼하게 분석해서 친절하게 설명해주면 정말 고맙겠다!\n\n" +
                            "--- 분석할 로그 데이터 --- \n" +
                            formattedLog + "\n" +
                            "--- 여기까지 로그 데이터 ---";

                    return groqApiClient.analyzeWithGroq(defaultModel, systemPrompt, userPrompt, defaultMaxTokens, defaultTemperature)
                            .map(groqResponse -> {
                                String analysisText = "No analysis result from LLM.";
                                String modelUsed = defaultModel;

                                if (groqResponse != null) {
                                    modelUsed = groqResponse.model() != null ? groqResponse.model() : defaultModel;
                                    if (groqResponse.choices() != null && !groqResponse.choices().isEmpty() && groqResponse.choices().get(0).message() != null) {
                                        analysisText = groqResponse.choices().get(0).message().content();
                                    }
                                }
                                return LogAnalysisResponse.builder()
                                        .analysisResult(analysisText)
                                        .modelUsed(modelUsed)
                                        .build();
                            });
                })
                .doOnError(e -> { // 에러 로깅 (예외는 여전히 전파됨)
                    if (e instanceof CustomException) {
                        CustomException ce = (CustomException) e;
                        log.error("CustomError during Groq analysis for projectId {}: {}, logId: {}. ErrorCode: {}, Details: {}",
                                projectId, request.getLogId(), ce.getErrorCode(), ce.getParameters(), ce);
                    } else {
                        log.error("Error during Groq analysis for projectId {}: {}, logId: {}",
                                projectId, request.getLogId(), e.getMessage(), e);
                    }
                });
    }
}