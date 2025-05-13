package com.ssafy.cholog.domain.log.controller;

import com.ssafy.cholog.domain.log.dto.response.LogEntryResponse;
import com.ssafy.cholog.domain.log.dto.response.LogStatsResponse;
import com.ssafy.cholog.domain.log.dto.response.LogTimelineResponse;
import com.ssafy.cholog.domain.log.service.LogService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@Tag(name = "로그", description = "로그 관련 API \n\n 각 logtype에 따라 event, error, http 필드의 존재 유무 달라짐 \n\n source에 따라 client 필드 존재 유무 달라짐")
public class LogController {

    private final LogService logService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 로그 전체 조회", description = "프로젝트 로그 전체 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<CustomPage<LogEntryResponse>>> getProjectAllLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            // PageableDefault의 sort 필드를 ES의 실제 타임스탬프 필드명(@timestamp) 또는
            // LogService에서 매핑 처리할 이름("createdAt")으로 지정.
            // 여기서는 LogService에서 "createdAt" -> "@timestamp" 매핑을 하므로 "createdAt" 유지 가능.
            // 또는 명확하게 "@timestamp"로 지정.
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        CustomPage<LogEntryResponse> logs = logService.getProjectAllLog(userId, projectId, pageable);
        return CommonResponse.ok(logs);
    }

    @GetMapping("/{projectId}/detail/{logId}")
    @Operation(summary = "로그 상세 조회", description = "로그 상세 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_NOT_FOUND})
    public ResponseEntity<CommonResponse<LogEntryResponse>> getLogDetail(
            @PathVariable Integer projectId,
            @PathVariable String logId
    ) {
        LogEntryResponse logDetail = logService.getLogDetail(projectId, logId);
        return CommonResponse.ok(logDetail);
    }

    @GetMapping("/{projectId}/trace/{traceId}")
    @Operation(summary = "traceId로 로그 조회", description = "traceId로 로그 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<List<LogEntryResponse>>> getLogByTraceId(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @PathVariable String traceId
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        List<LogEntryResponse> logs = logService.getLogByTraceId(userId, projectId, traceId);
        return CommonResponse.ok(logs);
    }

    @GetMapping("/{projectId}/search")
    @Operation(summary = "로그 검색", description = "로그 검색 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<CustomPage<LogEntryResponse>>> searchLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String message,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable){

        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        CustomPage<LogEntryResponse> logs = logService.searchLog(userId, projectId, level, apiPath, message, pageable);
        return CommonResponse.ok(logs);
    }

    @GetMapping("/{projectId}/stats")
    @Operation(summary = "프로젝트 로그 통계 조회", description = "프로젝트 로그 통계 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<LogStatsResponse>> getProjectLogStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        return CommonResponse.ok(logService.getProjectLogStats(userId, projectId));
    }

    @GetMapping("/{projectId}/timeline")
    @Operation(summary = "시간대별 로그 발생 추이", description = "시간대별 로그 발생 추이 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<List<LogTimelineResponse>>> getProjectLogTimeline(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            // 기본값은 endDate는 현재 시간, startDate는 1주일 전을 기본값으로 설정
            @RequestParam(required = false, defaultValue = "#{T(java.time.LocalDate).now().minusDays(7).toString()}") String startDate,
            @RequestParam(required = false, defaultValue = "#{T(java.time.LocalDate).now().toString()}") String endDate
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        List<LogTimelineResponse> logs = logService.getProjectLogTimeline(userId, projectId, startDate, endDate);
        return CommonResponse.ok(logs);
    }
}
