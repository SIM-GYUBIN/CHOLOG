package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ssafy.cholog.domain.log.dto.response.LogEntryResponse;
import com.ssafy.cholog.domain.log.dto.response.LogStatsResponse;
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
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    public CustomPage<LogEntryResponse> searchLog(Integer userId, Integer projectId, String level, String apiPath, String message, Pageable pageable) {

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

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(level)) {
            boolQueryBuilder.must(QueryBuilders.term(t -> t
                    .field("level")
                    .value(level)
            ));
        }
        if (StringUtils.hasText(apiPath)) {
            boolQueryBuilder.must(QueryBuilders.wildcard(w -> w
                    .field("http.request.url.keyword")
                    .value("*" + apiPath + "*") // 성능 모니터링 필요
                    .caseInsensitive(true)
            ));
        }
        if (StringUtils.hasText(message)) {
            boolQueryBuilder.must(QueryBuilders.match(m -> m
                    .field("message")
                    .query(message)
            ));
        }

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

        org.springframework.data.elasticsearch.core.query.Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .withSort(sortOptionsList)
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

        if (aggregationsContainer instanceof ElasticsearchAggregations) {
            ElasticsearchAggregations esAggsImpl = (ElasticsearchAggregations) aggregationsContainer;
            ElasticsearchAggregation levelCountAggregationWrapper = esAggsImpl.get(levelCountsAggregationName);

            if (levelCountAggregationWrapper != null) {
                Aggregation sdeInternalAggregation = levelCountAggregationWrapper.aggregation();

                if (sdeInternalAggregation != null) {
                    Aggregate elcAggregate = sdeInternalAggregation.getAggregate();

                    if (elcAggregate != null && elcAggregate.isSterms()) {
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
}