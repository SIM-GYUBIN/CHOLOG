package com.ssafy.cholog.domain.user.controller;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import com.ssafy.cholog.domain.user.dto.request.SignupRequest;
import com.ssafy.cholog.domain.user.dto.response.LoginResponse;
import com.ssafy.cholog.domain.user.enums.Provider;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "로그인 확인", description = "로그인 확인 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.UNAUTHORIZED_ACCESS})
    public ResponseEntity<CommonResponse<Void>> signUp() {
        return CommonResponse.ok();
    }

    @PostMapping
    @Operation(summary = "회원가입 처리", description = "회원가입 호출 API")
    @ApiErrorCodeExamples({ErrorCode.EMAIL_ALREADY_EXISTS, ErrorCode.NICKNAME_ALREADY_EXISTS})
    public ResponseEntity<CommonResponse<Void>> signUp(@Valid @RequestBody SignupRequest signupRequest) {
        return CommonResponse.ok(userService.signup(signupRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인 처리", description = "로그인 호출 API")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.NOT_GENERAL_USER, ErrorCode.INVALID_INPUT_VALUE})
    public ResponseEntity<CommonResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResult loginResult = userService.login(loginRequest);

        ResponseCookie accessTokenCookie = CookieUtil.makeAccessTokenCookie(loginResult.getAccessToken());
        ResponseCookie refreshTokenCookie = CookieUtil.makeRefreshTokenCookie(loginResult.getRefreshToken());

        return CommonResponse.okWithCookie(accessTokenCookie, refreshTokenCookie);
//        return CommonResponse.redirectWithCookie("https://www.cholog.com", accessTokenCookie, refreshTokenCookie);
    }

    @GetMapping("/login/{provider}")
    @Operation(summary = "소셜 로그인 처리", description = "소셜 로그인 인증 후 자동으로 호출되는 API입니다.\n\n이 API는 직접 호출하지 않으며, 소셜 로그인 과정에서 자동으로 호출됩니다.")
    @ApiErrorCodeExamples({ErrorCode.INVALID_OAUTH_PROVIDER, ErrorCode.NOT_OAUTH_USER, ErrorCode.OAUTH_SERVER_ERROR})
    public ResponseEntity<CommonResponse<LoginResponse>> socialLogin(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            return CommonResponse.redirect("https://www.cholog.com/login");
        }

        Provider socialProvider = Provider.valueOf(provider.toUpperCase());
        LoginResult loginResult = userService.handleOAuthLogin(socialProvider, code);

        ResponseCookie accessTokenCookie = CookieUtil.makeAccessTokenCookie(loginResult.getAccessToken());
        ResponseCookie refreshTokenCookie = CookieUtil.makeRefreshTokenCookie(loginResult.getRefreshToken());

        return CommonResponse.redirectWithCookie("https://www.cholog.com/projectlist", accessTokenCookie, refreshTokenCookie);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 처리", description = "로그아웃 호출 API")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    public ResponseEntity<CommonResponse<Void>> logout() {
        ResponseCookie deletedAccessTokenCookie = CookieUtil.deleteAccessTokenCookie();
        ResponseCookie deletedRefreshTokenCookie = CookieUtil.deleteRefreshTokenCookie();

        return CommonResponse.okWithCookie(deletedAccessTokenCookie, deletedRefreshTokenCookie);
    }
}
