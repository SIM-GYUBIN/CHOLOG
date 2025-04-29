package com.ssafy.cholog.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    @Schema(example = "BE_심규빈")
    private final String nickname;
}

