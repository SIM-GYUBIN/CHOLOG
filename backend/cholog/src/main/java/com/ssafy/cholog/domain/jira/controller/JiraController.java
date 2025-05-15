package com.ssafy.cholog.domain.jira.controller;

import com.ssafy.cholog.domain.jira.dto.request.JiraIssueRequest;
import com.ssafy.cholog.domain.jira.dto.request.JiraProjectRequest;
import com.ssafy.cholog.domain.jira.dto.request.JiraUserRequest;
import com.ssafy.cholog.domain.jira.dto.response.JiraIssueCreationResponse;
import com.ssafy.cholog.domain.jira.dto.response.JiraProjectResponse;
import com.ssafy.cholog.domain.jira.dto.response.JiraUserListResponse;
import com.ssafy.cholog.domain.jira.dto.response.JiraUserResponse;
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

    @GetMapping("/user")
    @Operation(summary = "JIRA 개인 설정 조회", description = "JIRA 개인 설정 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<JiraUserResponse>> getJiraUser (
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.getJiraUser(userId));
    }

    @PostMapping("/user")
    @Operation(summary = "JIRA 개인 설정 등록", description = "JIRA 개인 설정 등록 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.JIRA_USER_ALREADY_EXISTS})
    public ResponseEntity<CommonResponse<Void>> registJiraUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JiraUserRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.registJiraUser(userId, request));
    }

    @PutMapping("/user")
    @Operation(summary = "JIRA 개인 설정 수정", description = "JIRA 개인 설정 수정 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.JIRA_USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<Void>> updateJiraUser (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JiraUserRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.updateJiraUser(userId, request));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "JIRA 프로젝트 설정 조회", description = "JIRA 프로젝트 설정 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<JiraProjectResponse>> getJiraProject (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.getJiraProject(userId, projectId));
    }

    @PostMapping("/project/{projectId}")
    @Operation(summary = "JIRA 프로젝트 설정 등록", description = "JIRA 프로젝트 설정 등록 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
                            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER, ErrorCode.JIRA_PROJECT_ALREADY_EXISTS})
    public ResponseEntity<CommonResponse<Void>> registJiraProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraProjectRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.registJiraProject(userId, projectId, request));
    }

    @PutMapping("/project/{projectId}")
    @Operation(summary = "JIRA 프로젝트 설정 수정", description = "JIRA 프로젝트 설정 수정 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
                            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER, ErrorCode.JIRA_PROJECT_NOT_FOUND})
    public ResponseEntity<CommonResponse<Void>> updateJiraProject (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraProjectRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.updateJiraProject(userId, projectId, request));
    }

    @GetMapping("/issue/{projectId}")
    @Operation(summary = "JIRA 이슈 생성을 위한 정보 조회", description = "JIRA 이슈 생성을 위한 정보 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER,
            ErrorCode.JIRA_PROJECT_NOT_FOUND, ErrorCode.JIRA_PROJECT_NOT_FOUND})
    public ResponseEntity<CommonResponse<JiraUserListResponse>> getJira(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraService.getJiraUserList(userId, projectId));
    }

    @PostMapping("/issue/{projectId}")
    @Operation(summary = "JIRA 이슈 생성", description = "JIRA 이슈 생성 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
                            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER,
                            ErrorCode.JIRA_USER_NOT_FOUND, ErrorCode.JIRA_PROJECT_NOT_FOUND})
    public ResponseEntity<CommonResponse<JiraIssueCreationResponse>> createJiraIssue(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody JiraIssueRequest request){

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(jiraIssueService.createJiraIssue(userId, projectId, request));
    }
}
