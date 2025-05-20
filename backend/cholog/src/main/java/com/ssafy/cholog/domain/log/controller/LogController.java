package com.ssafy.cholog.domain.log.controller;

import com.ssafy.cholog.domain.log.dto.response.*;
import com.ssafy.cholog.domain.log.service.LogAnalysisService;
import com.ssafy.cholog.domain.log.service.LogSearchService;
import com.ssafy.cholog.domain.log.service.LogService;
import com.ssafy.cholog.domain.webhook.dto.request.LogAnalysisRequest;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.CustomPage;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@Tag(name = "로그", description = "로그 관련 API \n\n 각 logtype에 따라 event, error, http 필드의 존재 유무 달라짐 \n\n source에 따라 client 필드 존재 유무 달라짐")
public class LogController {

    private final LogService logService;
    private final LogSearchService logSearchService;
    private final LogAnalysisService logAnalysisService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 로그 전체 조회", description = "프로젝트 로그 전체 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<CustomPage<LogListResponse>>> getProjectAllLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        CustomPage<LogListResponse> logs = logService.getProjectAllLog(userId, projectId, pageable);
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
    public ResponseEntity<CommonResponse<List<LogListResponse>>> getLogByTraceId(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @PathVariable String traceId
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        List<LogListResponse> logs = logService.getLogByTraceId(userId, projectId, traceId);
        return CommonResponse.ok(logs);
    }

    @GetMapping("/{projectId}/search")
    @Operation(summary = "로그 검색", description = "로그 검색 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<CustomPage<LogListResponse>>> searchLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String source,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);
        CustomPage<LogListResponse> logs = logSearchService.searchLog(userId, projectId, level, apiPath, message, source, pageable);
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
    @ApiErrorCodeExamples({ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_START_TIME_AFTER_END_TIME, ErrorCode.PROJECT_USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<List<LogTimelineResponse>>> getProjectLogTimeline(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);

        String effectiveStartDate;
        if (startDate == null || startDate.trim().isEmpty()) {
            effectiveStartDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(7).toString();
        } else {
            effectiveStartDate = startDate;
        }

        String effectiveEndDate;
        if (endDate == null || endDate.trim().isEmpty()) {
            ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            LocalDateTime nextHourKst = nowKst.toLocalDateTime().withMinute(0).withSecond(0).withNano(0).plusHours(1);
            effectiveEndDate = nextHourKst.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            effectiveEndDate = endDate;
        }

        List<LogTimelineResponse> logs = logService.getProjectLogTimeline(userId, projectId, effectiveStartDate, effectiveEndDate);
        return CommonResponse.ok(logs);
    }

    @PostMapping("/{projectId}/analysis")
    @Operation(summary = "로그 LLM 분석", description = "특정 프로젝트의 지정된 로그에 대해 LLM 분석을 수행합니다.")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public Mono<ResponseEntity<CommonResponse<LogAnalysisResponse>>> analyzeLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @Valid @RequestBody LogAnalysisRequest request) {

        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);

        return logAnalysisService.analyzeLogWithGroq(projectId, request)
                .map(analysisResult -> {
                    log.info("Log analysis successfully completed for projectId: {}, logId: {}", projectId, request.getLogId());
                    return CommonResponse.ok(analysisResult);
                });
    }
}
