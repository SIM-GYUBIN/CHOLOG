package com.ssafy.cholog.domain.log.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ssafy.cholog.domain.log.dto.response.LogListResponse;
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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogSearchService {

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public CustomPage<LogListResponse> searchLog(Integer userId, Integer projectId, String level, String apiPath, String message, String source, Pageable pageable) {

        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        String frontendIndexName = "pjt-fe-" + projectToken;
        String backendIndexName = "pjt-be-" + projectToken;
        String[] targetIndices = {frontendIndexName, backendIndexName};

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // message 필드가 반드시 존재(null이 아님)하는 문서만 검색 대상에 포함
        boolQueryBuilder.must(QueryBuilders.exists(e -> e.field("message")));

        if (StringUtils.hasText(level)) {
            boolQueryBuilder.must(QueryBuilders.term(t -> t
                    .field("level")
                    .value(level)
            ));
        }
        if (StringUtils.hasText(apiPath)) {
            boolQueryBuilder.must(QueryBuilders.wildcard(w -> w
                    .field("http.requestUri")
                    .value("*" + apiPath + "*") // 성능 모니터링 필요
                    .caseInsensitive(true)
            ));
        }
        if (StringUtils.hasText(message)) {
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefix(m -> m
                    .field("message")
                    .query(message)
            ));
        }

        if (StringUtils.hasText(source)) {
            boolQueryBuilder.must(QueryBuilders.match(m -> m
                    .field("source")
                    .query(source)
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

        SearchHits<LogListDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogListDocument.class,
                IndexCoordinates.of(targetIndices)
        );

        List<LogListResponse> logEntries = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(LogListResponse::fromLogListDocument)
                .collect(Collectors.toList());

        Page<LogListResponse> page = new PageImpl<>(logEntries, pageable, searchHits.getTotalHits());
        return new CustomPage<>(page);
    }
}
