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
    private String pollRateConfig;

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
            if (webhookSetting.getIsEnabled() == null || !webhookSetting.getIsEnabled()) {
                log.debug("Webhook ID {} is disabled. Skipping.", webhookSetting.getId());
                continue;
            }
            try {
                processSingleWebhook(webhookSetting, currentPollExecutionTime);
            } catch (Exception e) {
                log.error("Error processing webhook ID {}: {}", webhookSetting.getId(), e.getMessage(), e);
            }
        }
        log.info("Webhook poll finished.");
    }

    protected void processSingleWebhook(Webhook webhookSetting, LocalDateTime currentPollExecutionTime) {
        log.debug("Processing webhook ID: {}", webhookSetting.getId());

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
            webhookRepository.save(webhookSetting);
            return;
        }
        String projectKeyForQuery = project.getProjectToken();

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.must(QueryBuilders.match(m -> m
                .field("projectKey")
                .query(projectKeyForQuery)
        ));

        boolQueryBuilder.must(QueryBuilders.match(m -> m
                .field("level")
                .query(webhookSetting.getLogLevel().name())
        ));

        long queryStartTimeMillis = queryStartTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        boolQueryBuilder.filter(QueryBuilders.range(r -> r
                .field("@timestamp")
                .gt(JsonData.of(queryStartTimeMillis))
        ));

        String envTagToFilter = webhookSetting.getNotificationENV();
        if (StringUtils.hasText(envTagToFilter)) {
            boolQueryBuilder.must(QueryBuilders.match(m -> m
                    .field("environment")
                    .query(envTagToFilter)
            ));
            log.debug("Applying 'environment' filter for webhook ID {}: {}", webhookSetting.getId(), envTagToFilter);
        }

        String indexName = "pjt-" + project.getProjectToken();

        org.springframework.data.elasticsearch.core.query.Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withSort(Sort.by(Sort.Direction.ASC, "@timestamp"))
                .withPageable(PageRequest.of(0, queryResultLimit))
                .build();

        SearchHits<LogDocument> searchHits = elasticsearchOperations.search(searchQuery, LogDocument.class, IndexCoordinates.of(indexName));
        log.debug("Found {} logs for webhook ID {}", searchHits.getTotalHits(), webhookSetting.getId());

        LocalDateTime latestLogTimestampForUpdate = null;

        if (searchHits.hasSearchHits()) {
            for (SearchHit<LogDocument> hit : searchHits) {
                LogDocument logDoc = hit.getContent();
                String esIndex = hit.getIndex();

                mattermostService.sendNotification(
                        webhookSetting.getMmURL(),
                        logDoc, // 타입 변경됨
                        webhookSetting,
                        esIndex
                );

                if (logDoc.getTimestampEs() != null) { // logDoc.getTimestampEs()는 Instant 타입
                    // Instant를 LocalDateTime으로 변환 (UTC 기준)
                    LocalDateTime hitTimestamp = LocalDateTime.ofInstant(logDoc.getTimestampEs(), ZoneOffset.UTC);

                    if (latestLogTimestampForUpdate == null || hitTimestamp.isAfter(latestLogTimestampForUpdate)) {
                        latestLogTimestampForUpdate = hitTimestamp; // LocalDateTime 타입으로 할당
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