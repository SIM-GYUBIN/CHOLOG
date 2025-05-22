package com.ssafy.cholog.domain.webhook.controller;

import com.ssafy.cholog.domain.webhook.dto.request.WebhookRequest;
import com.ssafy.cholog.domain.webhook.dto.response.WebhookResponse;
import com.ssafy.cholog.domain.webhook.service.WebhookService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
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
import org.springframework.web.bind.annotation.*;

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
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.WEBHOOK_ALREADY_EXISTS, ErrorCode.NOT_PROJECT_USER})
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
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> updateWebhook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody WebhookRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(webhookService.updateWebhook(userId, projectId, request));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "웹훅 알림 조회", description = "웹훅 알림 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<WebhookResponse>> getWebhook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(webhookService.getWebhook(userId, projectId));
    }
}