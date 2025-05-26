package com.ssafy.cholog.domain.report.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.report.dto.item.*;
import com.ssafy.cholog.domain.report.dto.request.ReportRequest;
import com.ssafy.cholog.domain.report.dto.response.ReportResponse;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${app.domain.url}")
    private String frontendBaseUrl;

    // Aggregation 이름 상수 (이전과 동일)
    private static final String AGG_FILTER_FRONTEND = "filter_frontend";
    private static final String AGG_FILTER_BACKEND = "filter_backend";
    private static final String AGG_LOG_LEVELS = "log_levels";
    private static final String AGG_LOG_TREND = "log_trend_by_hour";
    private static final String AGG_FILTER_FRONTEND_ERRORS = "filter_frontend_errors";
    private static final String AGG_TERMS_FRONTEND_ERRORS = "terms_frontend_errors";
    private static final String AGG_FILTER_BACKEND_ERRORS = "filter_backend_errors";
    private static final String AGG_TERMS_BACKEND_ERRORS = "terms_backend_errors";
    private static final String AGG_FILTER_SLOW_BACKEND_APIS = "filter_slow_backend_apis";
    private static final String AGG_TERMS_SLOW_BACKEND_APIS = "terms_slow_backend_apis";
    private static final String AGG_SUB_AVG_RESPONSE_TIME = "sub_avg_response_time";
    private static final String AGG_SUB_MAX_RESPONSE_TIME = "sub_max_response_time";
    private static final String AGG_SUB_API_METHOD = "sub_api_method";


    public ReportResponse makeReport(Integer userId, Integer projectId, ReportRequest reportRequest) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        String projectToken = project.getProjectToken();
        String indexName = "pjt-*-" + projectToken;

        ZoneId reportTimeZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime startDateTimeUtc = parseDateTime(reportRequest.getStartDate(), reportTimeZone, true);
        ZonedDateTime endDateTimeUtc = parseDateTime(reportRequest.getEndDate(), reportTimeZone, false);

        if (startDateTimeUtc.isAfter(endDateTimeUtc)) {
            throw new CustomException(ErrorCode.LOG_START_TIME_AFTER_END_TIME)
                    .addParameter("startDate", reportRequest.getStartDate())
                    .addParameter("endDate", reportRequest.getEndDate());
        }

        String periodDescription = generatePeriodDescription(startDateTimeUtc, endDateTimeUtc, reportTimeZone);

        NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder();
        buildReportAggregations(nativeQueryBuilder, startDateTimeUtc, endDateTimeUtc);

        Query timeRangeQuery = QueryBuilders.range(r -> r
                .field("timestamp")
                .gte(JsonData.of(startDateTimeUtc.toInstant().toEpochMilli()))
                .lt(JsonData.of(endDateTimeUtc.toInstant().toEpochMilli()))
        );

        // 메시지 필드 존재 여부 확인 쿼리 (LogService 참고)
        Query messageExistsQuery = QueryBuilders.exists(e -> e.field("message"));

        // 두 쿼리를 Bool 쿼리로 결합 (두 조건 모두 만족해야 함)
        Query finalQuery = QueryBuilders.bool(b -> b
                .must(timeRangeQuery)
                .must(messageExistsQuery)
        );

        nativeQueryBuilder
                .withQuery(finalQuery)
                .withMaxResults(0)
                .withTrackTotalHits(true);

        NativeQuery searchQuery = nativeQueryBuilder.build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexName)
        );

        return parseResultsAndBuildResponse(searchHits, project.getId().toString(), periodDescription);
    }

    private void buildReportAggregations(NativeQueryBuilder nativeQueryBuilder, ZonedDateTime startDateTimeUtc, ZonedDateTime endDateTimeUtc) {
        // --- 기존 코드 시작 ---
        // 1. Frontend/Backend 로그 수 필터
        Query frontendQuery = QueryBuilders.term(t -> t.field("source.keyword").value("frontend"));
        Query backendQuery = QueryBuilders.term(t -> t.field("source.keyword").value("backend")); // backendQuery 정의 확인

        nativeQueryBuilder.withAggregation(AGG_FILTER_FRONTEND,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a.filter(frontendQuery)));
        nativeQueryBuilder.withAggregation(AGG_FILTER_BACKEND,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a.filter(backendQuery)));

        // 2. LogLevelDistribution Aggregation (기존과 동일)
        TermsAggregation logLevelTermsAggBuilder = TermsAggregation.of(t -> t
                .field("level.keyword")
                .size(10)
        );
        nativeQueryBuilder.withAggregation(AGG_LOG_LEVELS,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a.terms(logLevelTermsAggBuilder)));

        // 3. LogCountTrend Aggregation (시간대별) (기존과 동일)
