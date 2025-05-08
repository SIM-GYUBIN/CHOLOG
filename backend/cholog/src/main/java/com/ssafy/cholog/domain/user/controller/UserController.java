package com.ssafy.cholog.domain.user.controller;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import com.ssafy.cholog.domain.user.dto.request.SignupRequest;
import com.ssafy.cholog.domain.user.dto.response.LoginResponse;
import com.ssafy.cholog.domain.user.service.UserService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "회원가입 처리", description = "회원가입 호출 API")
    @ApiErrorCodeExamples({ErrorCode.EMAIL_ALREADY_EXISTS, ErrorCode.NICKNAME_ALREADY_EXISTS})
    public ResponseEntity<CommonResponse<Void>> signUp(@Valid @RequestBody SignupRequest signupRequest) {
        return CommonResponse.ok(userService.signup(signupRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인 처리", description = "로그인 호출 API")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.INVALID_INPUT_VALUE})
    public ResponseEntity<CommonResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResult loginResult = userService.login(loginRequest);

        ResponseCookie accessTokenCookie = CookieUtil.makeAccessTokenCookie(loginResult.getAccessToken());
        ResponseCookie refreshTokenCookie = CookieUtil.makeRefreshTokenCookie(loginResult.getRefreshToken());

        return CommonResponse.okWithCookie(accessTokenCookie, refreshTokenCookie);
    }
}
