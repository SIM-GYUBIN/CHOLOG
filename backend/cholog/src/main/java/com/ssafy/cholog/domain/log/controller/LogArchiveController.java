package com.ssafy.cholog.domain.log.controller;

import com.ssafy.cholog.domain.log.dto.request.archive.LogArchiveRequest;
import com.ssafy.cholog.domain.log.dto.response.LogArchiveResponse;
import com.ssafy.cholog.domain.log.service.LogArchiveService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.CustomPage;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@Tag(name = "로그", description = "로그 아카이빙 API")
public class LogArchiveController {

    private final LogArchiveService logArchiveService;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/{projectId}/archive")
    @Operation(summary = "로그 아카이빙", description = "로그 아카이빙 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_NOT_FOUND})
    public ResponseEntity<CommonResponse<Void>> archiveLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestBody LogArchiveRequest logArchiveRequest
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        logArchiveService.archiveLog(userId, projectId, logArchiveRequest);
        return CommonResponse.ok();
    }

    @GetMapping("/{projectId}/archive")
    @Operation(summary = "로그 아카이빙 목록 조회", description = "로그 아카이빙 목록 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<CustomPage<LogArchiveResponse>>> getAllArchiveLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @PageableDefault Pageable pageable
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        return CommonResponse.ok(logArchiveService.getAllArchiveLog(userId, projectId, pageable));
    }
}
