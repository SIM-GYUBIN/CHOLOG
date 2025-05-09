package com.ssafy.cholog.domain.project.controller;

import com.ssafy.cholog.domain.project.dto.request.CreateProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.JoinProjectRequest;
import com.ssafy.cholog.domain.project.dto.response.CreateTokenResponse;
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
    public ResponseEntity<CommonResponse<Void>> createProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateProjectRequest request) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.created(projectService.createProject(userId, request));
    }

    @PostMapping("/uuid")
    @Operation(summary = "프로젝트 토큰 생성", description = "프로젝트 고유 토큰 생성 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<CreateTokenResponse>> createToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.created(projectService.createToken(userId));
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
}
