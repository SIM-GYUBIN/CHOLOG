package com.ssafy.cholog.domain.webhook.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.webhook.entity.ChologLogDocument;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPollingService {

    private final WebhookRepository webhookRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final MattermostService mattermostService;

    @Value("${cholog.notification.poll.rate}")
    private String pollRateConfig; // 스케줄러는 문자열 값을 직접 참조

    @Value("${cholog.notification.poll.initialLookbackMinutes}")
    private int initialLookbackMinutes;

    @Value("${cholog.notification.poll.queryResultLimit}")
    private int queryResultLimit;

    @Scheduled(fixedRateString = "${cholog.notification.poll.rate}")
    @Transactional
    public void pollAndNotify() {
        log.info("Starting webhook poll. This service will always run regardless of CHO:LOG server profile.");

        List<Webhook> activeWebhooks = webhookRepository.findByIsEnabledTrue();
        if (activeWebhooks.isEmpty()) {
            log.debug("No active webhooks found.");
            return;
        }

        LocalDateTime currentPollExecutionTime = LocalDateTime.now(ZoneOffset.UTC);

        for (Webhook webhookSetting : activeWebhooks) {
            // isEnabled 필드가 Boolean 래퍼 타입이므로 null 체크 또는 NPE 방지
            if (webhookSetting.getIsEnabled() == null || !webhookSetting.getIsEnabled()) {
                log.debug("Webhook ID {} is disabled. Skipping.", webhookSetting.getId());
                continue;
            }
            try {
                processSingleWebhook(webhookSetting, currentPollExecutionTime);
            } catch (Exception e) {
                // 개별 웹훅 처리 실패 시 로깅 후 다음 웹훅으로 넘어감
                log.error("Error processing webhook ID {}: {}", webhookSetting.getId(), e.getMessage(), e);
            }
        }
        log.info("Webhook poll finished.");
    }

    // 개별 웹훅 처리 로직
    protected void processSingleWebhook(Webhook webhookSetting, LocalDateTime currentPollExecutionTime) {
        log.debug("Processing webhook ID: {}", webhookSetting.getId());

        LocalDateTime queryStartTime;
        if (webhookSetting.getLastCheckedTimestamp() == null) {
            // @Builder.Default로 LocalDateTime.now()가 설정되어 있지만, DB에서 null로 올 수도 있으므로 방어 코드
            queryStartTime = currentPollExecutionTime.minusMinutes(initialLookbackMinutes);
            log.info("Initial poll or null lastCheckedTimestamp for webhook ID {}. Querying logs since {} (last {} minutes).",
                    webhookSetting.getId(), queryStartTime, initialLookbackMinutes);
        } else {
            queryStartTime = webhookSetting.getLastCheckedTimestamp();
        }

        Project project = webhookSetting.getProject();
        if (project == null || !StringUtils.hasText(project.getProjectToken())) {
            log.warn("Project or Project API Key is missing for webhook ID {}. Updating lastCheckedTimestamp and skipping.", webhookSetting.getId());
            webhookSetting.updateLastCheckedTimestamp(currentPollExecutionTime); // 반복적인 실패 방지
            webhookRepository.save(webhookSetting);
            return;
        }
        String projectKeyForQuery = project.getProjectToken();

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.must(QueryBuilders.match(m -> m
                .field("projectKey") // ES 문서의 실제 프로젝트 식별자 필드명
                .query(projectKeyForQuery)
        ));

        boolQueryBuilder.must(QueryBuilders.match(m -> m
                .field("level") // ES 문서의 실제 로그 레벨 필드명
                .query(webhookSetting.getLogLevel().name()) // Webhook 엔티티의 LogLevel Enum 사용
        ));

        long queryStartTimeMillis = queryStartTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        boolQueryBuilder.filter(QueryBuilders.range(r -> r
                .field("@timestamp") // ES 문서의 실제 타임스탬프 필드명
                .gt(JsonData.of(queryStartTimeMillis)) // gt (>) 사용, 이전 시간 이후 로그
        ));

        // 환경 필터링 로직 변경: Webhook 엔티티의 notificationENV (String) 필드 사용
        String envTagToFilter = webhookSetting.getNotificationENV();
        if (StringUtils.hasText(envTagToFilter)) {
            boolQueryBuilder.must(QueryBuilders.match(m -> m
                    .field("environment") // ES 문서의 실제 환경 정보 필드명
                    .query(envTagToFilter)
            ));
            log.debug("Applying 'environment' filter for webhook ID {}: {}", webhookSetting.getId(), envTagToFilter);
        }

        org.springframework.data.elasticsearch.core.query.Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withSort(Sort.by(Sort.Direction.ASC, "@timestamp"))
                .withPageable(PageRequest.of(0, queryResultLimit))
                .build();

        SearchHits<ChologLogDocument> searchHits = elasticsearchOperations.search(searchQuery, ChologLogDocument.class);
        log.debug("Found {} logs for webhook ID {}", searchHits.getTotalHits(), webhookSetting.getId());

        LocalDateTime latestLogTimestampForUpdate = null;

        if (searchHits.hasSearchHits()) {
            for (SearchHit<ChologLogDocument> hit : searchHits) {
                ChologLogDocument logDoc = hit.getContent();
                String esIndex = hit.getIndex();

                // MattermostService의 sendNotification 메소드 호출 시 Webhook 엔티티의 mmURL 사용
                mattermostService.sendNotification(
                        webhookSetting.getMmURL(), // Webhook 엔티티의 mmURL 필드 사용
                        logDoc,
                        webhookSetting,
                        esIndex
                );

                if (logDoc.getTimestamp() != null) { // ChologLogDocument의 timestamp (LocalDateTime)
                    if (latestLogTimestampForUpdate == null || logDoc.getTimestamp().isAfter(latestLogTimestampForUpdate)) {
                        latestLogTimestampForUpdate = logDoc.getTimestamp();
                    }
                }
            }
        }

        if (latestLogTimestampForUpdate != null) {
            webhookSetting.updateLastCheckedTimestamp(latestLogTimestampForUpdate);
            log.debug("Updating lastCheckedTimestamp to {} for webhook ID {}", latestLogTimestampForUpdate, webhookSetting.getId());
        } else {
            webhookSetting.updateLastCheckedTimestamp(currentPollExecutionTime);
            log.debug("No new logs found for webhook ID {}. Updating lastCheckedTimestamp to current poll time {}.",
                    webhookSetting.getId(), currentPollExecutionTime);
        }
        webhookRepository.save(webhookSetting);
    }
}