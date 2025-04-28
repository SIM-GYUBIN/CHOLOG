package com.ssafy.cholog.domain.auth.controller;

import com.ssafy.cholog.domain.auth.dto.LoginResult;
import com.ssafy.cholog.domain.auth.dto.request.LoginRequest;
import com.ssafy.cholog.domain.auth.dto.response.LoginResponse;
import com.ssafy.cholog.domain.auth.service.AuthService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 & 인가 API")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    @Operation(summary = "로그인 처리", description = "로그인 호출 API")
    @ApiErrorCodeExamples({ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<LoginResponse>> login(LoginRequest loginRequest) {
        LoginResult loginResult = authService.login(loginRequest);

        ResponseCookie accessTokenCookie = CookieUtil.makeAccessTokenCookie(loginResult.getAccessToken());
        ResponseCookie refreshTokenCookie = CookieUtil.makeRefreshTokenCookie(loginResult.getRefreshToken());

        return CommonResponse.okWithCookie(accessTokenCookie, refreshTokenCookie);
    }
}
