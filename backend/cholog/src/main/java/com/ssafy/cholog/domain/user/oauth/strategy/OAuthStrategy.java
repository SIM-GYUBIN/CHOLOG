package com.ssafy.cholog.domain.user.oauth.strategy;


import com.ssafy.cholog.domain.user.oauth.dto.OAuthUserInfo;
import com.ssafy.cholog.global.exception.CustomException;

public interface OAuthStrategy {
    OAuthUserInfo getUserInfo(String code) throws CustomException;
}
