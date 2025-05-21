package com.ssafy.cholog.domain.user.oauth.strategy;


import com.ssafy.cholog.domain.user.oauth.client.SsafyOAuthClient;
import com.ssafy.cholog.domain.user.oauth.client.SsafyTokenResponse;
import com.ssafy.cholog.domain.user.oauth.dto.OAuthUserInfo;
import com.ssafy.cholog.domain.user.oauth.dto.SsafyUserResponse;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SsafyOAuthStrategy implements OAuthStrategy {
    private final SsafyOAuthClient ssafyOAuthClient;

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        try {
            SsafyTokenResponse tokenResponse = ssafyOAuthClient.getToken(code);

            SsafyUserResponse userResponse = ssafyOAuthClient.getUserInfo(tokenResponse.getAccessToken());

            return OAuthUserInfo.of(userResponse);

        } catch (IOException e) {
            throw new CustomException(ErrorCode.OAUTH_SERVER_ERROR, "provider", "ssafy")
                    .addParameter("code", code)
                    .addParameter("message", e.getMessage());
        }
    }
}
