package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.ssafy.cholog.domain.log.dto.response.LogEntryResponse;
import com.ssafy.cholog.domain.log.dto.response.LogListResponse;
import com.ssafy.cholog.domain.log.dto.response.LogStatsResponse;
import com.ssafy.cholog.domain.log.dto.response.LogTimelineResponse;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.log.entity.LogListDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.global.common.CustomPage;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    public static final String INDEX_PREFIX = "pjt-*-";

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public CustomPage<LogListResponse> getProjectAllLog(Integer userId, Integer projectId, Pageable pageable) {

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        String indexName = INDEX_PREFIX + projectToken;

        // 1. Elasticsearch Query 객체 생성 (새로운 방식)
        co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl =
                QueryBuilders.exists(e -> e.field("message")); // 필드가 존재하는지 확인하는 쿼리
//                QueryBuilders.matchAll(m -> m); // MatchAllQuery

        // 2. SortOptions 리스트 생성 (새로운 방식)
        List<SortOptions> sortOptionsList = new ArrayList<>();
        pageable.getSort().forEach(order -> {
            final String finalProperty = order.getProperty(); // for lambda
            sortOptionsList.add(
                    SortOptions.of(so -> so
                            .field(f -> f
                                    .field(finalProperty)
                                    .order(order.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc)
                            )
                    )
            );
        });

        sortOptionsList.add(
                SortOptions.of(so -> so
                        .field(f -> f
                                .field("sequence") // 새로 추가한 sequence 필드
                                .order(SortOrder.Desc) // 동일 timestamp 내에서는 순서대로
                                .missing("_first") // sequence 없는 옛날 로그 처리 (필요시)
                        )
                )
        );

        // 3. NativeQuery 객체 빌드
        Query searchQuery = NativeQuery.builder()
                .withQuery(esQueryDsl) // co.elastic.clients.elasticsearch._types.query_dsl.Query 객체 전달
                .withPageable(pageable)
                .withSort(sortOptionsList) // List<SortOptions> 전달
                // 필요하다면 .withTrackTotalHits(true) 등을 추가할 수 있습니다. (기본적으로 true일 수 있음)
                .build();

        SearchHits<LogListDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogListDocument.class,
                IndexCoordinates.of(indexName)
        );

        List<LogListResponse> logEntries = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(LogListResponse::fromLogListDocument)
                .collect(Collectors.toList());

        Page<LogListResponse> page = new PageImpl<>(logEntries, pageable, searchHits.getTotalHits());
        return new CustomPage<>(page);
    }

    public LogEntryResponse getLogDetail(Integer projectId, String logId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        // INDEX_PREFIX가 "pjt-*-"라고 가정하면, indexPattern은 "pjt-*-<projectToken>"이 됩니다.
        // 이 패턴은 search API에 유효합니다.
        String indexPattern = INDEX_PREFIX + projectToken;

        // ID로 문서를 검색하는 쿼리 생성
        co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl = QueryBuilders.ids(iq -> iq
                .values(logId)
        );

        Query searchQuery = NativeQuery.builder()
                .withQuery(esQueryDsl)
                .withMaxResults(1) // ID는 고유하므로 최대 1개의 결과만 필요
                .build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexPattern) // 와일드카드가 포함된 인덱스 패턴 사용
        );

        if (searchHits.getTotalHits() == 0 || !searchHits.hasSearchHits()) {
            throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                    .addParameter("projectId", projectId)
                    .addParameter("logId", logId);
        }

        // 첫 번째 검색 결과에서 LogDocument를 가져옵니다.
        LogDocument logDocument = searchHits.getSearchHit(0).getContent();

        return LogEntryResponse.fromLogDocument(logDocument);
    }

    public List<LogListResponse> getLogByTraceId(Integer userId, Integer projectId, String traceId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId", projectId));

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        String projectToken = project.getProjectToken();
        String indexName = INDEX_PREFIX + projectToken;

        List<SortOptions> sortOptionsList = new ArrayList<>();
        SortOptions timestampSort = SortOptions.of(so -> so
                .field(f -> f
                        .field("timestamp")
                        .order(SortOrder.Asc)
                )
        );

        SortOptions sequenceSort = SortOptions.of(so -> so
                .field(f -> f
                        .field("sequence")
                        .order(SortOrder.Asc)
                        .missing("_first") // sequence 없는 옛날 로그 처리 (필요시)
                )
        );
        sortOptionsList.add(timestampSort);
        sortOptionsList.add(sequenceSort);

        // 1. requestId에 대한 term 쿼리
        co.elastic.clients.elasticsearch._types.query_dsl.Query termQuery = QueryBuilders.term(t -> t
                .field("requestId.keyword")
                .value(traceId)
        );

        // 2. message 필드 존재 여부 쿼리
        co.elastic.clients.elasticsearch._types.query_dsl.Query existsQuery = QueryBuilders.exists(e -> e
                .field("message")
        );

        // 3. 두 쿼리를 bool 쿼리로 결합 (filter 사용)
        co.elastic.clients.elasticsearch._types.query_dsl.Query finalQuery = QueryBuilders.bool(b -> b
                .filter(termQuery)
                .filter(existsQuery)
        );

        org.springframework.data.elasticsearch.core.query.Query searchQuery = NativeQuery.builder() // 변수 타입을 명시적으로 변경
                .withQuery(finalQuery) // 수정된 쿼리 적용
                .withSort(sortOptionsList)
                .withMaxResults(20) // Elasticsearch 기본 반환 개수 제한(100) 고려, 필요시 조절
                .build();

        SearchHits<LogListDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogListDocument.class,
                IndexCoordinates.of(indexName)
        );

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(LogListResponse::fromLogListDocument)
                .collect(Collectors.toList());
    }

    public LogStatsResponse getProjectLogStats(Integer userId, Integer projectId) {
        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));
        String projectToken = project.getProjectToken();
        String indexName = INDEX_PREFIX + projectToken;

        String levelCountsAggregationName = "level_counts";
        TermsAggregation esTermsAggregation = TermsAggregation.of(ta -> ta.field("level.keyword"));

        co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl =
