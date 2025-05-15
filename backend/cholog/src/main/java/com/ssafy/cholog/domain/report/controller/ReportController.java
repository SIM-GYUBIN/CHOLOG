package com.ssafy.cholog.domain.report.controller;

import com.ssafy.cholog.domain.report.dto.request.ReportRequest;
import com.ssafy.cholog.domain.report.dto.response.ReportResponse;
import com.ssafy.cholog.domain.report.service.ReportService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "로그", description = "로그 리포트 API")
public class ReportController {

    private final ReportService reportService;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/{projectId}")
    @Operation(summary = "로그 리포트 발급", description = "로그 리포트 발급 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_START_TIME_AFTER_END_TIME, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<ReportResponse>> archiveLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestBody ReportRequest reportRequest
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(reportService.makeReport(userId, projectId, reportRequest));
    }
}
