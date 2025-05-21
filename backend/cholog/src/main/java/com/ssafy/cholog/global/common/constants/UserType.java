package com.ssafy.cholog.global.common.constants;

import com.ssafy.cholog.domain.user.enums.Provider;

public enum UserType {
    GENERAL, SSAFY;

    public static UserType getUserTypeByProvider (Provider provider) {
        if (provider == Provider.SSAFY) {
            return UserType.SSAFY;
        } else {
            return UserType.GENERAL;
        }
    }
}
