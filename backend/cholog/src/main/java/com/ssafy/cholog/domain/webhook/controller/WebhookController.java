package com.ssafy.cholog.domain.webhook.controller;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.webhook.dto.request.WebhookRequest;
import com.ssafy.cholog.domain.webhook.entity.ChologLogDocument;
import com.ssafy.cholog.domain.webhook.entity.Webhook;
import com.ssafy.cholog.domain.webhook.repository.WebhookRepository;
import com.ssafy.cholog.domain.webhook.service.MattermostService;
import com.ssafy.cholog.domain.webhook.service.WebhookService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Tag(name = "웹훅", description = "웹훅 설정 및 관리 API")
public class WebhookController {

    private final WebhookService webhookService;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/{projectId}")
    @Operation(summary = "웹훅 알림 생성", description = "웹훅 알림 생성 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.WEBHOOK_ALREADY_EXISTS,
            ErrorCode.FORBIDDEN_ACCESS, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> createWebhook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody WebhookRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.created(webhookService.createWebhook(userId, projectId, request));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "웹훅 알림 수정", description = "웹훅 알림 수정 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.WEBHOOK_NOT_FOUND,
            ErrorCode.FORBIDDEN_ACCESS, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> updateWebhook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody WebhookRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(webhookService.updateWebhook(userId, projectId, request));
    }

    //===================================================================================//
    private final MattermostService mattermostService;
    private final WebhookRepository webhookRepository;
    private final ProjectRepository projectRepository;

    @PostMapping("/test/{projectId}")
    @Operation(summary = "웹훅 테스트", description = "웹훅 테스트 API")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResponse<String>> sendTestMattermostNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId){

        log.info("Received request to send test Mattermost notification for projectId: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 1. NotificationSetting 조회
        Webhook webhook = webhookRepository.findByProject(project).orElse(null);

        if (webhook == null) {
            log.warn("NotificationSetting not found for ID: {}", projectId);
            return ResponseEntity.notFound().build();
        }

        if (!webhook.getIsEnabled()) {
            String message = String.format("Test notification not sent. NotificationSetting ID %d is disabled.", projectId);
            log.info(message);
            return CommonResponse.ok(message);
        }

        if (!StringUtils.hasText(webhook.getMmURL())) {
            String message = String.format("Test notification not sent. Mattermost Webhook URL is not configured for NotificationSetting ID %d.", projectId);
            log.warn(message);
            return CommonResponse.error(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 2. 테스트용 ChologLogDocument 생성
        ChologLogDocument dummyLogDoc = new ChologLogDocument();

        // ChologLogDocument에 필요한 필드들을 설정합니다.
        // 실제 Project 엔티티를 로드해서 사용하는 것이 더 정확한 테스트가 됩니다.
        // 여기서는 NotificationSetting에 Project가 Eager 로딩되거나,
        // 또는 테스트 목적상 일부 정보만 사용한다고 가정합니다.
        String testApiKey = "TEST_API_KEY";
        String testAppName = "Test Application";
        if (webhook.getProject() != null) {
            if (StringUtils.hasText(webhook.getProject().getProjectToken())) {
                testApiKey = webhook.getProject().getProjectToken();
            }
            if (StringUtils.hasText(webhook.getProject().getName())) {
                testAppName = webhook.getProject().getName(); // 테스트 메시지에 프로젝트 이름 사용
            }
        }

        dummyLogDoc.setId("test-log-" + UUID.randomUUID().toString().substring(0, 8));
        dummyLogDoc.setProjectId(testApiKey); // 로그에 기록되는 프로젝트 식별자 (예: API Key)
        dummyLogDoc.setLevel(webhook.getLogLevel().name()); // 설정된 로그 레벨 사용
        dummyLogDoc.setMessage("🚨 이것은 CHO:LOG 알림 테스트 메시지입니다. (This is a CHO:LOG test notification message.)");
        dummyLogDoc.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        dummyLogDoc.setAppEnvironment(StringUtils.hasText(webhook.getNotificationENV()) ?
                webhook.getNotificationENV() : "test-env");
        dummyLogDoc.setAppName(testAppName); // ChologLogDocument에 appName 필드 가정
        dummyLogDoc.setStackTrace(
                "com.cholog.test.TestException: CHO:LOG Mattermost 알림 테스트 중 발생한 예외입니다.\n" +
                        "\tat com.cholog.test.MyTestClass.performTest(MyTestClass.java:42)\n" +
                        "\tat com.cholog.test.Framework.runTest(Framework.java:101)"
        );
        dummyLogDoc.setTraceId("test-trace-" + UUID.randomUUID().toString());
        // dummyLogDoc.setEsIndex("test-cholog-logs"); // ChologLogDocument에 esIndex 필드 가정

        String dummyEsIndex = "test-logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));


        // 3. Mattermost 알림 서비스 호출 (비동기)
        try {
            mattermostService.sendNotification(
                    webhook.getMmURL(),
                    dummyLogDoc,
                    webhook,
                    dummyEsIndex // Elasticsearch 인덱스명 (테스트용)
            );
            String successMessage = String.format("Test Mattermost notification successfully triggered for setting ID: %d. Check your Mattermost channel.", projectId);
            log.info(successMessage);
            return CommonResponse.ok(successMessage);
        } catch (Exception e) {
            // @Async 메소드에서 발생한 예외는 직접 잡히지 않을 수 있으나, Retryable의 Recover에서 로깅됩니다.
            // 여기서는 호출 시도 자체의 동기적 실패(예: 설정 오류로 URL이 아예 없는 경우 등)를 대비합니다.
            String errorMessage = String.format("Failed to trigger test Mattermost notification for setting ID %d: %s", projectId, e.getMessage());
            log.error(errorMessage, e);
            return CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
