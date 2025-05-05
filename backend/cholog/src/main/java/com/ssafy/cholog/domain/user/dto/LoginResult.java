package com.ssafy.cholog.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResult {
    private String accessToken;
    private String refreshToken;
}
