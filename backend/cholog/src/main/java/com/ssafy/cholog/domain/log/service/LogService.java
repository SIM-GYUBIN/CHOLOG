package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.ssafy.cholog.domain.log.dto.response.LogEntryResponse;
import com.ssafy.cholog.domain.log.dto.response.LogStatsResponse;
import com.ssafy.cholog.domain.log.dto.response.LogTimelineResponse;
import com.ssafy.cholog.domain.log.entity.LogDocument;
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
import org.springframework.util.StringUtils;

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

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public CustomPage<LogEntryResponse> getProjectAllLog(Integer userId, Integer projectId, Pageable pageable) {

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        String indexName = "pjt-" + projectToken;

        // 1. Elasticsearch Query 객체 생성 (새로운 방식)
        co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl =
                QueryBuilders.matchAll(m -> m); // MatchAllQuery

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

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexName)
        );

        List<LogEntryResponse> logEntries = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(LogEntryResponse::fromLogDocument)
                .collect(Collectors.toList());

        Page<LogEntryResponse> page = new PageImpl<>(logEntries, pageable, searchHits.getTotalHits());
        return new CustomPage<>(page);
    }

    public LogEntryResponse getLogDetail(Integer projectId, String logId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        String indexName = "pjt-" + projectToken;

        LogDocument logDocument = elasticsearchOperations.get(logId, LogDocument.class, IndexCoordinates.of(indexName));
        if (logDocument == null) {
            throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                    .addParameter("projectId", projectId)
                    .addParameter("logId", logId);
        }
        return LogEntryResponse.fromLogDocument(logDocument);
    }

    public List<LogEntryResponse> getLogByTraceId(Integer userId, Integer projectId, String traceId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId", projectId));

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        String projectToken = project.getProjectToken();
        String indexName = "pjt-" + projectToken;

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

        Query searchQuery = NativeQuery.builder()
                .withQuery(QueryBuilders.term(t -> t
                        .field("traceId.keyword")
                        .value(traceId)
                ))
                .withSort(sortOptionsList)
                .build();
        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexName)
        );

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(LogEntryResponse::fromLogDocument)
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
        String indexName = "pjt-" + projectToken;

        String levelCountsAggregationName = "level_counts";
        TermsAggregation esTermsAggregation = TermsAggregation.of(ta -> ta.field("level.keyword"));

        co.elastic.clients.elasticsearch._types.query_dsl.Query esQueryDsl =
                QueryBuilders.matchAll(m -> m);

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

        String indexName = "pjt-" + projectToken.toLowerCase();

        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates);

        try {
            if (!indexOps.exists()) {
                log.info("Elasticsearch index {} 생성 시작", indexName);

                Map<String, Object> settings = new HashMap<>();
                settings.put("index.number_of_shards", "1");
                settings.put("index.number_of_replicas", "0");

                indexOps.create(settings);

                // LogDocument 클래스의 @Field 어노테이션을 기반으로 매핑 정보 적용
                indexOps.putMapping(LogDocument.class);

            } else {
                log.info("Elasticsearch index {} already exists. Skipping creation.", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to create or configure Elasticsearch index {}. Error: {}", indexName, e.getMessage(), e);
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
        String indexName = "pjt-" + project.getProjectToken();
        String timestampFieldName = "timestamp";

        // Elasticsearch Range Query 생성 (final 변수 사용)
        co.elastic.clients.elasticsearch._types.query_dsl.Query elcRangeQuery =
                co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.of(r -> r
                        .field(timestampFieldName)
                        .gte(JsonData.of(startDateTimeBoundaryUtc.toInstant().toEpochMilli()))
                        .lt(JsonData.of(endDateTimeBoundaryUtc.toInstant().toEpochMilli()))
                )._toQuery();

        // Date Histogram Aggregation 생성 (final 변수 사용)
        String timelineAggregationName = "log_timeline_by_hour";
        DateHistogramAggregation dateHistogramAgg = DateHistogramAggregation.of(dh -> dh
                .field(timestampFieldName)
                .calendarInterval(CalendarInterval.Hour) // 1시간 간격
                .minDocCount(0) // 로그가 없는 시간대도 0으로 표시
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
                .withQuery(elcRangeQuery)
                .withAggregation(timelineAggregationName, co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(agg -> agg.dateHistogram(dateHistogramAgg)))
                .withMaxResults(0) // 집계 결과만 필요
                .build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
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
                        LocalDateTime bucketTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
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