//        DateHistogramAggregation logTrendDateHistogramAggBuilder = DateHistogramAggregation.of(d -> d
//                .field("timestamp")
//                .calendarInterval(CalendarInterval.Hour)
//                .timeZone(ZoneOffset.UTC.getId())
////                .minDocCount(0)
//                .extendedBounds(eb -> eb
//                        .min(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(startDateTimeUtc.toInstant().toEpochMilli()))))
//                        .max(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(endDateTimeUtc.minusNanos(1).toInstant().toEpochMilli()))))
//                )
//        );
//        nativeQueryBuilder.withAggregation(AGG_LOG_TREND,
//                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a.dateHistogram(logTrendDateHistogramAggBuilder)));

        // 4. TopErrors Aggregations (기존과 동일)
        // Frontend Errors
        TermsAggregation topFrontendErrorsTermsBuilder = TermsAggregation.of(t -> t
                .field("error.type.keyword")
                .size(5)
                .order(List.of(NamedValue.of("_count", SortOrder.Desc)))
        );
        Query frontendErrorFilterQuery = QueryBuilders.bool(b -> b
                .must(frontendQuery)
                .must(QueryBuilders.exists(e -> e.field("error.type.keyword")))
        );
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation frontendErrorsSubAgg =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(subAgg -> subAgg.terms(topFrontendErrorsTermsBuilder));

        nativeQueryBuilder.withAggregation(AGG_FILTER_FRONTEND_ERRORS,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .filter(frontendErrorFilterQuery)
                        .aggregations(AGG_TERMS_FRONTEND_ERRORS, frontendErrorsSubAgg)
                ));

        // Backend Errors
        TermsAggregation topBackendErrorsTermsBuilder = TermsAggregation.of(t -> t
                .field("error.className.keyword")
                .size(5)
                .order(List.of(NamedValue.of("_count", SortOrder.Desc)))
        );
        Query backendErrorFilterQuery = QueryBuilders.bool(b -> b
                .must(backendQuery)
                .must(QueryBuilders.exists(e -> e.field("error.className.keyword")))
        );
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation backendErrorsSubAgg =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(subAgg -> subAgg.terms(topBackendErrorsTermsBuilder));

        nativeQueryBuilder.withAggregation(AGG_FILTER_BACKEND_ERRORS,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .filter(backendErrorFilterQuery)
                        .aggregations(AGG_TERMS_BACKEND_ERRORS, backendErrorsSubAgg)
                ));
        // --- 기존 코드 끝 ---

        // 5. SlowBackendApis Aggregation (*** 수정된 부분 ***)
        // 서브 집계 정의 (이 부분은 기존과 동일)
        AverageAggregation avgResponseTimeAggBuilder = AverageAggregation.of(a -> a.field("http.responseTime"));
        MaxAggregation maxResponseTimeAggBuilder = MaxAggregation.of(m -> m.field("http.responseTime"));
        TermsAggregation apiMethodTermsAggBuilder = TermsAggregation.of(t -> t.field("http.method.keyword").size(1)); // 예시로 size(1) 추가

        co.elastic.clients.elasticsearch._types.aggregations.Aggregation avgResponseTimeSubAgg =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(sub -> sub.avg(avgResponseTimeAggBuilder));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation maxResponseTimeSubAgg =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(sub -> sub.max(maxResponseTimeAggBuilder));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation apiMethodSubAgg =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(sub -> sub.terms(apiMethodTermsAggBuilder));

        // Terms 집계와 그 하위 집계를 Aggregation.Builder를 사용하여 함께 정의
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation slowApisTermsAndSubAggs =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(aggBuilder -> aggBuilder
                        .terms(termsBuilder -> termsBuilder // termsBuilder는 TermsAggregation.Builder 타입
                                .field("http.requestUri.keyword")
                                .size(5)
                                .order(List.of(NamedValue.of(AGG_SUB_AVG_RESPONSE_TIME, SortOrder.Desc))) // 정렬 기준은 서브 집계 이름
                        )
                        // aggBuilder (Aggregation.Builder)의 컨텍스트에서 서브 집계들을 추가
                        .aggregations(AGG_SUB_AVG_RESPONSE_TIME, avgResponseTimeSubAgg)
                        .aggregations(AGG_SUB_MAX_RESPONSE_TIME, maxResponseTimeSubAgg)
                        .aggregations(AGG_SUB_API_METHOD, apiMethodSubAgg)
                );

        // 필터 쿼리 (기존과 동일)
        Query slowApisFilterQuery = QueryBuilders.bool(b -> b
                .must(backendQuery) // backendQuery는 메소드 상단에서 정의되어 있어야 합니다.
                .must(QueryBuilders.exists(e -> e.field("http.requestUri.keyword")))
                .must(QueryBuilders.exists(e -> e.field("http.responseTime")))
        );

        // 최종 집계를 NativeQueryBuilder에 추가
        nativeQueryBuilder.withAggregation(AGG_FILTER_SLOW_BACKEND_APIS,
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(filterAggBuilder -> filterAggBuilder
                        .filter(slowApisFilterQuery)
                        .aggregations(AGG_TERMS_SLOW_BACKEND_APIS, slowApisTermsAndSubAggs) // 여기 slowApisTermsAndSubAggs를 사용
                ));
    }

    private ReportResponse parseResultsAndBuildResponse(SearchHits<LogDocument> searchHits, String projectIdStr, String periodDescription) {
        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();
        ElasticsearchAggregations esAggs = null;
        if (aggregationsContainer instanceof ElasticsearchAggregations) {
            esAggs = (ElasticsearchAggregations) aggregationsContainer;
        }

        if (esAggs == null) {
            log.warn("Elasticsearch aggregations are null or not of expected type. Returning empty report sections.");
            // (이전과 동일한 빈 리포트 반환 로직)
            return ReportResponse.builder()
//                    .reportId(UUID.randomUUID().toString())
                    .projectId(projectIdStr)
                    .periodDescription(periodDescription)
                    .generatedAt(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .totalLogCounts(TotalLogCounts.builder().build())
                    .logLevelDistribution(LogLevelDistribution.builder().distribution(Collections.emptyList()).build())
//                    .logCountTrend(Collections.emptyList())
                    .topErrors(Collections.emptyList())
                    .slowBackendApis(Collections.emptyList())
                    .build();
        }

        // 1. TotalLogCounts (이전과 동일)
        long overallTotal = searchHits.getTotalHits();
        long frontendTotal = Optional.ofNullable(esAggs.get(AGG_FILTER_FRONTEND))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate)
                .filter(Aggregate::isFilter).map(agg -> agg.filter().docCount()).orElse(0L);
        long backendTotal = Optional.ofNullable(esAggs.get(AGG_FILTER_BACKEND))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate)
                .filter(Aggregate::isFilter).map(agg -> agg.filter().docCount()).orElse(0L);

        TotalLogCounts totalLogCounts = TotalLogCounts.builder()
                .overallTotal(overallTotal)
                .frontendTotal(frontendTotal)
                .backendTotal(backendTotal)
                .build();

        // 2. LogLevelDistribution (이전과 동일)
        List<LogLevelDistribution.LogLevelDetail> logLevelDetails = new ArrayList<>();
        long totalLogsInDistribution = 0L;
        Aggregate logLevelAggResult = Optional.ofNullable(esAggs.get(AGG_LOG_LEVELS))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate).orElse(null);

        if (logLevelAggResult != null && logLevelAggResult.isSterms()) {
            StringTermsAggregate sterms = logLevelAggResult.sterms();
            for (StringTermsBucket bucket : sterms.buckets().array()) {
                totalLogsInDistribution += bucket.docCount();
            }
            for (StringTermsBucket bucket : sterms.buckets().array()) {
                logLevelDetails.add(LogLevelDistribution.LogLevelDetail.builder()
                        .level(bucket.key().stringValue())
                        .count(bucket.docCount())
                        .percentage(totalLogsInDistribution > 0 ? (double) bucket.docCount() / totalLogsInDistribution * 100.0 : 0.0)
                        .build());
            }
        }
        LogLevelDistribution logLevelDistribution = LogLevelDistribution.builder()
                .distribution(logLevelDetails)
                .totalLogsInDistribution(totalLogsInDistribution > 0 ? totalLogsInDistribution : overallTotal)
                .build();

        // 3. LogCountTrend (이전과 동일)
        List<TimeSlotLogCount> logCountTrend = new ArrayList<>();
        Aggregate logTrendAggResult = Optional.ofNullable(esAggs.get(AGG_LOG_TREND))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate).orElse(null);
        if (logTrendAggResult != null && logTrendAggResult.isDateHistogram()) {
            DateHistogramAggregate dateHistogram = logTrendAggResult.dateHistogram();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            for (DateHistogramBucket bucket : dateHistogram.buckets().array()) {
                ZonedDateTime zdt = Instant.ofEpochMilli(bucket.key()).atZone(ZoneOffset.UTC);
                logCountTrend.add(TimeSlotLogCount.builder()
                        .timeSlot(zdt.format(formatter))
                        .count(bucket.docCount())
                        .build());
            }
        }

        // 4. TopErrors (이전과 동일, parseTopErrors 호출)
        List<TopErrorOccurrence> topErrors = new ArrayList<>();
        parseTopErrors(esAggs, AGG_FILTER_FRONTEND_ERRORS, AGG_TERMS_FRONTEND_ERRORS, "frontend", topErrors);
        parseTopErrors(esAggs, AGG_FILTER_BACKEND_ERRORS, AGG_TERMS_BACKEND_ERRORS, "backend", topErrors);

        topErrors.sort(Comparator.comparingLong(TopErrorOccurrence::getOccurrenceCount).reversed());
        List<TopErrorOccurrence> finalTopErrors = topErrors.stream().limit(5)
                .map(error -> TopErrorOccurrence.builder()
                        .rank(topErrors.indexOf(error) + 1)
                        .errorIdentifier(error.getErrorIdentifier())
                        .occurrenceCount(error.getOccurrenceCount())
                        .sourceOrigin(error.getSourceOrigin())
                        .build())
                .collect(Collectors.toList());

        // 5. SlowBackendApis (이전과 동일, Map<String, Aggregate> 사용 부분은 이미 수정됨)
        List<SlowApiEndpoint> slowBackendApis = new ArrayList<>();
        Aggregate slowApisFilterAgg = Optional.ofNullable(esAggs.get(AGG_FILTER_SLOW_BACKEND_APIS))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate).orElse(null);

        if (slowApisFilterAgg != null && slowApisFilterAgg.isFilter()) {
            Map<String, Aggregate> subAggregations = slowApisFilterAgg.filter().aggregations();
            if (subAggregations != null && subAggregations.containsKey(AGG_TERMS_SLOW_BACKEND_APIS)) {
                Aggregate slowApisTermsAgg = subAggregations.get(AGG_TERMS_SLOW_BACKEND_APIS);
                if (slowApisTermsAgg != null && slowApisTermsAgg.isSterms()) {
                    StringTermsAggregate sterms = slowApisTermsAgg.sterms();
                    int rank = 1;
                    for (StringTermsBucket bucket : sterms.buckets().array()) {
                        String requestPath = bucket.key().stringValue();
                        long totalRequests = bucket.docCount();

                        Map<String, Aggregate> bucketSubAggs = bucket.aggregations();
                        double avgResponseTime = Optional.ofNullable(bucketSubAggs.get(AGG_SUB_AVG_RESPONSE_TIME))
                                .filter(Aggregate::isAvg).map(a -> a.avg().value()).orElse(0.0);
                        double maxResponseTime = Optional.ofNullable(bucketSubAggs.get(AGG_SUB_MAX_RESPONSE_TIME))
                                .filter(Aggregate::isMax).map(a -> a.max().value()).orElse(0.0);

                        String httpMethod = "N/A";
                        Aggregate methodAggResult = bucketSubAggs.get(AGG_SUB_API_METHOD);
                        if (methodAggResult != null && methodAggResult.isSterms()) {
                            StringTermsAggregate methodSterms = methodAggResult.sterms();
                            if (!methodSterms.buckets().array().isEmpty()) {
                                httpMethod = methodSterms.buckets().array().get(0).key().stringValue();
                            }
                        }

                        slowBackendApis.add(SlowApiEndpoint.builder()
                                .rank(rank++)
                                .requestPath(requestPath)
                                .httpMethod(httpMethod)
                                .averageResponseTimeMs(Math.round(avgResponseTime))
                                .maxResponseTimeMs(Math.round(maxResponseTime))
                                .totalRequests(totalRequests)
                                .build());
                    }
                }
            }
        }

        return ReportResponse.builder()
