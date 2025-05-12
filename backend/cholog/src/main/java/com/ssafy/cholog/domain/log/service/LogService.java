package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ssafy.cholog.domain.log.dto.response.LogEntryResponse;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.global.common.CustomPage;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public CustomPage<LogEntryResponse> getProjectAllLog(Integer userId, Integer projectId, Pageable pageable) {

        projectUserRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                        .addParameter("userId", userId)
                        .addParameter("projectId", projectId));

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

    public LogEntryResponse getLogDetail(String projectId, String logId) {
        // projectId로 index를 조회하고
        // index와 id로 조회하여 단건을 가져옴
        Project project = projectRepository.findById(Integer.parseInt(projectId))
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));
        String projectToken = project.getProjectToken();
        String indexName = "pjt-" + projectToken;
        // indexName과 logId로 단건 조회
        LogDocument logDocument = elasticsearchOperations.get(logId, LogDocument.class, IndexCoordinates.of(indexName));
        if (logDocument == null) {
            throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                    .addParameter("projectId", projectId)
                    .addParameter("logId", logId);
        }
        return LogEntryResponse.fromLogDocument(logDocument);
    }
}