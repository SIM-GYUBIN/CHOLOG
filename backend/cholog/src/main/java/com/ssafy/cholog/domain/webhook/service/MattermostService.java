package com.ssafy.cholog.domain.webhook.service;

import com.ssafy.cholog.domain.webhook.dto.mattermost.Attachment;
import com.ssafy.cholog.domain.webhook.dto.mattermost.Field;
import com.ssafy.cholog.domain.webhook.dto.mattermost.MattermostPayload;
import com.ssafy.cholog.domain.webhook.dto.mattermost.Props;
import com.ssafy.cholog.domain.webhook.entity.ChologLogDocument;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MattermostService {

    private final RestTemplate restTemplate;

    @Value("${app.domain.url}")
    private String chologUiBaseUrl;

    @Async
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendNotification(String userWebhookUrl, ChologLogDocument logDoc, Webhook setting, String esIndexFromSearchHit) {
        String projectName = setting.getProject() != null && StringUtils.hasText(setting.getProject().getName()) ?
                setting.getProject().getName() : "알 수 없는 프로젝트";
        Integer projectId = setting.getProject() != null ? setting.getProject().getId() : null;

        String ruleName = projectName + " - " + setting.getLogLevel().name() + " 레벨 알림";

        String timestampStr = logDoc.getTimestamp() != null ?
                logDoc.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME) + "Z" : "N/A";
        String message = StringUtils.hasText(logDoc.getMessage()) ? logDoc.getMessage() : "내용 없음";
        String docId = StringUtils.hasText(logDoc.getId()) ? logDoc.getId() : "N/A";
        String appEnv = StringUtils.hasText(logDoc.getAppEnvironment()) ? logDoc.getAppEnvironment() : "N/A";
        String appName = StringUtils.hasText(logDoc.getAppName()) ? logDoc.getAppName() : "N/A";
        String logLevel = logDoc.getLevel() != null ? logDoc.getLevel().toString() : "N/A";
        String stackTrace = logDoc.getStackTrace();

        String mainText = String.format("🚨 **에러 알림 발생 - %s**\n\n" +
                        "⏰ **시간**: %s\n" +
                        "📜 **메시지**: %s\n" +
                        "🔍 **인덱스**: %s\n" +
                        "🔗 **문서 ID**: %s\n" +
                        "**환경**: %s",
                ruleName,
                timestampStr,
                message,
                StringUtils.hasText(esIndexFromSearchHit) ? esIndexFromSearchHit : "N/A",
                docId,
                appEnv
        );

        MattermostPayload payload = new MattermostPayload();
        payload.setText(mainText);

        if (StringUtils.hasText(stackTrace)) {
            Props props = new Props(String.format("**스택 트레이스**:\n```\n%s\n```", stackTrace));
            payload.setProps(props);
        }

        String contextLinkPath = "fallback-link-not-available";
        if (projectId != null) {
            if (StringUtils.hasText(logDoc.getTraceId())) {
                contextLinkPath = String.format("projects/%d/trace/%s", projectId, logDoc.getTraceId());
            } else if (StringUtils.hasText(logDoc.getId())) {
                contextLinkPath = String.format("projects/%d/logs/%s", projectId, logDoc.getId());
            }
        }
        String fullTitleLink = chologUiBaseUrl + "/" + contextLinkPath;

        Attachment attachment = new Attachment();
        attachment.setFallback("에러 발생: " + ruleName);
        attachment.setColor("#FF0000");
        attachment.setTitle("CHO:LOG에서 자세히 보기");
        attachment.setTitleLink(fullTitleLink);

        List<Field> fields = new ArrayList<>();
        fields.add(new Field(true, "애플리케이션", appName));
        fields.add(new Field(true, "로그 레벨", logLevel));
        attachment.setFields(fields);
        payload.setAttachments(Collections.singletonList(attachment));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MattermostPayload> requestEntity = new HttpEntity<>(payload, headers);

        try {
            log.debug("Sending rich Mattermost notification to webhook URL for setting ID {}...", setting.getId()); // logger 대신 log 사용
            ResponseEntity<String> response = restTemplate.postForEntity(userWebhookUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && "ok".equalsIgnoreCase(response.getBody())) {
                log.info("Successfully sent rich Mattermost notification for log (Doc ID: {}) via setting ID {}.",
                        docId, setting.getId());
            } else {
                log.warn("Rich Mattermost notification sent for log (Doc ID: {}), but received status: {} - Body: {}",
                        docId, response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.warn("Failed to send rich Mattermost notification for log (Doc ID: {}) for setting ID {} (Attempting retry if applicable): {}",
                    docId, setting.getId(), e.getMessage());
            throw e;
        }
    }

    @Recover
    public void recover(RestClientException e, String userWebhookUrl, ChologLogDocument logDoc, Webhook setting, String esIndexFromSearchHit) {
        String docId = logDoc != null && StringUtils.hasText(logDoc.getId()) ? logDoc.getId() : "N/A";
        Integer settingId = setting != null ? setting.getId() : null;
        log.error("All retries failed for rich Mattermost notification. Log (Doc ID: {}), Setting ID {}. Final Error: {}",
                docId, settingId, e.getMessage());
    }
}