//                .reportId(UUID.randomUUID().toString())
                .projectId(projectIdStr)
                .periodDescription(periodDescription)
                .generatedAt(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .totalLogCounts(totalLogCounts)
                .logLevelDistribution(logLevelDistribution)
//                .logCountTrend(logCountTrend)
                .topErrors(finalTopErrors)
                .slowBackendApis(slowBackendApis)
                .build();
    }

    private void parseTopErrors(ElasticsearchAggregations esAggs, String filterAggName, String termsAggName, String sourceOrigin, List<TopErrorOccurrence> topErrorsList) {
        Aggregate filterAgg = Optional.ofNullable(esAggs.get(filterAggName))
                .map(ElasticsearchAggregation::aggregation).map(org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate).orElse(null);

        if (filterAgg != null && filterAgg.isFilter()) {
            Map<String, Aggregate> subAggregations = filterAgg.filter().aggregations();
            if (subAggregations != null && subAggregations.containsKey(termsAggName)) {
                Aggregate termsAggResult = subAggregations.get(termsAggName);
                if (termsAggResult != null && termsAggResult.isSterms()) {
                    StringTermsAggregate sterms = termsAggResult.sterms();
                    for (StringTermsBucket bucket : sterms.buckets().array()) {
                        topErrorsList.add(TopErrorOccurrence.builder()
                                .errorIdentifier(bucket.key().stringValue())
                                .occurrenceCount(bucket.docCount())
                                .sourceOrigin(sourceOrigin)
                                .build());
                    }
                }
            }
        }
    }

    private ZonedDateTime parseDateTime(String dateTimeStr, ZoneId targetZone, boolean isStart) {
        // (이전과 동일)
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(targetZone)
                    .withZoneSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e1) {
            try {
                LocalDate date = LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDateTime dateTime = isStart ? date.atStartOfDay() : date.plusDays(1).atStartOfDay();
                return dateTime.atZone(targetZone)
                        .withZoneSameInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE)
                        .addParameter("dateTime", dateTimeStr)
                        .addParameter("reason", "Invalid date/time format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss.");
            }
        }
    }

    private String generatePeriodDescription(ZonedDateTime startDateTimeUtc, ZonedDateTime endDateTimeUtc, ZoneId displayZone) {
        // (이전과 동일)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        String startDateStr = startDateTimeUtc.withZoneSameInstant(displayZone).format(dateFormatter);
        String endDateStr = endDateTimeUtc.minusSeconds(1).withZoneSameInstant(displayZone).format(dateFormatter); // endDateTimeUtc는 exclusive이므로 -1초
        if (startDateStr.equals(endDateStr)) {
            return startDateStr;
        }
        return String.format("%s ~ %s", startDateStr, endDateStr);
    }

    @PostConstruct
    public void initializePlaywrightAndBrowsers() {
        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            Browser browser = chromium.launch(new BrowserType.LaunchOptions().setHeadless(true));
            log.info("Chromium 브라우저가 성공적으로 실행되었습니다. 버전: {}", browser.version());
            browser.close();
        } catch (PlaywrightException e) {
            log.error("애플리케이션 시작 중 Playwright 초기화 또는 브라우저 다운로드에 실패했습니다: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Playwright 초기화 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 제공된 HTML 콘텐츠를 사용하여 PDF를 생성합니다.
     * @param htmlContent PDF로 변환할 전체 HTML 문자열
     * @param projectId 현재 컨텍스트의 프로젝트 ID (로그나 파일명 등에 사용 가능)
     * @return 생성된 PDF의 byte 배열
     * @throws PlaywrightException PDF 생성 중 Playwright 관련 오류 발생 시
     * @throws IllegalArgumentException htmlContent가 null이거나 비어있을 경우
     */
    public byte[] generatePdfFromHtml(String htmlContent, Integer projectId) throws PlaywrightException, IllegalArgumentException {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            log.warn("PDF 생성을 위한 HTML 내용이 비어있거나 null입니다. 프로젝트 ID: {}", projectId);
            throw new IllegalArgumentException("HTML content cannot be null or empty for PDF generation.");
        }

        log.info("Playwright를 사용하여 PDF 생성을 시작합니다. 프로젝트 ID: {}, Frontend Base URL: {}", projectId, frontendBaseUrl);

        String effectiveBaseUrl = frontendBaseUrl;
        if (effectiveBaseUrl != null && !effectiveBaseUrl.endsWith("/")) {
            effectiveBaseUrl += "/";
        } else if (effectiveBaseUrl == null || effectiveBaseUrl.trim().isEmpty()) {
            log.error("Frontend Base URL이 설정되지 않았습니다! HTML 내 상대 경로 리소스 로딩에 문제가 발생할 수 있습니다. 기본 URL을 '/'로 사용합니다.");
            effectiveBaseUrl = "/"; // 또는 적절한 기본값, 혹은 예외 발생
        }

        String modifiedHtmlContent;
        if (htmlContent.toLowerCase().contains("<head>")) {
            modifiedHtmlContent = htmlContent.replaceFirst("(?i)<head>", "<head><base href=\"" + effectiveBaseUrl + "\">");
            log.debug("프로젝트 ID {}: <base href=\"{}\"> 태그가 <head> 내에 삽입되었습니다.", projectId, effectiveBaseUrl);
        } else if (htmlContent.toLowerCase().contains("<html>")){
            modifiedHtmlContent = htmlContent.replaceFirst("(?i)<html>", "<html><head><base href=\"" + effectiveBaseUrl + "\"></head>");
            log.debug("프로젝트 ID {}: <head>와 <base href=\"{}\"> 태그가 <html> 내에 삽입되었습니다.", projectId, effectiveBaseUrl);
        } else {
            log.warn("프로젝트 ID {}: HTML 내용에 <head> 또는 <html> 태그를 찾을 수 없어 <base> 태그를 포함한 기본 구조를 맨 앞에 추가합니다.", projectId);
            modifiedHtmlContent = "<!DOCTYPE html><html><head><base href=\"" + effectiveBaseUrl + "\"></head><body>" + htmlContent + "</body></html>";
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            log.debug("프로젝트 ID {}: Playwright 브라우저 및 새 페이지 생성 완료.", projectId);

            page.onPageError(exception -> {
                log.error("!!! 프로젝트 ID {}: Playwright Page JavaScript UNCAUGHT Exception (raw): {}", projectId, exception.toString());
            });
            page.onRequest(request -> log.debug(">> 프로젝트 ID {}: Playwright Request: METHOD=[{}], URL=[{}]", projectId, request.method(), request.url()));
            page.onResponse(response -> log.debug("<< 프로젝트 ID {}: Playwright Response: STATUS=[{}], METHOD=[{}], URL=[{}]", projectId, response.status(), response.request().method(), response.url()));
            page.onRequestFailed(request -> {
                String errorText = request.failure();
                log.warn("!! 프로젝트 ID {}: Playwright Request Failed: METHOD=[{}], URL=[{}], Error=[{}]", projectId, request.method(), request.url(), errorText != null ? errorText : "N/A");
            });
            page.onConsoleMessage(msg -> {
                String logType = msg.type().toLowerCase();
                if ("error".equals(logType)) {
                    log.error("BROWSER CONSOLE (Playwright) - 프로젝트 ID {}: type=[{}], text=[{}]", projectId, msg.type(), msg.text());
                } else if ("warning".equals(logType)) {
                    log.warn("BROWSER CONSOLE (Playwright) - 프로젝트 ID {}: type=[{}], text=[{}]", projectId, msg.type(), msg.text());
                }
            });

            // 1. 먼저 빈 페이지라도 frontendBaseUrl로 이동하여 페이지의 origin을 설정합니다.
            //    이렇게 하면 React Router가 초기 경로를 인식하는 데 도움이 될 수 있고,
            //    localStorage 접근 시 origin 'null' 문제를 피하는 데도 도움이 될 수 있습니다.
            //    (주의: 이 navigate 호출은 modifiedHtmlContent를 사용하는 setContent 전에 이루어져야 합니다.)
            //    waitUntil 옵션은 필요에 따라 DOMCONTENTLOADED 또는 LOAD로 조정할 수 있습니다.
            String initialPageLoadUrl = effectiveBaseUrl; // 기본적으로는 도메인 루트
            // 만약 리포트 페이지가 특정 경로를 가져야 한다면 해당 경로를 포함한 URL로 navigate 할 수도 있습니다.
            // String initialPageLoadUrl = effectiveBaseUrl + "report/" + projectId; // 예시
            // 하지만 이 경우, 이후 setContent가 이 내용을 덮어씁니다.
            // 따라서, 여기서는 origin 설정을 위해 도메인 루트로만 navigate 하는 것이 더 적절할 수 있습니다.

            log.debug("프로젝트 ID {}: 초기 컨텍스트 설정을 위해 {} 로 navigate 시도...", projectId, initialPageLoadUrl);
            page.navigate(initialPageLoadUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // 2. 그 다음, 실제 렌더링된 HTML 콘텐츠를 페이지에 설정합니다.
            //    <base> 태그는 modifiedHtmlContent 안에 이미 삽입되어 있으므로, 리소스 로딩에 사용됩니다.
            log.debug("프로젝트 ID {}: 수정된 HTML 콘텐츠로 페이지 내용 설정 시작...", projectId);
            page.setContent(modifiedHtmlContent, new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
            );
            log.debug("프로젝트 ID {}: HTML 콘텐츠 설정 완료.", projectId);


            // 3. (선택 사항, 여전히 "No routes matched location 'blank'"가 문제라면 시도)
            // setContent 후 React Router가 실제 의도된 경로를 인식하도록 URL을 다시 설정합니다.
            // replaceState는 history 스택에 새 항목을 만들지 않습니다.
            // 이 작업은 setContent 이후에 실행되어야 React 앱의 History 객체에 작용합니다.
            String currentReportPath = "/report/" + projectId; // 프론트엔드 라우팅 경로
            try {
                page.evaluate(String.format("window.history.replaceState({}, '', '%s')", currentReportPath));
            } catch (PlaywrightException e) {
                log.warn("프로젝트 ID {}: window.history.replaceState 실행 중 오류 발생 (무시하고 진행): {}", projectId, e.getMessage());
            }


            page.setViewportSize(1200, 800);
            log.debug("프로젝트 ID {}: 뷰포트 크기 설정 완료: 1200x800", projectId);

            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.SCREEN));
            log.debug("프로젝트 ID {}: 스크린 미디어 타입으로 에뮬레이트 완료.", projectId);

            // 라이트 모드로 강제 (dark 클래스 추가 안 함)
            log.debug("프로젝트 ID {}: 라이트 모드로 PDF 생성 (dark 클래스 미적용).", projectId);


            // 4. 실제 콘텐츠가 렌더링될 때까지 대기 (CSS 선택자 사용)
            //    ReportPage.tsx에서 실제 리포트 내용을 감싸는 div에 id="actual-report-content"를 추가했다고 가정합니다.
            String reportContentSelector = "#actual-report-content";
            try {
                log.debug("프로젝트 ID {}: '{}' 선택자가 나타날 때까지 대기 시작 (최대 15초)...", projectId, reportContentSelector);
                page.waitForSelector(reportContentSelector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            } catch (TimeoutError e) {
                log.warn("프로젝트 ID {}: '{}' 선택자 대기 시간(5초) 초과. 페이지 주요 콘텐츠가 렌더링되지 않았을 수 있습니다.", projectId, reportContentSelector);
            }


            try {
                log.debug("프로젝트 ID {}: 웹 폰트 로딩 대기 시작 (document.fonts.ready)...", projectId);
                page.waitForFunction("() => document.fonts.ready.then(() => true)", null, new Page.WaitForFunctionOptions().setTimeout(2500));
            } catch (TimeoutError e) {
                log.warn("프로젝트 ID {}: 폰트 로딩 대기 시간(2초) 초과. 일부 폰트가 제대로 로드되지 않았을 수 있습니다.", projectId);
            }

            String screenshotFilename = "pdf_debug_screenshot_project_" + projectId + "_" + System.currentTimeMillis() + ".png";
            String screenshotPath = "/tmp/" + screenshotFilename;
            try {
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)).setFullPage(true));
            } catch (PlaywrightException e) {
                log.error("프로젝트 ID {}: 스크린샷 저장 중 오류 발생: {}", projectId, e.getMessage(), e);
            }

            Page.PdfOptions pdfOptions = new Page.PdfOptions()
                    .setFormat("A4")
                    .setScale(0.55)
                    .setPrintBackground(true);
            log.debug("프로젝트 ID {}: PDF 생성 옵션 설정 완료.", projectId);

            log.debug("프로젝트 ID {}: PDF 데이터 생성 시작...", projectId);
            byte[] pdfBytes = page.pdf(pdfOptions);

            browser.close();
            log.debug("프로젝트 ID {}: Playwright 브라우저 종료.", projectId);
            return pdfBytes;

        } catch (PlaywrightException e) {
            log.error("프로젝트 ID {}: Playwright를 사용하여 PDF 생성 중 오류가 발생했습니다 (PlaywrightException): {}", projectId, e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("프로젝트 ID {}: 잘못된 인자로 PDF 생성 시도 (IllegalArgumentException): {}", projectId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("프로젝트 ID {}: PDF 생성 중 알 수 없는 오류 발생 (Exception): {}", projectId, e.getMessage(), e);
            throw new PlaywrightException("Unknown error during PDF generation for project " + projectId + ": " + e.getMessage(), e);
        }
    }
}