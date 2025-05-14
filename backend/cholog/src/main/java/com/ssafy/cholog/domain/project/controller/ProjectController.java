package com.ssafy.cholog.domain.project.controller;

import com.ssafy.cholog.domain.project.dto.request.CreateProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.JoinProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.RecreateTokenRequest;
import com.ssafy.cholog.domain.project.dto.request.UpdateProjectNameRequest;
import com.ssafy.cholog.domain.project.dto.response.CreateProjectResponse;
import com.ssafy.cholog.domain.project.dto.response.ProjectDetailResponse;
import com.ssafy.cholog.domain.project.dto.response.RecreateTokenResponse;
import com.ssafy.cholog.domain.project.dto.response.UserProjectListResponse;
import com.ssafy.cholog.domain.project.service.ProjectService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "프로젝트", description = "프로젝트 생성 및 관리 API")
public class ProjectController {

    private final ProjectService projectService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("")
    @Operation(summary = "프로젝트 목록 조회", description = "프로젝트 목록을 최신순으로 조회합니다.")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<UserProjectListResponse>> getUserProjectList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.getUserProjectList(userId));
    }

    @PostMapping("")
    @Operation(summary = "프로젝트 생성", description = "프로젝트 생성 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<CreateProjectResponse>> createProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateProjectRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.created(projectService.createProject(userId, request));
    }

    @PutMapping("/uuid")
    @Operation(summary = "프로젝트 토큰 재발급", description = "프로젝트 고유 토큰 재발급 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
                            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.FORBIDDEN_ACCESS, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<RecreateTokenResponse>> createToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RecreateTokenRequest reqeust) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.created(projectService.recreateToken(userId, reqeust));
    }

    @PostMapping("/join")
    @Operation(summary = "프로젝트 참여", description = "고유 토큰으로 프로젝트 참여 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND,
                            ErrorCode.INTERNAL_SERVER_ERROR,
                            ErrorCode.PROJECT_NOT_FOUND,
                            ErrorCode.PROJECT_ALREADY_JOINED})
    public ResponseEntity<CommonResponse<Void>> joinProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JoinProjectRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.joinProject(userId, request));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "프로젝트 이름 수정", description = "프로젝트 이름 수정 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.FORBIDDEN_ACCESS, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> updateProjectName(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId,
            @Valid @RequestBody UpdateProjectNameRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.updateProjectName(userId, projectId, request));
    }

    @DeleteMapping("/{projectId}/me")
    @Operation(summary = "프로젝트 탈퇴", description = "프로젝트 탈퇴 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> withdrawProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.withdrawProject(userId, projectId));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제", description = "프로젝트 삭제 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.FORBIDDEN_ACCESS, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.deleteProject(userId, projectId));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 기타 정보 조회", description = "프로젝트 정보 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.NOT_PROJECT_USER})
    public ResponseEntity<CommonResponse<ProjectDetailResponse>> getProjectDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("projectId") Integer projectId) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.getProjectDetail(userId, projectId));
    }
}
