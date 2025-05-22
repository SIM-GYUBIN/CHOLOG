package com.ssafy.cholog.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResult {
    private String accessToken;
    private String refreshToken;

    public static LoginResult of(String accessToken, String refreshToken) {
        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
