package com.ssafy.cholog.domain.webhook.service;

import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.webhook.dto.mattermost.Attachment;
import com.ssafy.cholog.domain.webhook.dto.mattermost.Field;
import com.ssafy.cholog.domain.webhook.dto.mattermost.MattermostPayload;
import com.ssafy.cholog.domain.webhook.dto.mattermost.Props;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
    public void sendNotification(String userWebhookUrl, LogDocument logDoc, Webhook setting, List<String> matchedKeywords) {
        String projectName = setting.getProject() != null && StringUtils.hasText(setting.getProject().getName()) ?
                setting.getProject().getName() : "알 수 없는 프로젝트";
        Integer projectId = setting.getProject() != null ? setting.getProject().getId() : null;

        String keywordsDisplay = StringUtils.hasText(setting.getKeywords()) ? setting.getKeywords() : "지정 안됨";
        String ruleName = String.format("%s", projectName);

        // --- LogDocument 필드 값 추출 ---
        String timestampStr = "N/A";
        if (logDoc.getTimestampOriginal() != null) {
            Instant eventInstantUtc = logDoc.getTimestampOriginal();
            // UTC Instant를 한국 시간대(KST, Asia/Seoul)의 ZonedDateTime으로 변환
            ZonedDateTime eventKst = eventInstantUtc.atZone(ZoneId.of("Asia/Seoul"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분", Locale.KOREAN);
            timestampStr = eventKst.format(formatter);
        }

        String primaryMessage = StringUtils.hasText(logDoc.getMessage()) ? logDoc.getMessage() : "";
        LogDocument.ErrorInfo errorInfo = logDoc.getError();
        if (errorInfo != null && StringUtils.hasText(errorInfo.getMessage())) {
            if (primaryMessage.isEmpty() || !primaryMessage.contains(errorInfo.getMessage())) {
                primaryMessage = primaryMessage.isEmpty() ? errorInfo.getMessage() : primaryMessage + " | 오류: " + errorInfo.getMessage();
            }
        }
        if (primaryMessage.isEmpty()) { primaryMessage = "내용 없음"; }

        String message = primaryMessage;

        String appEnv = StringUtils.hasText(logDoc.getEnvironment()) ? logDoc.getEnvironment() : "N/A";
        String appName = StringUtils.hasText(logDoc.getSource()) ? logDoc.getSource() : "N/A";
        String logLevel = StringUtils.hasText(logDoc.getLevel()) ? logDoc.getLevel() : "N/A";
        String stackTrace = (errorInfo != null && StringUtils.hasText(errorInfo.getStacktrace())) ?
                errorInfo.getStacktrace() : null; // stackTrace는 내용이 없으면 null로 두어 if 조건에서 처리

        // 1. `text` 필드 구성
        StringBuilder mainTextBuilder = new StringBuilder();
        mainTextBuilder.append(String.format("### 🚨 **로그 발생 - %s**\n\n", ruleName));
        mainTextBuilder.append(String.format("⏰ **시간**: %s\n", timestampStr));
        mainTextBuilder.append(String.format("📜 **메시지**:\n```\n%s\n```\n", message));
        mainTextBuilder.append(String.format("**환경**: %s", appEnv));

        if (matchedKeywords != null && !matchedKeywords.isEmpty()) {
            mainTextBuilder.append(String.format("\n**매칭 키워드**: `%s`", matchedKeywords.stream().collect(Collectors.joining("`, `"))));
        }
        String mainText = mainTextBuilder.toString();

        MattermostPayload payload = new MattermostPayload();
        payload.setText(mainText);

        // 2. `props` 필드 (스택 트레이스) 구성
        if (StringUtils.hasText(stackTrace)) {
            Props props = new Props(String.format("**스택 트레이스**:\n```\n%s\n```", stackTrace));
            payload.setProps(props);
        }

        // 3. `attachments` 필드 구성
        String contextLinkPath = "fallback-link-not-available";
        if (projectId != null) {
            contextLinkPath = String.format("project/%d/log/%s", projectId, logDoc.getId());
        }
        String fullTitleLink = chologUiBaseUrl + "/" + contextLinkPath;

        // ================= 시간 되면 커스텀도 가능하도록 ================= //
        Attachment attachment = new Attachment();
        attachment.setFallback("로그 발생: " + ruleName);

        // --- 로그 레벨에 따라 동적으로 색상 설정 ---
        String attachmentColor;
        switch (logLevel) {
            case "ERROR":
            case "FATAL": // FATAL 레벨도 ERROR와 유사하게 처리
                attachmentColor = "#D00000"; // 진한 빨간색 (또는 기존 #FF0000)
                break;
            case "WARN":
            case "WARNING": // WARNING도 WARN과 유사하게 처리
                attachmentColor = "#FFA500"; // 주황색
                break;
            case "INFO":
                attachmentColor = "#2EB886"; // 초록색 계열 (또는 파란색 #007BFF)
                break;
            case "DEBUG":
            case "TRACE":
                attachmentColor = "#CCCCCC"; // 밝은 회색
                break;
            default:
                attachmentColor = "#808080"; // 기본 회색 (알 수 없는 레벨)
                break;
        }
        attachment.setColor(attachmentColor);

        attachment.setTitle("CHO:LOG에서 자세히 보기");
        attachment.setTitleLink(fullTitleLink);

        List<Field> fields = new ArrayList<>();
        fields.add(new Field(true, "애플리케이션", appName));
        fields.add(new Field(true, "로그 레벨", logLevel));
         if (matchedKeywords != null && !matchedKeywords.isEmpty()) {
            fields.add(new Field(false, "매칭된 키워드", String.join(", ", matchedKeywords)));
         }
        attachment.setFields(fields);
        payload.setAttachments(Collections.singletonList(attachment));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MattermostPayload> requestEntity = new HttpEntity<>(payload, headers);

        try {
            log.debug("Sending rich Mattermost notification to webhook URL for setting ID {}...", setting.getId());
            ResponseEntity<String> response = restTemplate.postForEntity(userWebhookUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && "ok".equalsIgnoreCase(response.getBody())) {
//                log.info("Successfully sent rich Mattermost notification for log via setting ID {}.",
//                        setting.getId());
            } else {
                log.warn("Rich Mattermost notification sent for log, but received status: {} - Body: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.warn("Failed to send rich Mattermost notification for log for setting ID {} (Attempting retry if applicable): {}",
                    setting.getId(), e.getMessage());
            throw e;
        }
    }

    @Recover
    public void recover(RestClientException e, String userWebhookUrl, LogDocument logDoc, Webhook webhookSetting, String esIndexFromSearchHit, List<String> matchedKeywords) {
        String docId = (logDoc != null && StringUtils.hasText(logDoc.getId())) ? logDoc.getId() : "N/A";
        Integer settingId = (webhookSetting != null) ? webhookSetting.getId() : null; // Integer로 가정
        log.error("All retries failed for Mattermost notification. Log (Doc ID: {}), Setting ID {}. Matched Keywords: {}. Final Error: {}",
                docId, settingId, (matchedKeywords != null ? String.join(", ", matchedKeywords) : "N/A"), e.getMessage());
    }
}
