package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
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
                    String indexName = "pjt-*-" + projectToken;

//                    LogDocument logDocument = elasticsearchOperations.get(request.getLogId(), LogDocument.class, IndexCoordinates.of(indexName));
//
//                    if (logDocument == null) {
//                        // CustomException에 addParameter 메서드가 있다고 가정
//                        throw new CustomException(ErrorCode.LOG_NOT_FOUND)
//                                .addParameter("projectId", projectId)
//                                .addParameter("logId", request.getLogId());
//                    }
//                    return logDocument; // 다음 단계로 logDocument 전달
                    co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl = QueryBuilders.ids(iq -> iq
                            .values(request.getLogId())
                    );

                    Query searchQuery = NativeQuery.builder()
                            .withQuery(esQueryDsl)
                            .withMaxResults(1) // ID는 고유하므로 최대 1개의 결과만 필요
                            .build();

                    SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                            searchQuery,
                            LogDocument.class,
                            IndexCoordinates.of(indexName) // 와일드카드가 포함된 인덱스 패턴 사용
                    );

                    if (searchHits.getTotalHits() == 0 || !searchHits.hasSearchHits()) {
                        throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                                .addParameter("projectId", projectId)
                                .addParameter("logId", request.getLogId());
                    }
                    // 첫 번째 검색 결과에서 LogDocument를 가져옵니다.
                    return searchHits.getSearchHit(0).getContent();
                })
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업을 별도 스레드 풀에서 실행
                .flatMap(logDocument -> { // 이전 단계의 결과를 받아 리액티브 체인 계속
                    String formattedLog = groqApiClient.formatLogsForLlm(Collections.singletonList(logDocument));

                    String systemPrompt = "당신은 [CHO:LOG]의 공식 마스코트이자, 사용자를 돕는 것을 매우 좋아하는 똑똑하고 친절한 AI 로그 분석가 '초록이'입니다. " +
                            "당신의 주요 임무는 사용자가 제공하는 JSON 형식의 단일 로그 항목을 분석하여 유용한 정보를 제공하는 것입니다. " +
                            "분석 결과에는 다음 세 가지 핵심 내용이 **각각 명확하고 간결하게, 단 한 번씩만 포함**되어야 합니다. **설명 과정에서 동일한 내용이 반복되지 않도록 주의해주세요.** " +
                            "1. 로그에서 확인된 주요 상태(정상, 경고, 오류 등)와 문제점 또는 에러 상황 (만약 있다면 구체적으로). " +
                            "   **만약 분석 결과 특이사항이나 오류가 없다면, '로그 내용이 정상적으로 보입니다.' 와 같이 긍정적으로 언급하고, 해당 로그가 어떤 작업을 수행했는지 간략히 요약해주세요.** " +
                            "2. 해당 문제의 가장 가능성이 높은 근본 원인에 대한 추론 (문제가 있을 경우에만 해당, **1번 항목에서 이미 명확히 언급된 경우 간략히 확인만 하거나 생략 가능**). " +
                            "3. 사용자가 문제를 해결하거나 상황을 개선하는 데 도움이 될 수 있는 구체적인 권장 조치 (1~2가지, 문제가 있을 경우에만 해당, **이미 언급된 내용을 재차 설명하지 않도록 주의**). " +
                            "모든 답변은 **반드시 자연스러운 한국어로만** 작성해야 합니다. " +
                            "그리고 가장 중요한 것은, 친절하고 공손하게 대답해야 한다는 것입니다. 답변은 논리적이고 일관성이 있어야 하며, 사용자가 혼란을 느끼지 않도록 명확해야 합니다. " +
                            "예를 들어, 분석 결과를 설명할 때 다음과 같은 방식으로 답변해야 합니다: " +
                            " (오류 발생 시 예시) \"로그를 살펴보니, [로그에서 확인된 문제점, 예: 주문 처리 중 'ITEM001' 상품의 재고 부족]으로 인해 오류가 발생했네요! 아마도 [간단한 원인 언급, 예: 실시간 재고 업데이트가 늦어졌거나, 동시에 많은 주문이 몰린 것] 같아요. 이 문제를 해결하려면, [조치 1, 예: 즉시 해당 상품의 재고를 확인하고 보충해주시는 게 좋겠어요!] 또는 [조치 2, 예: 일시적으로 해당 상품의 주문을 중지하는 것도 방법이에요!] 조치가 완료되면 다시 한번 확인해보세요! \" " +
                            " (경고 발생 시 예시 - 'deprecated' 상황) \"로그를 살펴보니, 경고가 발생했어요! 사용자가 'oldCheckoutButton'이라는 버튼을 클릭했는데, 이 버튼은 현재 **더 이상 사용되지 않는(deprecated)** 상태인 것 같아요. 아마 새로운 결제 방식이 도입되면서 기존 버튼이 지원 중단된 것으로 보여요. 사용자분들이 혼란스럽지 않도록, 해당 버튼을 새로운 결제 페이지로 안내하거나, UI에서 제거하는 것을 고려해보시는 게 좋겠어요! \" " +
                            " (정상 작동 시 예시) \"로그를 살펴보니, 모든 작업이 성공적으로 완료된 것 같아요! 요청하신 [작업 내용] 처리가 문제없이 잘 마무리되었습니다! 잘 하고 있어요! \" " +
                            "사용자에게 항상 밝고 긍정적인 태도로, 명확하고 이해하기 쉽게 설명해주세요.";

                    String userPrompt = "아래 로그 데이터를 보고 무슨 일인지 자세히 좀 알려줘. " +
                            "꼼꼼하게 분석해서 친절하게 설명해주면 정말 고맙겠다!\n\n" +
                            "--- 분석할 로그 데이터 --- \n" +
                            formattedLog + "\n" +
                            "--- 여기까지 로그 데이터 ---";

                    return groqApiClient.analyzeWithGroq(defaultModel, systemPrompt, userPrompt, defaultMaxTokens, defaultTemperature)
                            .map(groqResponse -> {
                                // 응답 받은거 로그 출력!
                                log.info("Groq analysis response: {}", groqResponse.choices().get(0).message().content());

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