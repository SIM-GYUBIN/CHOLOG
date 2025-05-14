package com.ssafy.cholog.domain.jira.controller;

import com.ssafy.cholog.domain.jira.dto.request.JiraIssueRequest;
import com.ssafy.cholog.domain.jira.dto.request.JiraRequest;
import com.ssafy.cholog.domain.jira.dto.response.JiraResponse;
import com.ssafy.cholog.domain.jira.service.JiraIssueService;
import com.ssafy.cholog.domain.jira.service.JiraService;
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
@RequestMapping("/api/jira")
@RequiredArgsConstructor
@Tag(name = "Jira", description = "Jira 설정 및 관리 API")
public class JiraController {

    private final JiraService jiraService;
    private final JiraIssueService jiraIssueService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/{projectId}")
    @Operation(summary = "JIRA 토큰 조회", description = "JIRA 토큰 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<JiraResponse>> getJiraToken (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.getJiraToken(userId, projectId));
    }

    @PostMapping("/{projectId}")
    @Operation(summary = "JIRA 토큰 등록", description = "JIRA 토큰 등록 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER, ErrorCode.JIRATOKEN_ALREADY_EXISTS})
    public ResponseEntity<CommonResponse<Void>> registJiraToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.registJiraToken(userId, projectId, request));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "JIRA 토큰 수정", description = "JIRA 토큰 수정 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER, ErrorCode.JIRATOKEN_NOT_EXISTS})
    public ResponseEntity<CommonResponse<Void>> updateJiraToken (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.updateJiraToken(userId, projectId, request));
    }

    @PostMapping("/{projectId}/issue")
    @Operation(summary = "JIRA 이슈 생성", description = "JIRA 이슈 생성 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER, ErrorCode.JIRATOKEN_NOT_EXISTS})
    public ResponseEntity<CommonResponse<Void>> createJiraIssue(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraIssueRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraIssueService.createJiraIssue(userId, projectId, request));
    }
}
