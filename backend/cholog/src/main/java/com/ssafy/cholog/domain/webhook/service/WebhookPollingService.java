package com.ssafy.cholog.domain.webhook.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import com.ssafy.cholog.domain.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPollingService {

    private final WebhookRepository webhookRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final MattermostService mattermostService;

    @Value("${cholog.notification.poll.rate}")
    private String pollRateConfig;

    @Value("${cholog.notification.poll.initialLookbackMinutes}")
    private int initialLookbackMinutes;

    @Value("${cholog.notification.poll.queryResultLimit}")
    private int queryResultLimit;

    @Scheduled(fixedRateString = "${cholog.notification.poll.rate}")
    @Transactional
    public void pollAndNotify() {
        List<Webhook> activeWebhooks = webhookRepository.findByIsEnabledTrue();
        if (activeWebhooks.isEmpty()) {
            log.debug("No active webhooks found.");
            return;
        }

        LocalDateTime currentPollExecutionTime = LocalDateTime.now(ZoneOffset.UTC);
        List<Webhook> webhooksToUpdate = new ArrayList<>(); // 업데이트할 웹훅들을 담을 리스트

        for (Webhook webhookSetting : activeWebhooks) {
            if (webhookSetting.getIsEnabled() == null || !webhookSetting.getIsEnabled()) {
                log.debug("Webhook ID {} is disabled. Skipping.", webhookSetting.getId());
                continue;
            }
            try {
                processSingleWebhook(webhookSetting, currentPollExecutionTime);
                webhooksToUpdate.add(webhookSetting);
            } catch (Exception e) {
                log.error("Error processing webhook ID {}: {}", webhookSetting.getId(), e.getMessage(), e);
            }
        }

        if (!webhooksToUpdate.isEmpty()) {
            webhookRepository.saveAll(webhooksToUpdate);
        } else {
            log.info("No webhooks were processed or needed updates in this cycle.");
        }
    }

    protected void processSingleWebhook(Webhook webhookSetting, LocalDateTime currentPollExecutionTime) {
        LocalDateTime queryStartTime;
        if (webhookSetting.getLastCheckedTimestamp() == null) {
            queryStartTime = currentPollExecutionTime.minusMinutes(initialLookbackMinutes);
            log.info("Initial poll or null lastCheckedTimestamp for webhook ID {}. Querying logs since {} (last {} minutes).",
                    webhookSetting.getId(), queryStartTime, initialLookbackMinutes);
        } else {
            queryStartTime = webhookSetting.getLastCheckedTimestamp();
        }

        Project project = webhookSetting.getProject();
        if (project == null || !StringUtils.hasText(project.getProjectToken())) {
            log.warn("Project or Project Token is missing for webhook ID {}. Updating lastCheckedTimestamp and skipping.", webhookSetting.getId());
            webhookSetting.updateLastCheckedTimestamp(currentPollExecutionTime);
            return;
        }
        String projectKeyForQuery = project.getProjectToken();

        // --- 키워드 파싱 ---
        String keywordsString = webhookSetting.getKeywords();
        List<String> parsedKeywords = new ArrayList<>();
        if (StringUtils.hasText(keywordsString)) {
            String[] rawKeywords = keywordsString.split(",");
            for (String k : rawKeywords) {
                String trimmedKeyword = k.trim().toLowerCase(); // 비교를 위해 소문자로 저장
                if (StringUtils.hasText(trimmedKeyword)) {
                    parsedKeywords.add(trimmedKeyword);
                }
            }
        }

        if (parsedKeywords.isEmpty()) {
            log.warn("No valid keywords configured for webhook ID {}. Skipping this setting.", webhookSetting.getId());
            webhookSetting.updateLastCheckedTimestamp(currentPollExecutionTime);
            return;
        }
        // --- 키워드 파싱 끝 ---

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.must(QueryBuilders.match(m -> m.field("projectKey").query(projectKeyForQuery)));

        // 키워드 OR 검색 조건 추가
        BoolQuery.Builder keywordShouldClauses = new BoolQuery.Builder();
        for (String keyword : parsedKeywords) {
            keywordShouldClauses.should(QueryBuilders.match(m -> m
                    .field("message")
                    .query(keyword)
            ));
            // 필요시 error.message, error.stacktrace 등 다른 필드에도 OR 조건 추가 가능
            // keywordShouldClauses.should(QueryBuilders.match(m -> m.field("error.message").query(keyword)));
            // keywordShouldClauses.should(QueryBuilders.match(m -> m.field("error.stacktrace").query(keyword)));
        }
        keywordShouldClauses.minimumShouldMatch("1");
        boolQueryBuilder.must(q -> q.bool(keywordShouldClauses.build()));

        long queryStartTimeMillis = queryStartTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        boolQueryBuilder.filter(QueryBuilders.range(r -> r.field("@timestamp").gt(JsonData.of(queryStartTimeMillis))));

        String envTagToFilter = webhookSetting.getNotificationENV();
        if (StringUtils.hasText(envTagToFilter)) {
            boolQueryBuilder.must(QueryBuilders.match(m -> m.field("environment").query(envTagToFilter)));
            log.debug("Applying 'environment' filter for webhook ID {}: {}", webhookSetting.getId(), envTagToFilter);
        }

        String indexName = "pjt-*-" + project.getProjectToken();

        org.springframework.data.elasticsearch.core.query.Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withSort(Sort.by(Sort.Direction.ASC, "@timestamp"))
                .withPageable(PageRequest.of(0, queryResultLimit))
                .build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                LogDocument.class,
                IndexCoordinates.of(indexName)
        );
//        log.debug("Found {} logs for webhook ID {} from index/pattern '{}'",
//                searchHits.getTotalHits(), webhookSetting.getId(), indexName);

        LocalDateTime latestLogTimestampForUpdate = null;

        if (searchHits.hasSearchHits()) {
            for (SearchHit<LogDocument> hit : searchHits) {
                LogDocument logDoc = hit.getContent();
                String esIndex = hit.getIndex();

                // --- 어떤 키워드가 매칭되었는지 확인 ---
                List<String> actuallyMatchedKeywords = new ArrayList<>();
                String logMessageText = ""; // 여러 필드를 합쳐서 검색할 텍스트
                if (StringUtils.hasText(logDoc.getMessage())) {
                    logMessageText += logDoc.getMessage().toLowerCase() + " ";
                }
                if (logDoc.getError() != null) {
                    if (StringUtils.hasText(logDoc.getError().getMessage())) {
                        logMessageText += logDoc.getError().getMessage().toLowerCase() + " ";
                    }
                    if (StringUtils.hasText(logDoc.getError().getStacktrace())) {
                        logMessageText += logDoc.getError().getStacktrace().toLowerCase();
                    }
                }

                if (StringUtils.hasText(logMessageText)) {
                    for (String keyword : parsedKeywords) { // parsedKeywords는 이미 소문자
                        if (logMessageText.contains(keyword)) {
                            // 매칭된 키워드는 원본 대소문자를 가진 키워드로 넣고 싶다면,
                            // parsedKeywords 생성 시 Map<String_lowercase, String_original> 형태로 관리 필요.
                            // 여기서는 그냥 파싱된 (소문자) 키워드를 추가.
                            actuallyMatchedKeywords.add(keyword);
                        }
                    }
                }
                // --- 매칭된 키워드 확인 로직 끝 ---

                // 매칭된 키워드가 있을 경우에만 알림을 보내도록 할 수 있음 (선택적 강화)
                if (actuallyMatchedKeywords.isEmpty() && !parsedKeywords.isEmpty()) {
                    // ES 쿼리는 통과했지만, Java 로직에서 매칭된 키워드가 없는 경우 (예: ES analyzer와 Java String.contains 동작 차이)
                    // 이 경우는 드물지만, 발생한다면 로깅.
                    log.warn("Log Doc ID {} matched ES query for webhook ID {} but no keywords matched in Java logic. ES_Query_Keywords: {}, Log_Content_Snippet: {}",
                            logDoc.getId(), webhookSetting.getId(), parsedKeywords,
                            logMessageText.substring(0, Math.min(logMessageText.length(), 200)));
                    // continue; // 알림을 보내지 않으려면 주석 해제
                }


                mattermostService.sendNotification(
                        webhookSetting.getMmURL(),
                        logDoc,
                        webhookSetting,
                        actuallyMatchedKeywords // 매칭된 키워드 리스트 전달
                );

                if (logDoc.getTimestampOriginal() != null) {
                    ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
                    LocalDateTime kstHitTimestamp = LocalDateTime.ofInstant(logDoc.getTimestampOriginal(), seoulZoneId);
                    LocalDateTime hitTimestamp = LocalDateTime.ofInstant(logDoc.getTimestampOriginal(), ZoneOffset.UTC);
                    if (latestLogTimestampForUpdate == null || hitTimestamp.isAfter(latestLogTimestampForUpdate)) {
                        latestLogTimestampForUpdate = kstHitTimestamp;
                    }
                }
            }
        }

        if (latestLogTimestampForUpdate != null) {
            webhookSetting.updateLastCheckedTimestamp(latestLogTimestampForUpdate);
        } else {
            webhookSetting.updateLastCheckedTimestamp(currentPollExecutionTime);
        }
    }
}