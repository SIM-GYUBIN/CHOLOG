package com.ssafy.cholog.domain.user.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OAuthUserInfo {
    private String id;
    private String email;
    private String nickname;

    public static OAuthUserInfo of (SsafyUserResponse userResponse) {
        return OAuthUserInfo.builder()
                .id(userResponse.getUserId())
                .email(userResponse.getEmail())
                .nickname(userResponse.getName())
                .build();
    }
}

