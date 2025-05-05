package com.ssafy.cholog.domain.project.controller;

import com.ssafy.cholog.domain.project.dto.response.UserProjectListResponse;
import com.ssafy.cholog.domain.project.service.ProjectService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "프로젝트", description = "프로젝트 생성 및 관리 API")
public class ProjectController {

    private final ProjectService projectService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("")
    @Operation(summary = "프로젝트 목록 조회", description = "프로젝트 목록 조회 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<UserProjectListResponse>> getUserProjectList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Integer userId =  authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(projectService.getUserProjectList(userId));
    }
}