//                QueryBuilders.matchAll(m -> m);
                QueryBuilders.exists(e -> e.field("message")); // 메시지 없는거 제외

        Query searchQuery = NativeQuery.builder()
                .withQuery(esQueryDsl)
                .withAggregation(levelCountsAggregationName, co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(agg -> agg.terms(esTermsAggregation)))
                .withMaxResults(0)
                .withTrackTotalHits(true)
                .build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexName)
        );

        long total = searchHits.getTotalHits();
        int trace = 0;
        int debug = 0;
        int info = 0;
        int warn = 0;
        int error = 0;
        int fatal = 0;

        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();

        if (aggregationsContainer instanceof ElasticsearchAggregations esAggsImpl) {
            ElasticsearchAggregation levelCountAggregationWrapper = esAggsImpl.get(levelCountsAggregationName);

            if (levelCountAggregationWrapper != null) {
                Aggregation sdeInternalAggregation = levelCountAggregationWrapper.aggregation();

                Aggregate elcAggregate = sdeInternalAggregation.getAggregate();

                if (elcAggregate.isSterms()) {
                    StringTermsAggregate sterms = elcAggregate.sterms();
                    for (StringTermsBucket bucket : sterms.buckets().array()) {
                        String level = bucket.key().stringValue();
                        long count = bucket.docCount();
                        switch (level.toUpperCase()) {
                            case "TRACE": trace = (int) count; break;
                            case "DEBUG": debug = (int) count; break;
                            case "INFO":  info  = (int) count; break;
                            case "WARN":  warn  = (int) count; break;
                            case "ERROR": error = (int) count; break;
                            case "FATAL": fatal = (int) count; break;
                        }
                    }
                }
            }
        }

        return LogStatsResponse.of(total, trace, debug, info, warn, error, fatal);
    }

    public void createIndex(String projectToken) {

        String lowerCaseToken = projectToken.toLowerCase();
        String frontIndexName = "pjt-fe-" + lowerCaseToken;
        String backIndexName = "pjt-be-" + lowerCaseToken;

        // 공통으로 사용할 설정
        Map<String, Object> settings = new HashMap<>();
        settings.put("index.number_of_shards", "1");
        settings.put("index.number_of_replicas", "0");

        try {
            // --- 프론트엔드 인덱스 처리 ---
            IndexCoordinates frontIndexCoordinates = IndexCoordinates.of(frontIndexName);
            IndexOperations frontIndexOps = elasticsearchOperations.indexOps(frontIndexCoordinates);

            if (!frontIndexOps.exists()) {
                frontIndexOps.create(settings);
//                frontIndexOps.putMapping(LogDocument.class);
            } else {
                log.info("Elasticsearch frontend index {} already exists. Skipping creation.", frontIndexName);
            }

            // --- 백엔드 인덱스 처리 ---
            IndexCoordinates backIndexCoordinates = IndexCoordinates.of(backIndexName);
            IndexOperations backIndexOps = elasticsearchOperations.indexOps(backIndexCoordinates);

            if (!backIndexOps.exists()) {
                backIndexOps.create(settings);
//                backIndexOps.putMapping(LogDocument.class);
            } else {
                log.info("Elasticsearch backend index {} already exists. Skipping creation.", backIndexName);
            }
        } catch (Exception e) {
            // 어떤 인덱스에서 오류가 발생했는지 특정하기 어려우므로, projectToken 레벨에서 로깅
            log.error("Failed during creation/configuration of Elasticsearch indices for project token {}. Error: {}",
                    projectToken, e.getMessage(), e);
            throw new CustomException(ErrorCode.INDEX_CREATE_FAIL);
        }
    }

    public List<LogTimelineResponse> getProjectLogTimeline(Integer userId, Integer projectId, String startDateStr, String endDateStr) {

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        final ZonedDateTime startDateTimeBoundaryUtc;
        final LocalDate startDateForCompare;

        try {
            LocalDateTime parsedLdtStart = null;
            try {
                // 1. LocalDateTime (yyyy-MM-ddTHH:mm:ss) 형식으로 파싱 시도
                parsedLdtStart = LocalDateTime.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // 실패해도 괜찮음, 다음 단계에서 LocalDate로 파싱 시도
            }

            if (parsedLdtStart != null) { // LocalDateTime 파싱 성공
                startDateTimeBoundaryUtc = parsedLdtStart.atZone(ZoneOffset.UTC);
                startDateForCompare = parsedLdtStart.toLocalDate();
            } else { // LocalDateTime 파싱 실패 시, LocalDate (yyyy-MM-dd) 형식으로 파싱 시도
                LocalDate parsedLdStart = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE); // 여기서 실패하면 외부 catch로
                startDateTimeBoundaryUtc = parsedLdStart.atStartOfDay(ZoneOffset.UTC);
                startDateForCompare = parsedLdStart;
            }
        } catch (DateTimeParseException ex) { // LocalDate 파싱마저 실패한 경우 (즉, 두 형식 모두 아님)
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE)
                    .addParameter("startDate", startDateStr)
                    .addParameter("reason", "유효하지 않은 시작 날짜 형식입니다. yyyy-MM-dd 또는 yyyy-MM-ddTHH:mm:ss 형식을 사용해주세요.");
        }

        final ZonedDateTime endDateTimeBoundaryUtc;
        final LocalDate endDateForCompare;

        try {
            LocalDateTime parsedLdtEnd = null;
            try {
                // 1. LocalDateTime (yyyy-MM-ddTHH:mm:ss) 형식으로 파싱 시도
                parsedLdtEnd = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // 실패해도 괜찮음, 다음 단계에서 LocalDate로 파싱 시도
            }

            if (parsedLdtEnd != null) { // LocalDateTime 파싱 성공
                endDateTimeBoundaryUtc = parsedLdtEnd.atZone(ZoneOffset.UTC); // 시간 명시된 경우 해당 시간 사용
                endDateForCompare = parsedLdtEnd.toLocalDate();
            } else { // LocalDateTime 파싱 실패 시, LocalDate (yyyy-MM-dd) 형식으로 파싱 시도
                LocalDate parsedLdEnd = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE); // 여기서 실패하면 외부 catch로
                endDateTimeBoundaryUtc = parsedLdEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC); // 날짜만 명시 시, 다음날 00:00 UTC (lt 조건용)
                endDateForCompare = parsedLdEnd;
            }
        } catch (DateTimeParseException ex) { // LocalDate 파싱마저 실패한 경우 (즉, 두 형식 모두 아님)
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE)
                    .addParameter("endDate", endDateStr)
                    .addParameter("reason", "유효하지 않은 종료 날짜 형식입니다. yyyy-MM-dd 또는 yyyy-MM-ddTHH:mm:ss 형식을 사용해주세요.");
        }

        // 날짜 순서 유효성 검사
        if (startDateForCompare.isAfter(endDateForCompare)) {
            throw new CustomException(ErrorCode.LOG_START_TIME_AFTER_END_TIME)
                    .addParameter("startDate", startDateStr)
                    .addParameter("endDate", endDateStr);
        }

        // Elasticsearch 쿼리 로직 시작
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));
        String indexName = INDEX_PREFIX + project.getProjectToken();
        String timestampFieldName = "timestamp";

        // Elasticsearch Range Query 생성 (final 변수 사용)
        co.elastic.clients.elasticsearch._types.query_dsl.Query elcRangeQuery =
                co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.of(r -> r
                        .field(timestampFieldName)
                        .gte(JsonData.of(startDateTimeBoundaryUtc.toInstant().toEpochMilli()))
                        .lt(JsonData.of(endDateTimeBoundaryUtc.toInstant().toEpochMilli()))
                )._toQuery();

        // 2. message 필드 존재 여부 쿼리
        co.elastic.clients.elasticsearch._types.query_dsl.Query existsQuery = QueryBuilders.exists(e -> e
                .field("message")
        );

        // 3. 두 쿼리를 bool 쿼리로 결합 (filter 사용)
        co.elastic.clients.elasticsearch._types.query_dsl.Query finalQuery = QueryBuilders.bool(b -> b
                .filter(elcRangeQuery)
                .filter(existsQuery)
        );

        // Date Histogram Aggregation 생성 (final 변수 사용)
        String timelineAggregationName = "log_timeline_by_hour";
        DateHistogramAggregation dateHistogramAgg = DateHistogramAggregation.of(dh -> dh
                .field(timestampFieldName)
                .calendarInterval(CalendarInterval.Hour) // 1시간 간격
                .minDocCount(1) // 로그가 없는 시간대도 0으로 표시
                .extendedBounds(eb -> eb // 집계 범위를 명시적으로 설정하여 빈 구간도 포함
                        .min(FieldDateMath.of(fdmBuilder -> fdmBuilder.expr(String.valueOf(startDateTimeBoundaryUtc.toInstant().toEpochMilli()))))
                        .max(FieldDateMath.of(fdmBuilder -> fdmBuilder.expr(String.valueOf(
                                endDateTimeBoundaryUtc.minusNanos(1)
                                        .truncatedTo(ChronoUnit.HOURS)
                                        .toInstant().toEpochMilli()
                        ))))
                )
        );

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withAggregation(timelineAggregationName, co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(agg -> agg.dateHistogram(dateHistogramAgg)))
                .withMaxResults(0) // 집계 결과만 필요
                .build();

        SearchHits<LogListDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogListDocument.class,
                IndexCoordinates.of(indexName)
        );

        List<LogTimelineResponse> timelineResponses = new ArrayList<>();
        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();

        if (aggregationsContainer instanceof ElasticsearchAggregations esAggsImpl) {
            ElasticsearchAggregation timelineAggregationWrapper = esAggsImpl.get(timelineAggregationName);
            if (timelineAggregationWrapper != null) {
                Aggregation sdeInternalAggregation = timelineAggregationWrapper.aggregation();
                Aggregate elcAggregate = sdeInternalAggregation.getAggregate();
                if (elcAggregate.isDateHistogram()) {
                    DateHistogramAggregate dateHistogram = elcAggregate.dateHistogram();
                    for (DateHistogramBucket bucket : dateHistogram.buckets().array()) {
                        long epochMillis = bucket.key();
//                        LocalDateTime bucketTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
                        LocalDateTime bucketTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("Asia/Seoul"));
                        Integer logCount = (int) bucket.docCount();

                        timelineResponses.add(LogTimelineResponse.builder()
                                .timestamp(bucketTimestamp)
                                .logCount(logCount)
                                .build());
                    }
                }
            }
        }
        return timelineResponses;
    }
}